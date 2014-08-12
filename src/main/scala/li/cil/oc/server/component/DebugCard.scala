package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.Container
import li.cil.oc.api.network.{Arguments, Callback, Context, Visibility}
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.common.component
import net.minecraft.block.Block
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

    @Callback
    def getDimensionId(context: Context, args: Arguments): Array[AnyRef] =
      result(world.provider.dimensionId)

    @Callback
    def getDimensionName(context: Context, args: Arguments): Array[AnyRef] =
      result(world.provider.getDimensionName)

    @Callback
    def getSeed(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getSeed)

    @Callback
    def isRaining(context: Context, args: Arguments): Array[AnyRef] =
      result(world.isRaining)

    @Callback
    def setRaining(context: Context, args: Arguments): Array[AnyRef] = {
      world.getWorldInfo.setRaining(args.checkBoolean(0))
      null
    }

    @Callback
    def isThundering(context: Context, args: Arguments): Array[AnyRef] =
      result(world.isThundering)

    @Callback
    def setThundering(context: Context, args: Arguments): Array[AnyRef] = {
      world.getWorldInfo.setThundering(args.checkBoolean(0))
      null
    }

    @Callback
    def getTime(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getWorldTime)

    @Callback
    def setTime(context: Context, args: Arguments): Array[AnyRef] = {
      world.setWorldTime(args.checkDouble(0).toLong)
      null
    }

    @Callback
    def getSpawnPoint(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getWorldInfo.getSpawnX, world.getWorldInfo.getSpawnY, world.getWorldInfo.getSpawnZ)

    @Callback
    def setSpawnPoint(context: Context, args: Arguments): Array[AnyRef] = {
      world.getWorldInfo.setSpawnPosition(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      null
    }

    // ----------------------------------------------------------------------- //

    @Callback
    def getBlockId(context: Context, args: Arguments): Array[AnyRef] =
      result(Block.getIdFromBlock(world.getBlock(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))))

    @Callback
    def getMetadata(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getBlockMetadata(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback
    def isLoaded(context: Context, args: Arguments): Array[AnyRef] =
      result(world.blockExists(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback
    def hasTileEntity(context: Context, args: Arguments): Array[AnyRef] = {
      val (x, y, z) = (args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))
      val block = world.getBlock(x, y, z)
      result(block != null && block.hasTileEntity(world.getBlockMetadata(x, y, z)))
    }

    @Callback
    def getLightOpacity(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getBlockLightOpacity(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback
    def getLightValue(context: Context, args: Arguments): Array[AnyRef] =
      result(world.getBlockLightValue(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback
    def canSeeSky(context: Context, args: Arguments): Array[AnyRef] =
      result(world.canBlockSeeTheSky(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))

    @Callback
    def setBlock(context: Context, args: Arguments): Array[AnyRef] =
      result(world.setBlock(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2), Block.getBlockById(args.checkInteger(3)), args.checkInteger(4), 3))

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