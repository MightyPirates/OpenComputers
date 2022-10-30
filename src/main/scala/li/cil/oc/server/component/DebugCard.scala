package li.cil.oc.server.component

import java.util.UUID
import java.util.function.Supplier

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
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.server.PacketSender
import li.cil.oc.server.network.DebugNetwork
import li.cil.oc.server.network.DebugNetwork.DebugNode
import li.cil.oc.server.component.DebugCard.AccessContext
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.Block
import net.minecraft.command.CommandSource
import net.minecraft.command.ICommandSource
import net.minecraft.entity.item.minecart.MinecartEntity
import net.minecraft.entity.{Entity, LivingEntity}
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt._
import net.minecraft.scoreboard.{ScoreCriteria, Scoreboard}
import net.minecraft.server.MinecraftServer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.ResourceLocation
import net.minecraft.util.RegistryKey
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.vector.Vector2f
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.registry.Registry
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.{GameType, World, WorldSettings}
import net.minecraft.world.server.ServerWorld
import net.minecraft.world.storage.IServerWorldInfo
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.server.ServerLifecycleHooks
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.ForgeRegistry
import net.minecraftforge.registries.IForgeRegistry

import scala.collection.JavaConverters.{collectionAsScalaIterable, mapAsScalaMap}
import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

class DebugCard(host: EnvironmentHost) extends AbstractManagedEnvironment with DebugNode {
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

  private var CommandMessages: Option[String] = None

