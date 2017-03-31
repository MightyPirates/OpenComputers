package li.cil.oc.server.component

import com.google.common.base.Strings
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ComponentConnector
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Packet
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.server.PacketSender
import li.cil.oc.server.network.DebugNetwork
import li.cil.oc.server.network.DebugNetwork.DebugNode
import li.cil.oc.server.component.DebugCard.{AccessContext, CommandSender}
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.Block
import net.minecraft.command.CommandResultStats.Type
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt._
import net.minecraft.scoreboard.{IScoreCriteria, Scoreboard}
import net.minecraft.server.management.UserListOpsEntry
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.text.ITextComponent
import net.minecraft.world.GameType
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.ModAPIManager

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class DebugCard(host: EnvironmentHost) extends prefab.ManagedEnvironment with DebugNode {
  override val node: ComponentConnector = Network.newNode(this, Visibility.Neighbors).
    withComponent("debug").
    withConnector().
    create()

  // Used to detect disconnects.
  private var remoteNode: Option[Node] = None

  // Used for delayed connecting to remote node again after loading.
  private var remoteNodePosition: Option[(Int, Int, Int)] = None

  // Player this card is bound to (if any) to use for permissions.
  implicit var access: Option[AccessContext] = None

  def player: Option[String] = access.map(_.player)

  private lazy val CommandSender = {
    def defaultFakePlayer = FakePlayerFactory.get(host.world.asInstanceOf[WorldServer], Settings.get.fakePlayerProfile)
    new CommandSender(host, player match {
      case Some(name) => Option(FMLCommonHandler.instance.getMinecraftServerInstance.getPlayerList.getPlayerByUsername(name)).getOrElse(defaultFakePlayer)
      case _ => defaultFakePlayer
    })
  }

  // ----------------------------------------------------------------------- //

  import li.cil.oc.server.component.DebugCard.checkAccess

  @Callback(doc = """function(value:number):number -- Changes the component network's energy buffer by the specified delta.""")
  def changeBuffer(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(node.changeBuffer(args.checkDouble(0)))
  }

  @Callback(doc = """function():number -- Get the container's X position in the world.""")
  def getX(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(host.xPosition)
  }

  @Callback(doc = """function():number -- Get the container's Y position in the world.""")
  def getY(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(host.yPosition)
  }

  @Callback(doc = """function():number -- Get the container's Z position in the world.""")
  def getZ(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(host.zPosition)
  }

  @Callback(doc = """function([id:number]):userdata -- Get the world object for the specified dimension ID, or the container's.""")
  def getWorld(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    if (args.count() > 0) result(new DebugCard.WorldValue(DimensionManager.getWorld(args.checkInteger(0))))
    else result(new DebugCard.WorldValue(host.world))
  }

  @Callback(doc = """function():table -- Get a list of all world IDs, loaded and unloaded.""")
  def getWorlds(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(DimensionManager.getStaticDimensionIDs)
  }

  @Callback(doc = """function(name:string):userdata -- Get the entity of a player.""")
  def getPlayer(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(new DebugCard.PlayerValue(args.checkString(0)))
  }

  @Callback(doc = """function():table -- Get a list of currently logged-in players.""")
  def getPlayers(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(FMLCommonHandler.instance.getMinecraftServerInstance.getOnlinePlayerNames)
  }

  @Callback(doc = """function():userdata -- Get the scoreboard object for the world""")
  def getScoreboard(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(new DebugCard.ScoreboardValue(Option(host.world)))
  }

  @Callback(doc = """function(name:string):boolean -- Get whether a mod or API is loaded.""")
  def isModLoaded(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    val name = args.checkString(0)
    result(Loader.isModLoaded(name) || ModAPIManager.INSTANCE.hasAPI(name))
  }

  @Callback(doc = """function(command:string):number -- Runs an arbitrary command using a fake player.""")
  def runCommand(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    val commands =
      if (args.isTable(0)) collectionAsScalaIterable(args.checkTable(0).values())
      else Iterable(args.checkString(0))

    CommandSender.synchronized {
      CommandSender.prepare()
      var value = 0
      for (command <- commands) {
        value = FMLCommonHandler.instance.getMinecraftServerInstance.getCommandManager.executeCommand(CommandSender, command.toString)
      }
      result(value, CommandSender.messages.orNull)
    }
  }

  @Callback(doc = """function(x:number, y:number, z:number):boolean -- Connect the debug card to the block at the specified coordinates.""")
  def connectToBlock(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    val x = args.checkInteger(0)
    val y = args.checkInteger(1)
    val z = args.checkInteger(2)
    findNode(BlockPosition(x, y, z)) match {
      case Some(other) =>
        remoteNode.foreach(other => node.disconnect(other))
        remoteNode = Some(other)
        remoteNodePosition = Some((x, y, z))
        node.connect(other)
        result(true)
      case _ =>
        result(Unit, "no node found at this position")
    }
  }

  private def findNode(position: BlockPosition) =
    if (host.world.blockExists(position)) {
      host.world.getTileEntity(position) match {
        case env: SidedEnvironment => EnumFacing.values.map(env.sidedNode).find(_ != null)
        case env: Environment => Option(env.node)
        case _ => None
      }
    }
    else None

  @Callback(doc = """function():userdata -- Test method for user-data and general value conversion.""")
  def test(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()

    val v1 = mutable.Map("a" -> true, "b" -> "test")
    val v2 = Map(10 -> "zxc", false -> v1)
    v1 += "c" -> v2

    result(v2, new DebugCard.TestValue(), host.world)
  }

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(player:string, text:string) -- Sends text to the specified player's clipboard if possible.""")
  def sendToClipboard(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    Option(FMLCommonHandler.instance.getMinecraftServerInstance.getPlayerList.getPlayerByUsername(args.checkString(0))) match {
      case Some(player) =>
        PacketSender.sendClipboard(player, args.checkString(1))
        result(true)
      case _ =>
        result(false, "no such player")
    }
  }

  @Callback(doc = """function(address:string, data...) -- Sends data to the debug card with the specified address.""")
  def sendToDebugCard(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    val destination = args.checkString(0)
    DebugNetwork.getEndpoint(destination).filter(_ != this).foreach{endpoint =>
      // Cast to iterable to use Scala's toArray instead of the Arguments' one (which converts byte arrays to Strings).
      val packet = Network.newPacket(node.address, destination, 0, args.drop(1).asInstanceOf[java.lang.Iterable[AnyRef]].toArray)
      endpoint.receivePacket(packet)
    }
    result()
  }

  override def receivePacket(packet: Packet) {
    val distance = 0
    node.sendToReachable("computer.signal", Seq("debug_message", packet.source, Int.box(packet.port), Double.box(distance)) ++ packet.data: _*)
  }

  override def address: String = if(node != null) node.address() else "debug"

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node): Unit = {
    super.onConnect(node)
    if (node == this.node) {
      DebugNetwork.add(this)
      remoteNodePosition.foreach {
        case (x, y, z) =>
          remoteNode = findNode(BlockPosition(x, y, z))
          remoteNode match {
            case Some(other) => node.connect(other)
            case _ => remoteNodePosition = None
          }
      }
    }
  }

  override def onDisconnect(node: Node): Unit = {
    super.onDisconnect(node)
    if (node == this.node) {
      DebugNetwork.remove(this)
      remoteNode.foreach(other => other.disconnect(node))
    }
    else if (remoteNode.contains(node)) {
      remoteNode = None
      remoteNodePosition = None
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound): Unit = {
    super.load(nbt)
    access = AccessContext.load(nbt)
    if (nbt.hasKey(Settings.namespace + "remoteX")) {
      val x = nbt.getInteger(Settings.namespace + "remoteX")
      val y = nbt.getInteger(Settings.namespace + "remoteY")
      val z = nbt.getInteger(Settings.namespace + "remoteZ")
      remoteNodePosition = Some((x, y, z))
    }
  }

  override def save(nbt: NBTTagCompound): Unit = {
    super.save(nbt)
    access.foreach(_.save(nbt))
    remoteNodePosition.foreach {
      case (x, y, z) =>
        nbt.setInteger(Settings.namespace + "remoteX", x)
        nbt.setInteger(Settings.namespace + "remoteY", y)
        nbt.setInteger(Settings.namespace + "remoteZ", z)
    }
  }
}

