package li.cil.oc.server.component

import com.google.common.base.Strings
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.ModAPIManager
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.server.component.DebugCard.CommandSender
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.JsonToNBT
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.server.management.UserListOpsEntry
import net.minecraft.util.IChatComponent
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraft.world.WorldSettings.GameType
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidHandler

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class DebugCard(host: EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("debug").
    withConnector().
    create()

  // Used to detect disconnects.
  private var remoteNode: Option[Node] = None

  // Used for delayed connecting to remote node again after loading.
  private var remoteNodePosition: Option[(Int, Int, Int)] = None

  // Player this card is bound to (if any) to use for permissions.
  var player: Option[String] = None

  private lazy val CommandSender = {
    def defaultFakePlayer = FakePlayerFactory.get(host.world.asInstanceOf[WorldServer], Settings.get.fakePlayerProfile)
    new CommandSender(host, player match {
      case Some(name) => Option(MinecraftServer.getServer.getConfigurationManager.func_152612_a(name)) match {
        case Some(playerEntity) => playerEntity
        case _ => defaultFakePlayer
      }
      case _ => defaultFakePlayer
    })
  }

  // ----------------------------------------------------------------------- //

  import li.cil.oc.server.component.DebugCard.checkEnabled

  @Callback(doc = """function(value:number):number -- Changes the component network's energy buffer by the specified delta.""")
  def changeBuffer(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    result(node.changeBuffer(args.checkDouble(0)))
  }

  @Callback(doc = """function():number -- Get the container's X position in the world.""")
  def getX(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    result(host.xPosition)
  }

  @Callback(doc = """function():number -- Get the container's Y position in the world.""")
  def getY(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    result(host.yPosition)
  }

  @Callback(doc = """function():number -- Get the container's Z position in the world.""")
  def getZ(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    result(host.zPosition)
  }

  @Callback(doc = """function([id:number]):userdata -- Get the world object for the specified dimension ID, or the container's.""")
  def getWorld(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    if (args.count() > 0) result(new DebugCard.WorldValue(DimensionManager.getWorld(args.checkInteger(0))))
    else result(new DebugCard.WorldValue(host.world))
  }

  @Callback(doc = """function():table -- Get a list of all world IDs, loaded and unloaded.""")
  def getWorlds(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    result(DimensionManager.getStaticDimensionIDs)
  }

  @Callback(doc = """function(name:string):userdata -- Get the entity of a player.""")
  def getPlayer(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    result(new DebugCard.PlayerValue(args.checkString(0)))
  }

  @Callback(doc = """function():table -- Get a list of currently logged-in players.""")
  def getPlayers(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    result(MinecraftServer.getServer.getAllUsernames)
  }

  @Callback(doc = """function(name:string):boolean -- Get whether a mod or API is loaded.""")
  def isModLoaded(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    val name = args.checkString(0)
    result(Loader.isModLoaded(name) || ModAPIManager.INSTANCE.hasAPI(name))
  }

  @Callback(doc = """function(command:string):number -- Runs an arbitrary command using a fake player.""")
  def runCommand(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    val commands =
      if (args.isTable(0)) collectionAsScalaIterable(args.checkTable(0).values())
      else Iterable(args.checkString(0))

    CommandSender.synchronized {
      CommandSender.prepare()
      var value = 0
      for (command <- commands) {
        value = MinecraftServer.getServer.getCommandManager.executeCommand(CommandSender, command.toString)
      }
      result(value, CommandSender.messages.orNull)
    }
  }

  @Callback(doc = """function(x:number, y:number, z:number):boolean -- Connect the debug card to the block at the specified coordinates.""")
  def connectToBlock(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    val x = args.checkInteger(0)
    val y = args.checkInteger(1)
    val z = args.checkInteger(2)
    findNode(x, y, z) match {
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

  private def findNode(x: Int, y: Int, z: Int) =
    if (host.world.blockExists(x, y, z)) {
      host.world.getTileEntity(x, y, z) match {
        case env: SidedEnvironment => ForgeDirection.VALID_DIRECTIONS.map(env.sidedNode).find(_ != null)
        case env: Environment => Option(env.node)
        case _ => None
      }
    }
    else None

  @Callback(doc = """function():userdata -- Test method for user-data and general value conversion.""")
  def test(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()

    val v1 = mutable.Map("a" -> true, "b" -> "test")
    val v2 = Map(10 -> "zxc", false -> v1)
    v1 += "c" -> v2

    result(v2, new DebugCard.TestValue(), host.world)
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node): Unit = {
    super.onConnect(node)
    if (node == this.node) remoteNodePosition.foreach {
      case (x, y, z) =>
        remoteNode = findNode(x, y, z)
        remoteNode match {
          case Some(other) => node.connect(other)
          case _ => remoteNodePosition = None
        }
    }
  }

  override def onDisconnect(node: Node): Unit = {
    super.onDisconnect(node)
    if (node == this.node) {
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
    if (nbt.hasKey(Settings.namespace + "remoteX")) {
      val x = nbt.getInteger(Settings.namespace + "remoteX")
      val y = nbt.getInteger(Settings.namespace + "remoteY")
      val z = nbt.getInteger(Settings.namespace + "remoteZ")
      remoteNodePosition = Some((x, y, z))
    }
    if (nbt.hasKey(Settings.namespace + "player")) {
      player = Option(nbt.getString(Settings.namespace + "player"))
    }
  }

  override def save(nbt: NBTTagCompound): Unit = {
    super.save(nbt)
    remoteNodePosition.foreach {
      case (x, y, z) =>
        nbt.setInteger(Settings.namespace + "remoteX", x)
        nbt.setInteger(Settings.namespace + "remoteY", y)
        nbt.setInteger(Settings.namespace + "remoteZ", z)
    }
    player.foreach(nbt.setString(Settings.namespace + "player", _))
  }
}

object DebugCard {

  import li.cil.oc.util.ResultWrapper.result

  def checkEnabled() = if (!Settings.get.enableDebugCard) throw new Exception("debug card functionality is disabled")

  class PlayerValue(var name: String) extends prefab.AbstractValue {
    def this() = this("") // For loading.

    // ----------------------------------------------------------------------- //

    def withPlayer(f: (EntityPlayerMP) => Array[AnyRef]) = {
      checkEnabled()
      MinecraftServer.getServer.getConfigurationManager.func_152612_a(name) match {
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
      withPlayer(player => result(player.theItemInWorldManager.getGameType.getName))

    @Callback(doc = """function(gametype:string) -- Set the player's game type (survival, creative, adventure).""")
    def setGameType(context: Context, args: Arguments): Array[AnyRef] =
      withPlayer(player => {
        player.setGameType(GameType.getByName(args.checkString(0).toLowerCase))
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

    // ----------------------------------------------------------------------- //

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      name = nbt.getString("name")
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      nbt.setString("name", name)
    }
  }

  class WorldValue(var world: World) extends prefab.AbstractValue {
    def this() = this(null) // For loading.

    // ----------------------------------------------------------------------- //

    @Callback(doc = """function():number -- Gets the numeric id of the current dimension.""")
    def getDimensionId(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.provider.dimensionId)
    }

    @Callback(doc = """function():string -- Gets the name of the current dimension.""")
    def getDimensionName(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.provider.getDimensionName)
    }

    @Callback(doc = """function():number -- Gets the seed of the world.""")
    def getSeed(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.getSeed)
    }

    @Callback(doc = """function():boolean -- Returns whether it is currently raining.""")
    def isRaining(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.isRaining)
    }

    @Callback(doc = """function(value:boolean) -- Sets whether it is currently raining.""")
    def setRaining(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      world.getWorldInfo.setRaining(args.checkBoolean(0))
      null
    }

    @Callback(doc = """function():boolean -- Returns whether it is currently thundering.""")
    def isThundering(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.isThundering)
    }

    @Callback(doc = """function(value:boolean) -- Sets whether it is currently thundering.""")
    def setThundering(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      world.getWorldInfo.setThundering(args.checkBoolean(0))
      null
    }

    @Callback(doc = """function():number -- Get the current world time.""")
    def getTime(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.getWorldTime)
    }

    @Callback(doc = """function(value:number) -- Set the current world time.""")
    def setTime(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      world.setWorldTime(args.checkDouble(0).toLong)
      null
    }

    @Callback(doc = """function():number, number, number -- Get the current spawn point coordinates.""")
    def getSpawnPoint(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.getWorldInfo.getSpawnX, world.getWorldInfo.getSpawnY, world.getWorldInfo.getSpawnZ)
    }

    @Callback(doc = """function(x:number, y:number, z:number) -- Set the spawn point coordinates.""")
    def setSpawnPoint(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      world.getWorldInfo.setSpawnPosition(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      null
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the ID of the block at the specified coordinates.""")
    def getBlockId(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(Block.getIdFromBlock(world.getBlock(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the metadata of the block at the specified coordinates.""")
    def getMetadata(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.getBlockMetadata(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates is loaded.""")
    def isLoaded(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.blockExists(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates has a tile entity.""")
    def hasTileEntity(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      val (x, y, z) = (args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val block = world.getBlock(x, y, z)
      result(block != null && block.hasTileEntity(world.getBlockMetadata(x, y, z)))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the light opacity of the block at the specified coordinates.""")
    def getLightOpacity(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.getBlockLightOpacity(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the light value (emission) of the block at the specified coordinates.""")
    def getLightValue(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.getBlockLightValue(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))
    }

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get whether the block at the specified coordinates is directly under the sky.""")
    def canSeeSky(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      result(world.canBlockSeeTheSky(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))
    }

    @Callback(doc = """function(x:number, y:number, z:number, id:number or string, meta:number):number -- Set the block at the specified coordinates.""")
    def setBlock(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      val block = if (args.isInteger(3)) Block.getBlockById(args.checkInteger(3)) else Block.getBlockFromName(args.checkString(3))
      val metadata = args.checkInteger(4)
      result(world.setBlock(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2), block, metadata, 3))
    }

    @Callback(doc = """function(x1:number, y1:number, z1:number, x2:number, y2:number, z2:number, id:number or string, meta:number):number -- Set all blocks in the area defined by the two corner points (x1, y1, z1) and (x2, y2, z2).""")
    def setBlocks(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      val (xMin, yMin, zMin) = (args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val (xMax, yMax, zMax) = (args.checkInteger(3), args.checkInteger(4), args.checkInteger(5))
      val block = if (args.isInteger(6)) Block.getBlockById(args.checkInteger(6)) else Block.getBlockFromName(args.checkString(6))
      val metadata = args.checkInteger(7)
      for (x <- math.min(xMin, xMax) to math.max(xMin, xMax)) {
        for (y <- math.min(yMin, yMax) to math.max(yMin, yMax)) {
          for (z <- math.min(zMin, zMax) to math.max(zMin, zMax)) {
            world.setBlock(x, y, z, block, metadata, 3)
          }
        }
      }
      null
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = """function(id:string, count:number, damage:number, nbt:string, x:number, y:number, z:number, side:number):boolean - Insert an item stack into the inventory at the specified location. NBT tag is expected in JSON format.""")
    def insertItem(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      val item = Item.itemRegistry.getObject(args.checkString(0)).asInstanceOf[Item]
      if (item == null) {
        throw new IllegalArgumentException("invalid item id")
      }
      val count = args.checkInteger(1)
      val damage = args.checkInteger(2)
      val tagJson = args.checkString(3)
      val tag = if (Strings.isNullOrEmpty(tagJson)) null else JsonToNBT.func_150315_a(tagJson).asInstanceOf[NBTTagCompound]
      val position = BlockPosition(args.checkDouble(4), args.checkDouble(5), args.checkDouble(6), world)
      val side = args.checkSide(7, ForgeDirection.VALID_DIRECTIONS: _*)
      InventoryUtils.inventoryAt(position) match {
        case Some(inventory) =>
          val stack = new ItemStack(item, count, damage)
          stack.setTagCompound(tag)
          result(InventoryUtils.insertIntoInventory(stack, inventory, Option(side)))
        case _ => result(Unit, "no inventory")
      }
    }

    @Callback(doc = """function(x:number, y:number, z:number, slot:number[, count:number]):number - Reduce the size of an item stack in the inventory at the specified location.""")
    def removeItem(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      val position = BlockPosition(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2), world)
      InventoryUtils.inventoryAt(position) match {
        case Some(inventory) =>
          val slot = args.checkSlot(inventory, 3)
          val count = args.optInteger(4, inventory.getInventoryStackLimit)
          val removed = inventory.decrStackSize(slot, count)
          if (removed == null) result(0)
          else result(removed.stackSize)
        case _ => result(null, "no inventory")
      }
    }

    @Callback(doc = """function(id:string, amount:number, x:number, y:number, z:number, side:number):boolean - Insert some fluid into the tank at the specified location.""")
    def insertFluid(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      val fluid = FluidRegistry.getFluid(args.checkString(0))
      if (fluid == null) {
        throw new IllegalArgumentException("invalid fluid id")
      }
      val amount = args.checkInteger(1)
      val position = BlockPosition(args.checkDouble(2), args.checkDouble(3), args.checkDouble(4), world)
      val side = args.checkSide(5, ForgeDirection.VALID_DIRECTIONS: _*)
      world.getTileEntity(position) match {
        case handler: IFluidHandler => result(handler.fill(side, new FluidStack(fluid, amount), true))
        case _ => result(null, "no tank")
      }
    }

    @Callback(doc = """function(amount:number, x:number, y:number, z:number, side:number):boolean - Remove some fluid from a tank at the specified location.""")
    def removeFluid(context: Context, args: Arguments): Array[AnyRef] = {
      checkEnabled()
      val amount = args.checkInteger(0)
      val position = BlockPosition(args.checkDouble(1), args.checkDouble(2), args.checkDouble(3), world)
      val side = args.checkSide(4, ForgeDirection.VALID_DIRECTIONS: _*)
      world.getTileEntity(position) match {
        case handler: IFluidHandler => result(handler.drain(side, amount, true))
        case _ => result(null, "no tank")
      }
    }

    // ----------------------------------------------------------------------- //

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      world = DimensionManager.getWorld(nbt.getInteger("dimension"))
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      nbt.setInteger("dimension", world.provider.dimensionId)
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

    override def getCommandSenderName = underlying.getCommandSenderName

    override def getEntityWorld = host.world

    override def addChatMessage(message: IChatComponent) {
      messages = Option(messages.fold("")(_ + "\n") + message.getUnformattedText)
    }

    override def canCommandSenderUseCommand(level: Int, command: String) = {
      val profile = underlying.getGameProfile
      val server = underlying.mcServer
      val config = server.getConfigurationManager
      server.isSinglePlayer || (config.func_152596_g(profile) && (config.func_152603_m.func_152683_b(profile) match {
        case entry: UserListOpsEntry => entry.func_152644_a >= level
        case _ => server.getOpPermissionLevel >= level
      }))
    }

    override def getPlayerCoordinates = BlockPosition(host).toChunkCoordinates

    override def func_145748_c_() = underlying.func_145748_c_()
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

    override def load(nbt: NBTTagCompound): Unit = {
      super.load(nbt)
      value = nbt.getString("value")
    }

    override def save(nbt: NBTTagCompound): Unit = {
      super.save(nbt)
      nbt.setString("value", value)
    }
  }

}
