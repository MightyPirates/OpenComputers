package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.server.component.DebugCard.CommandSender
import li.cil.oc.util.BlockPosition
import net.minecraft.block.Block
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.server.management.UserListOpsEntry
import net.minecraft.util.IChatComponent
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraft.world.WorldSettings.GameType
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.FakePlayerFactory

class DebugCard(host: EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("debug").
    withConnector().
    create()

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

  @Callback(doc = """function():userdata -- Get the container's world object.""")
  def getWorld(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    result(new DebugCard.WorldValue(host.world))
  }

  @Callback(doc = """function(name:string):userdata -- Get the entity of a player.""")
  def getPlayer(context: Context, args: Arguments): Array[AnyRef] = {
    checkEnabled()
    result(new DebugCard.PlayerValue(args.checkString(0)))
  }

  @Callback(doc = """function(command:string):number -- Runs an arbitrary command using a fake player.""")
  def runCommand(context: Context, args: Arguments): Array[AnyRef] = {
    val command = args.checkString(0)
    val sender = new CommandSender(host)
    val value = MinecraftServer.getServer.getCommandManager.executeCommand(sender, command)
    result(value, sender.messages.orNull)
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

    @Callback(doc = """function():userdata -- Get the container's world object.""")
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

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      world = DimensionManager.getWorld(nbt.getInteger("dimension"))
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      nbt.setInteger("dimension", world.provider.dimensionId)
    }
  }

  class CommandSender(val host: EnvironmentHost) extends ICommandSender {
    val fakePlayer = FakePlayerFactory.get(host.world.asInstanceOf[WorldServer], Settings.get.fakePlayerProfile)

    var messages: Option[String] = None

    override def getCommandSenderName = fakePlayer.getCommandSenderName

    override def getEntityWorld = host.world

    override def addChatMessage(message: IChatComponent) {
      messages = Option(messages.getOrElse("") + message.getUnformattedText)
    }

    override def canCommandSenderUseCommand(level: Int, command: String) = {
      val profile = fakePlayer.getGameProfile
      val server = fakePlayer.mcServer
      val config = server.getConfigurationManager
      config.func_152596_g(profile) && (config.func_152603_m.func_152683_b(profile) match {
        case entry: UserListOpsEntry => entry.func_152644_a >= level
        case _ => server.getOpPermissionLevel >= level
      })
    }

    override def getPlayerCoordinates = BlockPosition(host).toChunkCoordinates

    override def func_145748_c_() = fakePlayer.func_145748_c_()
  }
}
