package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.Container
import li.cil.oc.api.network.{Arguments, Callback, Context, Visibility}
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.common.component
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.DimensionManager

import scala.math.ScalaNumber

class DebugCard(owner: Container) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("debug").
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(value:number):number -- Changes the component network's energy buffer by the specified delta.""")
  def changeBuffer(context: Context, args: Arguments): Array[AnyRef] = result(node.changeBuffer(args.checkDouble(0)))

  @Callback(doc = """function():number -- Get the container's X position in the world.""")
  def getX(context: Context, args: Arguments): Array[AnyRef] = result(owner.xPosition)

  @Callback(doc = """function():number -- Get the container's Y position in the world.""")
  def getY(context: Context, args: Arguments): Array[AnyRef] = result(owner.yPosition)

  @Callback(doc = """function():number -- Get the container's Z position in the world.""")
  def getZ(context: Context, args: Arguments): Array[AnyRef] = result(owner.zPosition)

  @Callback(doc = """function():userdata -- Get the container's world object.""")
  def getWorld(context: Context, args: Arguments): Array[AnyRef] = result(new DebugCard.WorldValue(owner.world))
}

object DebugCard {

  class WorldValue(var world: World) extends AbstractValue {
    def this() = this(null) // For loading.

    // ----------------------------------------------------------------------- //

    @Callback(doc = """function():number -- Gets the numeric id of the current dimension.""")
    def getDimensionId(context: Context, args: Arguments): Array[AnyRef] =
      result(world.provider.dimensionId)

    @Callback(doc = """function():string -- Gets the name of the current dimension.""")
    def getDimensionName(context: Context, args: Arguments): Array[AnyRef] =
      result(world.provider.getDimensionName)

    @Callback(doc = """function():number -- Gets the seed of the world.""")
    def getSeed(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getSeed)

    @Callback(doc = """function():boolean -- Returns whether it is currently raining.""")
    def isRaining(context: Context, args: Arguments): Array[AnyRef] =
      result(world.isRaining)

    @Callback(doc = """function(value:boolean) -- Sets whether it is currently raining.""")
    def setRaining(context: Context, args: Arguments): Array[AnyRef] = {
      world.getWorldInfo.setRaining(args.checkBoolean(0))
      null
    }

    @Callback(doc = """function():boolean -- Returns whether it is currently thundering.""")
    def isThundering(context: Context, args: Arguments): Array[AnyRef] =
      result(world.isThundering)

    @Callback(doc = """function(value:boolean) -- Sets whether it is currently thundering.""")
    def setThundering(context: Context, args: Arguments): Array[AnyRef] = {
      world.getWorldInfo.setThundering(args.checkBoolean(0))
      null
    }

    @Callback(doc = """function():number -- Get the current world time.""")
    def getTime(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getWorldTime)

    @Callback(doc = """function(value:number) -- Set the current world time.""")
    def setTime(context: Context, args: Arguments): Array[AnyRef] = {
      world.setWorldTime(args.checkDouble(0).toLong)
      null
    }

    @Callback(doc = """function():number, number, number -- Get the current spawn point coordinates.""")
    def getSpawnPoint(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getWorldInfo.getSpawnX, world.getWorldInfo.getSpawnY, world.getWorldInfo.getSpawnZ)

    @Callback(doc = """function(x:number, y:number, z:number) -- Set the spawn point coordinates.""")
    def setSpawnPoint(context: Context, args: Arguments): Array[AnyRef] = {
      world.getWorldInfo.setSpawnPosition(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      null
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the ID of the block at the specified coordinates.""")
    def getBlockId(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getBlockId(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the metadata of the block at the specified coordinates.""")
    def getMetadata(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getBlockMetadata(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback(doc = """function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates is loaded.""")
    def isLoaded(context: Context, args: Arguments): Array[AnyRef] =
      result(world.blockExists(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback(doc = """function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates has a tile entity.""")
    def hasTileEntity(context: Context, args: Arguments): Array[AnyRef] =
      result(world.blockHasTileEntity(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the light opacity of the block at the specified coordinates.""")
    def getLightOpacity(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getBlockLightOpacity(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get the light value (emission) of the block at the specified coordinates.""")
    def getLightValue(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getBlockLightValue(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback(doc = """function(x:number, y:number, z:number):number -- Get whether the block at the specified coordinates is directly under the sky.""")
    def canSeeSky(context: Context, args: Arguments): Array[AnyRef] =
      result(world.canBlockSeeTheSky(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback(doc = """function(x:number, y:number, z:number, id:number, meta:number):number -- Set the block at the specified coordinates.""")
    def setBlock(context: Context, args: Arguments): Array[AnyRef] =
      result(world.setBlock(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2), args.checkInteger(3), args.checkInteger(4), 3))

    @Callback(doc = """function(x1:number, y1:number, z1:number, x2:number, y2:number, z2:number, id:number, meta:number):number -- Set all blocks in the area defined by the two corner points (x1, y1, z1) and (x2, y2, z2).""")
    def setBlocks(context: Context, args: Arguments): Array[AnyRef] = {
      val (xMin, yMin, zMin) = (args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val (xMax, yMax, zMax) = (args.checkInteger(3), args.checkInteger(4), args.checkInteger(5))
      val (blockId, metadata) = (args.checkInteger(6), args.checkInteger(7))
      for (x <- math.min(xMin, xMax) to math.max(xMin, xMax)) {
        for (y <- math.min(yMin, yMax) to math.max(yMin, yMax)) {
          for (z <- math.min(zMin, zMax) to math.max(zMin, zMax)) {
            world.setBlock(x, y, z, blockId, metadata, 3)
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

    // ----------------------------------------------------------------------- //

    final protected def result(args: Any*): Array[AnyRef] = {
      def unwrap(arg: Any): AnyRef = arg match {
        case x: ScalaNumber => x.underlying
        case x => x.asInstanceOf[AnyRef]
      }
      Array(args map unwrap: _*)
    }
  }

}