object DebugCard {
  def checkAccess()(implicit ctx: Option[AccessContext]): Unit =
    for (msg <- Settings.get.debugCardAccess.checkAccess(ctx))
      throw new Exception(msg)

  object AccessContext {
    def remove(nbt: NBTTagCompound): Unit = {
      nbt.removeTag(Settings.namespace + "player")
      nbt.removeTag(Settings.namespace + "accessNonce")
    }

    def load(nbt: NBTTagCompound): Option[AccessContext] = {
      if (nbt.hasKey(Settings.namespace + "player"))
        Some(AccessContext(
          nbt.getString(Settings.namespace + "player"),
          nbt.getString(Settings.namespace + "accessNonce")
        ))
      else
        None
    }
  }

  case class AccessContext(player: String, nonce: String) {
    def save(nbt: NBTTagCompound): Unit = {
      nbt.setString(Settings.namespace + "player", player)
      nbt.setString(Settings.namespace + "accessNonce", nonce)
    }
  }

  class PlayerValue(var name: String)(implicit var ctx: Option[AccessContext]) extends prefab.AbstractValue {
    def this() = this("")(None) // For loading.

    // ----------------------------------------------------------------------- //

    def withPlayer(f: (EntityPlayerMP) => Array[AnyRef]): Array[AnyRef] = {
      checkAccess()
      FMLCommonHandler.instance.getMinecraftServerInstance.getPlayerList.getPlayerByUsername(name) match {
        case player: EntityPlayerMP => f(player)
        case _ => result(Unit, "player is offline")
      }
    }