  private def createCommandSourceStack(): CommandSource = {
    val sender = new ICommandSource {
      override def sendMessage(message: ITextComponent, sender: UUID) {
        CommandMessages = Option(CommandMessages.fold("")(_ + "\n") + message.getString)
      }

      override def acceptsSuccess = true

      override def acceptsFailure = true

      override def shouldInformAdmins = true
    }
    val world = host.world.asInstanceOf[ServerWorld]
    val server = world.getServer
    def defaultFakePlayer = FakePlayerFactory.get(world, Settings.get.fakePlayerProfile)
    val sourcePlayer = player match {
      case Some(name) => Option(server.getPlayerList.getPlayerByName(name)).getOrElse(defaultFakePlayer)
      case _ => defaultFakePlayer
    }
    val permLevel = server.getProfilePermissions(sourcePlayer.getGameProfile)
    new CommandSource(sender, new Vector3d(host.xPosition, host.yPosition, host.zPosition), Vector2f.ZERO, world,
      permLevel, sourcePlayer.getName.getString, sourcePlayer.getDisplayName, server, sourcePlayer)
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

  @Deprecated
  @Callback(doc = """function([id:number]):userdata -- Get the world object for the specified dimension ID, or the container's.""")
  def getWorld(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    if (args.count() > 0) {
      val server = ServerLifecycleHooks.getCurrentServer
      val world = args.checkInteger(0) match {
        case 0 => server.overworld
        case -1 => server.getLevel(World.NETHER)
        case 1 => server.getLevel(World.END)
        case _ => null
      }
      result(new DebugCard.WorldValue(world))
    }
    else result(new DebugCard.WorldValue(host.world))
  }

  @Deprecated
  @Callback(doc = """function():table -- Get a list of all world IDs, loaded and unloaded.""")
  def getWorlds(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(Array[Int](0, -1, 1))
  }

  @Callback(doc = """function(name:string):userdata -- Get the entity of a player.""")
  def getPlayer(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(new DebugCard.PlayerValue(args.checkString(0)))
  }

  @Callback(doc = """function():table -- Get a list of currently logged-in players.""")
  def getPlayers(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(ServerLifecycleHooks.getCurrentServer.getPlayerNames)
  }

  @Callback(doc = """function():userdata -- Get the scoreboard object for the world""")
  def getScoreboard(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    result(new DebugCard.ScoreboardValue(Option(host.world)))
  }


  @Deprecated
  @Callback(doc = "function(x: number, y: number, z: number[, worldId: number]):boolean, string, table -- returns contents at the location in world by id (default host world)")
  def scanContentsAt(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    val x = args.checkInteger(0)
    val y = args.checkInteger(1)
    val z = args.checkInteger(2)
    val server = ServerLifecycleHooks.getCurrentServer
    val world = if (args.count() > 3) args.checkInteger(3) match {
      case 0 => server.overworld
      case -1 => server.getLevel(World.NETHER)
      case 1 => server.getLevel(World.END)
      case _ => null
    } else host.world

    val position: BlockPosition = new BlockPosition(x, y, z, Option(world))
    val fakePlayer = FakePlayerFactory.get(world.asInstanceOf[ServerWorld], Settings.get.fakePlayerProfile)
    fakePlayer.setPos(position.x + 0.5, position.y + 0.5, position.z + 0.5)

    val candidates = world.getEntitiesOfClass(classOf[Entity], position.bounds, null)
    (if (!candidates.isEmpty) Some(candidates.minBy(fakePlayer.distanceToSqr(_))) else None) match {
      case Some(living: LivingEntity) => result(true, "EntityLiving", living)
      case Some(minecart: MinecartEntity) => result(true, "EntityMinecart", minecart)
      case _ =>
        val state = world.getBlockState(position.toBlockPos)
        val block = state.getBlock
        if (block.isAir(state, world, position.toBlockPos)) {
          result(false, "air", block)
        }
        else if (!block.isInstanceOf[IFluidBlock]) {
          val event = new BlockEvent.BreakEvent(world, position.toBlockPos, state, fakePlayer)
          MinecraftForge.EVENT_BUS.post(event)
          result(event.isCanceled, "liquid", block)
        }
        else if (block.isReplaceable(position)) {
          val event = new BlockEvent.BreakEvent(world, position.toBlockPos, state, fakePlayer)
          MinecraftForge.EVENT_BUS.post(event)
          result(event.isCanceled, "replaceable", block)
        }
        else if (state.getCollisionShape(world, position.toBlockPos, ISelectionContext.empty).isEmpty) {
          result(true, "passable", block)
        }
        else {
          result(true, "solid", block)
        }
    }
  }

  @Callback(doc = """function(name:string):boolean -- Get whether a mod or API is loaded.""")
  def isModLoaded(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    val name = args.checkString(0)
    result(ModList.get.isLoaded(name))
  }

  @Callback(doc = """function(command:string):number -- Runs an arbitrary command using a fake player.""")
  def runCommand(context: Context, args: Arguments): Array[AnyRef] = {
    checkAccess()
    val commands =
      if (args.isTable(0)) collectionAsScalaIterable(args.checkTable(0).values())
      else Iterable(args.checkString(0))

    val source = createCommandSourceStack
    CommandMessages.synchronized {
      CommandMessages = None
      var value = 0
      for (command <- commands) {
        value = ServerLifecycleHooks.getCurrentServer.getCommands.performCommand(source, command.toString)
      }
      result(value, CommandMessages.orNull)
    }
  }

  @Callback(doc = """function(x:number, y:number, z:number):boolean -- Add a component block at the specified coordinates to the computer network.""")
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
        result((), "no node found at this position")
    }
  }

  private def findNode(position: BlockPosition) =
    if (host.world.blockExists(position)) {
      host.world.getBlockEntity(position) match {
        case env: SidedEnvironment => Direction.values.map(env.sidedNode).find(_ != null)
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
    Option(ServerLifecycleHooks.getCurrentServer.getPlayerList.getPlayerByName(args.checkString(0))) match {
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
      val packet = Network.newPacket(node.address, destination, 0, args.drop(1).toArray)
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

  override def loadData(nbt: CompoundNBT): Unit = {
    super.loadData(nbt)
    access = AccessContext.loadData(nbt)
    if (nbt.contains(Settings.namespace + "remoteX")) {
      val x = nbt.getInt(Settings.namespace + "remoteX")
      val y = nbt.getInt(Settings.namespace + "remoteY")
      val z = nbt.getInt(Settings.namespace + "remoteZ")
      remoteNodePosition = Some((x, y, z))
    }
  }

  override def saveData(nbt: CompoundNBT): Unit = {
    super.saveData(nbt)
    access.foreach(_.saveData(nbt))
    remoteNodePosition.foreach {
      case (x, y, z) =>
        nbt.putInt(Settings.namespace + "remoteX", x)
        nbt.putInt(Settings.namespace + "remoteY", y)
        nbt.putInt(Settings.namespace + "remoteZ", z)
    }
  }
}

object DebugCard {
  def checkAccess()(implicit ctx: Option[AccessContext]): Unit =
    for (msg <- Settings.get.debugCardAccess.checkAccess(ctx))
      throw new Exception(msg)

  object AccessContext {
    def remove(nbt: CompoundNBT): Unit = {
      nbt.remove(Settings.namespace + "player")
      nbt.remove(Settings.namespace + "accessNonce")
    }

    def loadData(nbt: CompoundNBT): Option[AccessContext] = {
      if (nbt.contains(Settings.namespace + "player"))
        Some(AccessContext(
          nbt.getString(Settings.namespace + "player"),
          nbt.getString(Settings.namespace + "accessNonce")
        ))
      else
        None
    }
  }

  case class AccessContext(player: String, nonce: String) {
    def saveData(nbt: CompoundNBT): Unit = {
      nbt.putString(Settings.namespace + "player", player)
      nbt.putString(Settings.namespace + "accessNonce", nonce)
    }
  }

  class PlayerValue(var name: String)(implicit var ctx: Option[AccessContext]) extends prefab.AbstractValue {
    def this() = this("")(None) // For loading.

    // ----------------------------------------------------------------------- //

    def withPlayer(f: (ServerPlayerEntity) => Array[AnyRef]): Array[AnyRef] = {
      checkAccess()
      ServerLifecycleHooks.getCurrentServer.getPlayerList.getPlayerByName(name) match {
        case player: ServerPlayerEntity => f(player)
        case _ => result((), "player is offline")
      }
    }

    @Callback(doc = """function():userdata -- Get the player's world object.""")
    def getWorld(context: Context, args: Arguments): Array[AnyRef] = {
      withPlayer(player => result(new DebugCard.WorldValue(player.level)))
    }

    @Callback(doc = """function():string -- Get the player's game type.""")
    def getGameType(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => result(player.gameMode.getGameModeForPlayer.getName))

    @Callback(doc = """function(gametype:string) -- Set the player's game type (survival, creative, adventure).""")
    def setGameType(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        val gametype = args.checkString(0)
        player.gameMode.updateGameMode(GameType.byName(gametype, GameType.SURVIVAL))
        null
      })

    @Callback(doc = """function():number, number, number -- Get the player's position.""")
    def getPosition(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => result(player.getX, player.getY, player.getZ))

    @Callback(doc = """function(x:number, y:number, z:number) -- Set the player's position.""")
    def setPosition(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        player.teleportTo(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2))
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
      withPlayer(player => result(player.totalExperience))

    @Callback(doc = """function(level:number) -- Add a level to the player's experience level""")
    def addExperienceLevel(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        player.giveExperienceLevels(args.checkInteger(0))
        null
      })

    @Callback(doc = """function(level:number) -- Remove a level from the player's experience level""")
    def removeExperienceLevel(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        player.giveExperienceLevels(-args.checkInteger(0))
        null
      })

    @Callback(doc = """function() -- Clear the players inventory""")
    def clearInventory(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        player.inventory.clearContent()
        null
      })

    @Deprecated
    @Callback(doc = """function(id:string, amount:number, meta:number[, nbt:string]):number -- Adds the item stack to the players inventory""")
    def insertItem(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        val item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(args.checkString(0)))
        if (item == null) {
          throw new IllegalArgumentException("invalid item id")
        }
        val amount = args.checkInteger(1)
        args.checkInteger(2) // meta
        val tagJson = args.checkString(3)
        val tag = if (Strings.isNullOrEmpty(tagJson)) null else JsonToNBT.parseTag(tagJson)
        val stack = new ItemStack(item, amount)
        stack.setTag(tag)
        result(InventoryUtils.addToPlayerInventory(stack, player))
      })

    // ----------------------------------------------------------------------- //

    private final val NameTag = "name"

    override def loadData(nbt: CompoundNBT) {
      super.loadData(nbt)
      ctx = AccessContext.loadData(nbt)
      name = nbt.getString(NameTag)
    }

    override def saveData(nbt: CompoundNBT) {
      super.saveData(nbt)
      ctx.foreach(_.saveData(nbt))
      nbt.putString(NameTag, name)
    }
  }

  class ScoreboardValue(world: Option[World])(implicit var ctx: Option[AccessContext]) extends prefab.AbstractValue {
    var scoreboard: Scoreboard = world.fold(null: Scoreboard)(_.getScoreboard)
    var dimension: ResourceLocation = world.fold(World.OVERWORLD)(_.dimension).location

    def this() = this(None)(None) // For loading.

    @Callback(doc = """function(team:string) - Add a team to the scoreboard""")
    def addTeam(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val team = args.checkString(0)
      scoreboard.addPlayerTeam(team)
      null
    }

    @Callback(doc = """function(teamName: string) - Remove a team from the scoreboard""")
    def removeTeam(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val teamName = args.checkString(0)
      val team = scoreboard.getPlayersTeam(teamName)
      scoreboard.removePlayerTeam(team)
      null
    }

    @Callback(doc = """function(player:string, team:string):boolean - Add a player to a team""")
    def addPlayerToTeam(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val player = args.checkString(0)
      val teamName = args.checkString(1)
      val team = scoreboard.getPlayersTeam(teamName)
      result(scoreboard.addPlayerToTeam(player, team))
    }

    @Callback(doc = """function(player:string):boolean - Remove a player from their team""")
    def removePlayerFromTeams(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val player = args.checkString(0)
      result(scoreboard.removePlayerFromTeam(player))
    }

    @Callback(doc = """function(player:string, team:string):boolean - Remove a player from a specific team""")
    def removePlayerFromTeam(context: Context, args: Arguments): Array[AnyRef] =
    {
      checkAccess()
      val player = args.checkString(0)
      val teamName = args.checkString(1)
      val team = scoreboard.getPlayersTeam(teamName)
      scoreboard.removePlayerFromTeam(player, team)
      null
    }

    @Callback(doc = """function(objectiveName:string, objectiveCriteria:string) - Create a new objective for the scoreboard""")
    def addObjective(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val objName = args.checkString(0)
      val objType = args.checkString(1)
      val criteria = ScoreCriteria.byName(objType).orElseThrow(new Supplier[IllegalArgumentException] {
        override def get = new IllegalArgumentException("invalid criterion")
      })
      scoreboard.addObjective(objName, criteria, new StringTextComponent(objName), ScoreCriteria.RenderType.INTEGER)
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
      val score = scoreboard.getOrCreatePlayerScore(name,objective)
      score.setScore(scoreVal)
      null
    }

    @Callback(doc = """function(playerName:string, objectiveName:string):int - Gets the score of a player for a certain objective""")
    def getPlayerScore(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val name = args.checkString(0)
      val objective = scoreboard.getObjective(args.checkString(1))
      val score = scoreboard.getOrCreatePlayerScore(name, objective)
      result(score.getScore)
    }

    @Callback(doc = """function(playerName:string, objectiveName:string, score:int) - Increases the score of a player for a certain objective""")
    def increasePlayerScore(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val name = args.checkString(0)
      val objective = scoreboard.getObjective(args.checkString(1))
      val scoreVal = args.checkInteger(2)
      val score = scoreboard.getOrCreatePlayerScore(name,objective)
      score.add(scoreVal)
      null
    }

    @Callback(doc = """function(playerName:string, objectiveName:string, score:int) - Decrease the score of a player for a certain objective""")
    def decreasePlayerScore(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val name = args.checkString(0)
      val objective = scoreboard.getObjective(args.checkString(1))
      val scoreVal = args.checkInteger(2)
      val score = scoreboard.getOrCreatePlayerScore(name,objective)
      score.add(-scoreVal)
      null
    }


    // ----------------------------------------------------------------------- //

    private final val DimensionTag = "dimension"

    override def loadData(nbt: CompoundNBT) {
      super.loadData(nbt)
      ctx = AccessContext.loadData(nbt)
      dimension = new ResourceLocation(nbt.getString(DimensionTag))
      val dimKey = RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension)
      scoreboard = ServerLifecycleHooks.getCurrentServer.getLevel(dimKey).getScoreboard
    }

    override def saveData(nbt: CompoundNBT): Unit = {
      super.saveData(nbt)
      ctx.foreach(_.saveData(nbt))
      nbt.putString(DimensionTag, dimension.toString)
    }
  }


  class WorldValue(var world: World)(implicit var ctx: Option[AccessContext]) extends prefab.AbstractValue {
    def this() = this(null)(None) // For loading.

    // ----------------------------------------------------------------------- //

    @Deprecated
    @Callback(doc = """function():number -- Gets the numeric id of the current dimension.""")
    def getDimensionId(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      world.dimension match {
        case World.OVERWORLD => result(Int.box(0))
        case World.NETHER => result(Int.box(-1))
        case World.END => result(Int.box(1))
        case _ => throw new Error("deprecated")
      }
    }

    @Deprecated
    @Callback(doc = """function():string -- Gets the name of the current dimension.""")
    def getDimensionName(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.dimension.location.toString)
    }

    @Callback(doc = """function():string -- Gets the resource location of the current dimension.""")
    def getDimension(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.dimension.location.toString)
    }

    @Callback(doc = """function():number -- Gets the seed of the world.""")
    def getSeed(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.asInstanceOf[ServerWorld].getSeed)
    }

    @Callback(doc = """function():boolean -- Returns whether it is currently raining.""")
    def isRaining(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.isRaining)
    }

    @Callback(doc = """function(value:boolean) -- Sets whether it is currently raining.""")
    def setRaining(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      world.getLevelData.setRaining(args.checkBoolean(0))
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
      world.getLevelData.asInstanceOf[IServerWorldInfo].setThundering(args.checkBoolean(0))
      null
    }

    @Callback(doc = """function():number -- Get the current world time.""")
    def getTime(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.getDayTime)
    }

    @Callback(doc = """function(value:number) -- Set the current world time.""")
    def setTime(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      world.asInstanceOf[ServerWorld].setDayTime(args.checkDouble(0).toLong)
      null
    }

    @Callback(doc = """function():number, number, number -- Get the current spawn point coordinates.""")
    def getSpawnPoint(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.getLevelData.getXSpawn, world.getLevelData.getYSpawn, world.getLevelData.getZSpawn)
    }

    @Callback(doc = """function(x:number, y:number, z:number) -- Set the spawn point coordinates.""")
    def setSpawnPoint(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val x = args.checkInteger(0)
      val y = args.checkInteger(1)
      val z = args.checkInteger(2)
      val info = world.getLevelData.asInstanceOf[IServerWorldInfo]
      info.setXSpawn(x)
      info.setYSpawn(y)
      info.setZSpawn(z)
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

    @Deprecated
    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the ID of the block at the specified coordinates.""")
    def getBlockId(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val block = world.getBlockState(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))).getBlock
      result(ForgeRegistries.BLOCKS.asInstanceOf[ForgeRegistry[Block]].getID(block))
    }

    @Deprecated
    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the metadata of the block at the specified coordinates.""")
    def getMetadata(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      args.checkInteger(0)
      args.checkInteger(1)
      args.checkInteger(2)
      result(0)
    }

    @Deprecated
    @Callback(doc = """function(x:number, y:number, z:number[, actualState:boolean=false]) - gets the block state for the block at the specified position, optionally getting additional display related data""")
    def getBlockState(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val pos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      var state = world.getBlockState(pos)
      args.optBoolean(3, false) // actualState
      result(state)
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates is loaded.""")
    def isLoaded(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.isLoaded(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates has a tile entity.""")
    def hasTileEntity(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val blockPos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val state = world.getBlockState(blockPos)
      result(state.hasTileEntity)
    }

    @Callback(doc = """function(x:number, y:number, z:number):table -- Get the NBT of the block at the specified coordinates.""")
    def getTileNBT(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val blockPos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      world.getBlockEntity(blockPos) match {
        case tileEntity: TileEntity => result(toNbt((nbt) => tileEntity.save(nbt)).toTypedMap)
        case _ => null
      }
    }

    @Callback(doc = """function(x:number, y:number, z:number, nbt:table):boolean -- Set the NBT of the block at the specified coordinates.""")
    def setTileNBT(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val blockPos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val state = world.getBlockState(blockPos)
      world.getBlockEntity(blockPos) match {
        case tileEntity: TileEntity =>
          typedMapToNbt(mapAsScalaMap(args.checkTable(3)).toMap) match {
            case nbt: CompoundNBT =>
              tileEntity.load(state, nbt)
              tileEntity.setChanged()
              world.notifyBlockUpdate(blockPos)
              result(true)
            case nbt => result((), s"nbt tag COMPOUND expected, got 'nbt.getType.getName'")
          }
        case _ => result((), "no tile entity")
      }
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the light opacity of the block at the specified coordinates.""")
    def getLightOpacity(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val pos = new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val state = world.getBlockState(pos)
      result(state.getLightBlock(world, pos))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the light value (emission) of the block at the specified coordinates.""")
    def getLightValue(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.getLightEmission(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get whether the block at the specified coordinates is directly under the sky.""")
    def canSeeSky(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      result(world.canSeeSkyFromBelowWater(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))))
    }

    @Deprecated
    private def getStateFromMeta(block: Block, meta: Int) = {
      val states = block.getStateDefinition.getPossibleStates
      if (meta >= 0 && meta < states.size) states.get(meta) else block.defaultBlockState
    }

    @Deprecated
    @Callback(doc = """function(x:number, y:number, z:number, id:number or string, meta:number):number -- Set the block at the specified coordinates.""")
    def setBlock(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val registry = ForgeRegistries.BLOCKS.asInstanceOf[ForgeRegistry[Block]]
      val block = if (args.isInteger(3)) registry.getValue(args.checkInteger(3)) else registry.getValue(new ResourceLocation(args.checkString(3)))
      val metadata = args.checkInteger(4)
      result(world.setBlockAndUpdate(new BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)), getStateFromMeta(block, metadata)))
    }

    @Deprecated
    @Callback(doc = """function(x1:number, y1:number, z1:number, x2:number, y2:number, z2:number, id:number or string, meta:number):number -- Set all blocks in the area defined by the two corner points (x1, y1, z1) and (x2, y2, z2).""")
    def setBlocks(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val (xMin, yMin, zMin) = (args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val (xMax, yMax, zMax) = (args.checkInteger(3), args.checkInteger(4), args.checkInteger(5))
      val registry = ForgeRegistries.BLOCKS.asInstanceOf[ForgeRegistry[Block]]
      val block = if (args.isInteger(3)) registry.getValue(args.checkInteger(3)) else registry.getValue(new ResourceLocation(args.checkString(3)))
      val metadata = args.checkInteger(7)
      for (x <- math.min(xMin, xMax) to math.max(xMin, xMax)) {
        for (y <- math.min(yMin, yMax) to math.max(yMin, yMax)) {
          for (z <- math.min(zMin, zMax) to math.max(zMin, zMax)) {
            world.setBlockAndUpdate(new BlockPos(x, y, z), getStateFromMeta(block, metadata))
          }
        }
      }
      null
    }

    // ----------------------------------------------------------------------- //

    @Deprecated
    @Callback(doc = """function(id:string, count:number, damage:number, nbt:string, x:number, y:number, z:number, side:number):boolean - Insert an item stack into the inventory at the specified location. NBT tag is expected in JSON format.""")
    def insertItem(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(args.checkString(0)))
      if (item == null) {
        throw new IllegalArgumentException("invalid item id")
      }
      val count = args.checkInteger(1)
      val damage = args.checkInteger(2)
      val tagJson = args.optString(3, "")
      val tag = if (Strings.isNullOrEmpty(tagJson)) null else JsonToNBT.parseTag(tagJson)
      val position = BlockPosition(args.checkDouble(4), args.checkDouble(5), args.checkDouble(6), world)
      val side = args.checkSideAny(7)
      InventoryUtils.inventoryAt(position, side) match {
        case Some(inventory) =>
          val stack = new ItemStack(item, count)
          stack.setTag(tag)
          stack.setDamageValue(damage)
          result(InventoryUtils.insertIntoInventory(stack, inventory))
        case _ => result((), "no inventory")
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
        case _ => result((), "no inventory")
      }
    }

    @Callback(doc = """function(id:string, amount:number, x:number, y:number, z:number, side:number):boolean - Insert some fluid into the tank at the specified location.""")
    def insertFluid(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(args.checkString(0)))
      if (fluid == null) {
        throw new IllegalArgumentException("invalid fluid id")
      }
      val amount = args.checkInteger(1)
      val position = BlockPosition(args.checkDouble(2), args.checkDouble(3), args.checkDouble(4), world)
      val side = args.checkSideAny(5)
      world.getBlockEntity(position) match {
        case handler: IFluidHandler => result(handler.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE))
        case _ => result((), "no tank")
      }
    }

    @Callback(doc = """function(amount:number, x:number, y:number, z:number, side:number):boolean - Remove some fluid from a tank at the specified location.""")
    def removeFluid(context: Context, args: Arguments): Array[AnyRef] = {
      checkAccess()
      val amount = args.checkInteger(0)
      val position = BlockPosition(args.checkDouble(1), args.checkDouble(2), args.checkDouble(3), world)
      val side = args.checkSideAny(4)
      world.getBlockEntity(position) match {
        case handler: IFluidHandler => result(handler.drain(amount, IFluidHandler.FluidAction.EXECUTE))
        case _ => result((), "no tank")
      }
    }


    // ----------------------------------------------------------------------- //

    private final val DimensionTag = "dimension"

    override def loadData(nbt: CompoundNBT) {
      super.loadData(nbt)
      ctx = AccessContext.loadData(nbt)
      val dimension = new ResourceLocation(nbt.getString(DimensionTag))
      val dimKey = RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension)
      world = ServerLifecycleHooks.getCurrentServer.getLevel(dimKey)
    }

    override def saveData(nbt: CompoundNBT) {
      super.saveData(nbt)
      ctx.foreach(_.saveData(nbt))
      nbt.putString(DimensionTag, world.dimension.location.toString)
    }
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

    override def loadData(nbt: CompoundNBT): Unit = {
      super.loadData(nbt)
      value = nbt.getString(ValueTag)
    }

    override def saveData(nbt: CompoundNBT): Unit = {
      super.saveData(nbt)
      nbt.putString(ValueTag, value)
    }
  }

}
