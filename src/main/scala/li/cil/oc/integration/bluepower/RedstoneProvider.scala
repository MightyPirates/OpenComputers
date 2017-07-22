package li.cil.oc.integration.bluepower

import com.bluepowermod.api.BPApi
import com.bluepowermod.api.wire.redstone.IBundledDevice
import com.bluepowermod.api.wire.redstone.IRedstoneDevice
import com.bluepowermod.api.wire.redstone.IRedstoneProvider
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import li.cil.oc.common.tileentity.traits.RedstoneAware
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.util.BlockPosition
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable

object RedstoneProvider extends IRedstoneProvider with RedstoneProvider {
  val redstoneDevices = mutable.WeakHashMap.empty[RedstoneAware, RedstoneDevice]

  val bundledRedstoneDevices = mutable.WeakHashMap.empty[BundledRedstoneAware, BundledRedstoneDevice]

  def init(): Unit = {
    BPApi.getInstance.getRedstoneApi.registerRedstoneProvider(this)

    BundledRedstone.addProvider(this)
  }

  override def getRedstoneDeviceAt(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, face: ForgeDirection): IRedstoneDevice = {
    world.getTileEntity(x, y, z) match {
      case tileEntity: RedstoneAware => redstoneDevices.getOrElseUpdate(tileEntity, new RedstoneDevice(tileEntity))
      case _ => null
    }
  }

  override def getBundledDeviceAt(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, face: ForgeDirection): IBundledDevice = {
    world.getTileEntity(x, y, z) match {
      case tileEntity: BundledRedstoneAware => bundledRedstoneDevices.getOrElseUpdate(tileEntity, new BundledRedstoneDevice(tileEntity))
      case _ => null
    }
  }

  override def computeInput(pos: BlockPosition, side: ForgeDirection): Int = {
    val world = pos.world.get
    val (nx, ny, nz) = (pos.x + side.offsetX, pos.y + side.offsetY, pos.z + side.offsetZ)
    ForgeDirection.values.map(BPApi.getInstance.getRedstoneApi.getRedstoneDevice(world, nx, ny, nz, _, ForgeDirection.UNKNOWN)).collect {
      case device: IRedstoneDevice => device.getRedstonePower(side.getOpposite) & 0xFF
    }.padTo(1, 0).max
  }

  def computeBundledInput(pos: BlockPosition, side: ForgeDirection): Array[Int] = {
    val world = pos.world.get
    val (nx, ny, nz) = (pos.x + side.offsetX, pos.y + side.offsetY, pos.z + side.offsetZ)
    val inputs = ForgeDirection.values.map(BPApi.getInstance.getRedstoneApi.getBundledDevice(world, nx, ny, nz, _, ForgeDirection.UNKNOWN)).collect {
      case device: IBundledDevice => Option(device.getBundledOutput(side.getOpposite)).fold(null: Array[Int])(_.map(_ & 0xFF))
    }.filter(_ != null)
    if (inputs.isEmpty) null
    else inputs.reduce((a, b) => (a, b).zipped.map((l, r) => math.max(l, r)))
  }
}