    @Callback(doc = """function():userdata -- Get the player's world object.""")
    def getWorld(context: Context, args: Arguments): Array[AnyRef] = {
      withPlayer(player => result(new DebugCard.WorldValue(player.getEntityWorld)))
    }

    @Callback(doc = """function():string -- Get the player's game type.""")
    def getGameType(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => result(player.interactionManager.getGameType.getName))

    @Callback(doc = """function(gametype:string) -- Set the player's game type (survival, creative, adventure).""")
    def setGameType(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        val gametype = args.checkString(0)
        player.setGameType(GameType.values.find(_.name == gametype).getOrElse(GameType.SURVIVAL))
        null
      })

    @Callback(doc = """function():number, number, number -- Get the player's position.""")
    def getPosition(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => result(player.posX, player.posY, player.posZ))

    @Callback(doc = """function(x:number, y:number, z:number) -- Set the player's position.""")
    def setPosition(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        player.setPositionAndUpdate(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2))
        null
      })

    @Callback(doc = """function():number -- Get the player's health.""")
    def getHealth(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => result(player.getHealth))

    @Callback(doc = """function():number -- Get the player's max health.""")
    def getMaxHealth(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => result(player.getMaxHealth))

    @Callback(doc = """function(health:number) -- Set the player's health.""")
    def setHealth(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        player.setHealth(args.checkDouble(0).toFloat)
        null
      })

    @Callback(doc = """function():number -- Get the player's level""")
    def getLevel(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => result(player.experienceLevel))

    @Callback(doc = """function():number -- Get the player's total experience""")
    def getExperienceTotal(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => result(player.experienceTotal))

    @Callback(doc = """function(level:number) -- Add a level to the player's experience level""")
    def addExperienceLevel(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        player.addExperienceLevel(args.checkInteger(0))
        null
      })

    @Callback(doc = """function(level:number) -- Remove a level from the player's experience level""")
    def removeExperienceLevel(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        player.removeExperienceLevel(args.checkInteger(0))
        null
      })

    @Callback(doc = """function() -- Clear the players inventory""")
    def clearInventory(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        player.inventory.clear()
        null
      })

    @Callback(doc = """function(id:string, amount:number, meta:number[, nbt:string]):number -- Adds the item stack to the players inventory""")
    def insertItem(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        val item = Item.REGISTRY.getObject(new ResourceLocation(args.checkString(0)))
        if (item == null) {
          throw new IllegalArgumentException("invalid item id")
        }
        val amount = args.checkInteger(1)
        val meta = args.checkInteger(2)
        val tagJson = args.checkString(3)
        val tag = if (Strings.isNullOrEmpty(tagJson)) null else JsonToNBT.getTagFromJson(tagJson)
        val stack = new ItemStack(item, amount, meta)
        stack.setTagCompound(tag)
        result(InventoryUtils.addToPlayerInventory(stack, player))
      })

    // ----------------------------------------------------------------------- //

    private final val NameTag = "name"

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      ctx = AccessContext.load(nbt)
      name = nbt.getString(NameTag)
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      ctx.foreach(_.save(nbt))
      nbt.setString(NameTag, name)
    }
  }

  class ScoreboardValue(world: Option[World])(implicit var ctx: Option[AccessContext]) extends prefab.AbstractValue {
    var scoreboard: Scoreboard = world.fold(null: Scoreboard)(_.getScoreboard)
    var dimension: Int = world.fold(0)(_.provider.getDimension)

    def this() = this(None)(None) // For loading.

    @Callback(doc = """function(team:string) - Add a team to the scoreboard""")
    def addTeam(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val team = args.checkString(0)
      scoreboard.createTeam(team)
      null
    }

    @Callback(doc = """function(teamName: string) - Remove a team from the scoreboard""")
    def removeTeam(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val teamName = args.checkString(0)
      val team = scoreboard.getTeam(teamName)
      scoreboard.removeTeam(team)
      null
    }

    @Callback(doc = """function(player:string, team:string):boolean - Add a player to a team""")
    def addPlayerToTeam(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val player = args.checkString(0)
      val team = args.checkString(1)
      result(scoreboard.addPlayerToTeam(player, team))
    }

    @Callback(doc = """function(player:string):boolean - Remove a player from their team""")
    def removePlayerFromTeams(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val player = args.checkString(0)
      result(scoreboard.removePlayerFromTeams(player))
    }

    @Callback(doc = """function(player:string, team:string):boolean - Remove a player from a specific team""")
    def removePlayerFromTeam(context: Context, args: Arguments): Array[AnyRef] =
    {
      checkAccess()
      val player = args.checkString(0)
      val teamName = args.checkString(1)
      val team = scoreboard.getTeam(teamName)
      scoreboard.removePlayerFromTeam(player, team)
      null
    }

    @Callback(doc = """function(objectiveName:string, objectiveCriteria:string) - Create a new objective for the scoreboard""")
    def addObjective(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val objName = args.checkString(0)
      val objType = args.checkString(1)
      val criteria = IScoreCriteria.INSTANCES.get(objType)
      scoreboard.addScoreObjective(objName, criteria)
      null
    }

    @Callback(doc = """function(objectiveName:string) - Remove an objective from the scoreboard""")
    def removeObjective(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val objName = args.checkString(0)
      val objective = scoreboard.getObjective(objName)
      scoreboard.removeObjective(objective)
      null
    }

    @Callback(doc = """function(playerName:string, objectiveName:string, score:int) - Sets the score of a player for a certain objective""")
    def setPlayerScore(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val name = args.checkString(0)
      val objective = scoreboard.getObjective(args.checkString(1))
      val scoreVal = args.checkInteger(2)
      val score = scoreboard.getOrCreateScore(name,objective)
      score.setScorePoints(scoreVal)
      null
    }

    @Callback(doc = """function(playerName:string, objectiveName:string):int - Gets the score of a player for a certain objective""")
    def getPlayerScore(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val name = args.checkString(0)
      val objective = scoreboard.getObjective(args.checkString(1))
      val score = scoreboard.getOrCreateScore(name, objective)
      result(score.getScorePoints)
    }

    @Callback(doc = """function(playerName:string, objectiveName:string, score:int) - Increases the score of a player for a certain objective""")
    def increasePlayerScore(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val name = args.checkString(0)
      val objective = scoreboard.getObjective(args.checkString(1))
      val scoreVal = args.checkInteger(2)
      val score = scoreboard.getOrCreateScore(name,objective)
      score.increaseScore(scoreVal)
      null
    }

    @Callback(doc = """function(playerName:string, objectiveName:string, score:int) - Decrease the score of a player for a certain objective""")
    def decreasePlayerScore(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val name = args.checkString(0)
      val objective = scoreboard.getObjective(args.checkString(1))
      val scoreVal = args.checkInteger(2)
      val score = scoreboard.getOrCreateScore(name,objective)
      score.decreaseScore(scoreVal)
      null
    }


    // ----------------------------------------------------------------------- //

    private final val DimensionTag = "dimension"

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      ctx = AccessContext.load(nbt)
      dimension = nbt.getInteger(DimensionTag)
      scoreboard = DimensionManager.getWorld(dimension).getScoreboard
    }

    override def save(nbt: NBTTagCompound): Unit = {
      super.save(nbt)
      ctx.foreach(_.save(nbt))
      nbt.setInteger(DimensionTag, dimension)
    }
  }


  class WorldValue(var world: World)(implicit var ctx: Option[AccessContext]) extends prefab.AbstractValue {
    def this() = this(null)(None) // For loading.

    // ----------------------------------------------------------------------- //

    @Callback(doc = """function():number -- Gets the numeric id of the current dimension.""")
    def getDimensionId(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.provider.getDimension)
    }

    @Callback(doc = """function():string -- Gets the name of the current dimension.""")
    def getDimensionName(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.provider.getDimensionType.getName)
    }

    @Callback(doc = """function():number -- Gets the seed of the world.""")
    def getSeed(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.getSeed)
    }

    @Callback(doc = """function():boolean -- Returns whether it is currently raining.""")
    def isRaining(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.isRaining)
    }

    @Callback(doc = """function(value:boolean) -- Sets whether it is currently raining.""")
    def setRaining(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      world.getWorldInfo.setRaining(args.checkBoolean(0))
      null
    }

    @Callback(doc = """function():boolean -- Returns whether it is currently thundering.""")
    def isThundering(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.isThundering)
    }

    @Callback(doc = """function(value:boolean) -- Sets whether it is currently thundering.""")
    def setThundering(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      world.getWorldInfo.setThundering(args.checkBoolean(0))
      null
    }

    @Callback(doc = """function():number -- Get the current world time.""")
    def getTime(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.getWorldTime)
    }

    @Callback(doc = """function(value:number) -- Set the current world time.""")
    def setTime(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      world.setWorldTime(args.checkDouble(0).toLong)
      null
    }

    @Callback(doc = """function():number, number, number -- Get the current spawn point coordinates.""")
    def getSpawnPoint(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.getWorldInfo.getSpawnX, world.getWorldInfo.getSpawnY, world.getWorldInfo.getSpawnZ)
    }

    @Callback(doc = """function(x:number, y:number, z:number) -- Set the spawn point coordinates.""")
    def setSpawnPoint(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      world.getWorldInfo.setSpawn(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))
      null
    }

    @Callback(doc = """function(x:number, y:number, z:number, sound:string, range:number) -- Play a sound at the specified coordinates.""")
    def playSoundAt(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val (x, y, z) = (args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val sound = args.checkString(3)
      val range = args.checkInteger(4)
      PacketSender.sendSound(world, x, y, z, new ResourceLocation(sound), SoundCategory.MASTER, range)
      null
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the ID of the block at the specified coordinates.""")
    def getBlockId(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(Block.getIdFromBlock(world.getBlockState(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))).getBlock))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the metadata of the block at the specified coordinates.""")
    def getMetadata(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.getBlockState(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates is loaded.""")
    def isLoaded(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.isBlockLoaded(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates has a tile entity.""")
    def hasTileEntity(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val blockPos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val state = world.getBlockState(blockPos)
      result(state.getBlock.hasTileEntity(state))
    }

    @Callback(doc = """function(x:number, y:number, z:number):table -- Get the NBT of the block at the specified coordinates.""")
    def getTileNBT(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val blockPos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      world.getTileEntity(blockPos) match {
        case tileEntity: TileEntity => result(toNbt((nbt) => tileEntity.writeToNBT(nbt): Unit).toTypedMap)
        case _ => null
      }
    }

    @Callback(doc = """function(x:number, y:number, z:number, nbt:table):boolean -- Set the NBT of the block at the specified coordinates.""")
    def setTileNBT(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val blockPos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      world.getTileEntity(blockPos) match {
        case tileEntity: TileEntity =>
          typedMapToNbt(mapAsScalaMap(args.checkTable(3)).toMap) match {
            case nbt: NBTTagCompound =>
              tileEntity.readFromNBT(nbt)
              tileEntity.markDirty()
              world.notifyBlockUpdate(blockPos)
              result(true)
            case nbt => result(Unit, s"nbt tag compound expected, got '${NBTBase.NBT_TYPES(nbt.getId)}'")
          }
        case _ => result(Unit, "no tile entity")
      }
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the light opacity of the block at the specified coordinates.""")
    def getLightOpacity(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.getBlockLightOpacity(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the light value (emission) of the block at the specified coordinates.""")
    def getLightValue(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.getLight(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)), false))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get whether the block at the specified coordinates is directly under the sky.""")
    def canSeeSky(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.canBlockSeeSky(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))))
    }

    @Callback(doc = """function(x:number, y:number, z:number, id:number or string, meta:number):number -- Set the block at the specified coordinates.""")
    def setBlock(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val block = if (args.isInteger(3)) Block.getBlockById(args.checkInteger(3)) else Block.getBlockFromName(args.checkString(3))
      val metadata = args.checkInteger(4)
      result(world.setBlockState(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)), block.getStateFromMeta(metadata)))
    }

    @Callback(doc = """function(x1:number, y1:number, z1:number, x2:number, y2:number, z2:number, id:number or string, meta:number):number -- Set all blocks in the area defined by the two corner points (x1, y1, z1) and (x2, y2, z2).""")
    def setBlocks(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val (xMin, yMin, zMin) = (args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val (xMax, yMax, zMax) = (args.checkInteger(3), args.checkInteger(4), args.checkInteger(5))
      val block = if (args.isInteger(6)) Block.getBlockById(args.checkInteger(6)) else Block.getBlockFromName(args.checkString(6))
      val metadata = args.checkInteger(7)
      for (x <- math.min(xMin, xMax) to math.max(xMin, xMax)) {
        for (y <- math.min(yMin, yMax) to math.max(yMin, yMax)) {
          for (z <- math.min(zMin, zMax) to math.max(zMin, zMax)) {
            world.setBlockState(new BlockPos(x, y, z), block.getStateFromMeta(metadata))
          }
        }
      }
      null
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = """function(id:string, count:number, damage:number, nbt:string, x:number, y:number, z:number, side:number):boolean - Insert an item stack into the inventory at the specified location. NBT tag is expected in JSON format.""")
    def insertItem(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val item = Item.REGISTRY.getObject(new ResourceLocation(args.checkString(0)))
      if (item == null) {
        throw new IllegalArgumentException("invalid item id")
      }
      val count = args.checkInteger(1)
      val damage = args.checkInteger(2)
      val tagJson = args.checkString(3)
      val tag = if (Strings.isNullOrEmpty(tagJson)) null else JsonToNBT.getTagFromJson(tagJson)
      val position = BlockPosition(args.checkDouble(4), args.checkDouble(5), args.checkDouble(6), world)
      val side = args.checkSideAny(7)
      InventoryUtils.inventoryAt(position, side) match {
        case Some(inventory) =>
          val stack = new ItemStack(item, count, damage)
          stack.setTagCompound(tag)
          result(InventoryUtils.insertIntoInventory(stack, inventory))
        case _ => result(Unit, "no inventory")
      }
    }

    @Callback(doc = """function(x:number, y:number, z:number, slot:number[, count:number]):number - Reduce the size of an item stack in the inventory at the specified location.""")
    def removeItem(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val position = BlockPosition(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2), world)
      InventoryUtils.anyInventoryAt(position) match {
        case Some(inventory) =>
          val slot = args.checkSlot(inventory, 3)
          val count = args.optInteger(4, 64)
          val removed = inventory.extractItem(slot, count, false)
          if (removed.isEmpty) result(0)
          else result(removed.getCount)
        case _ => result(Unit, "no inventory")
      }
    }

    @Callback(doc = """function(id:string, amount:number, x:number, y:number, z:number, side:number):boolean - Insert some fluid into the tank at the specified location.""")
    def insertFluid(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val fluid = FluidRegistry.getFluid(args.checkString(0))
      if (fluid == null) {
        throw new IllegalArgumentException("invalid fluid id")
      }
      val amount = args.checkInteger(1)
      val position = BlockPosition(args.checkDouble(2), args.checkDouble(3), args.checkDouble(4), world)
      val side = args.checkSideAny(5)
      world.getTileEntity(position) match {
        case handler: IFluidHandler => result(handler.fill(new FluidStack(fluid, amount), true))
        case _ => result(Unit, "no tank")
      }
    }

    @Callback(doc = """function(amount:number, x:number, y:number, z:number, side:number):boolean - Remove some fluid from a tank at the specified location.""")
    def removeFluid(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val amount = args.checkInteger(0)
      val position = BlockPosition(args.checkDouble(1), args.checkDouble(2), args.checkDouble(3), world)
      val side = args.checkSideAny(4)
      world.getTileEntity(position) match {
        case handler: IFluidHandler => result(handler.drain(amount, true))
        case _ => result(Unit, "no tank")
      }
    }


    // ----------------------------------------------------------------------- //

    private final val DimensionTag = "dimension"

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      ctx = AccessContext.load(nbt)
      world = DimensionManager.getWorld(nbt.getInteger(DimensionTag))
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      ctx.foreach(_.save(nbt))
      nbt.setInteger(DimensionTag, world.provider.getDimension)
    }
  }

  class CommandSender(val host: EnvironmentHost, val underlying: EntityPlayerMP) extends FakePlayer(underlying.getEntityWorld.asInstanceOf[WorldServer], underlying.getGameProfile) {
    var messages: Option[String] = None

    def prepare(): Unit = {
      val blockPos = BlockPosition(host)
      posX = blockPos.x
      posY = blockPos.y
      posZ = blockPos.z
      messages = None
    }

    override def getName: String = underlying.getName

    override def getEntityWorld: World = host.world

    override def sendMessage(message: ITextComponent) {
      messages = Option(messages.fold("")(_ + "\n") + message.getUnformattedText)
    }

    override def getDisplayName: ITextComponent = underlying.getDisplayName

    override def setCommandStat(`type`: Type, amount: Int): Unit = underlying.setCommandStat(`type`, amount)

    override def getPosition: BlockPos = underlying.getPosition

    override def canUseCommand(level: Int, commandName: String): Boolean = {
      val profile = underlying.getGameProfile
      val server = underlying.mcServer
      val config = server.getPlayerList
      server.isSinglePlayer || (config.canSendCommands(profile) && (config.getOppedPlayers.getEntry(profile) match {
        case entry: UserListOpsEntry => entry.getPermissionLevel >= level
        case _ => server.getOpPermissionLevel >= level
      }))
    }

    override def getCommandSenderEntity: EntityPlayerMP = underlying

    override def getPositionVector: Vec3d = underlying.getPositionVector

    override def sendCommandFeedback(): Boolean = underlying.sendCommandFeedback()
  }

  class TestValue extends AbstractValue {
    var value = "hello"

    override def apply(context: Context, arguments: Arguments): AnyRef = {
      OpenComputers.log.info("TestValue.apply(" + arguments.toArray.mkString(", ") + ")")
      value
    }

    override def unapply(context: Context, arguments: Arguments): Unit = {
      OpenComputers.log.info("TestValue.unapply(" + arguments.toArray.mkString(", ") + ")")
      value = arguments.checkString(1)
    }

    override def call(context: Context, arguments: Arguments): Array[AnyRef] = {
      OpenComputers.log.info("TestValue.call(" + arguments.toArray.mkString(", ") + ")")
      result(arguments.toArray: _*)
    }

    override def dispose(context: Context): Unit = {
      super.dispose(context)
      OpenComputers.log.info("TestValue.dispose()")
    }

    private final val ValueTag = "value"

    override def load(nbt: NBTTagCompound): Unit = {
      super.load(nbt)
      value = nbt.getString(ValueTag)
    }

    override def save(nbt: NBTTagCompound): Unit = {
      super.save(nbt)
      nbt.setString(ValueTag, value)
    }
  }

}
