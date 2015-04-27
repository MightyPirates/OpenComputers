package li.cil.oc.integration.bluepower

import com.bluepowermod.api.BPApi
import com.bluepowermod.api.wire.redstone.IBundledDevice
import com.bluepowermod.api.wire.redstone.IRedstoneDevice
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.util.BlockPosition
import net.minecraftforge.common.util.ForgeDirection

object ModBluePower extends ModProxy with RedstoneProvider {
  override def getMod = Mods.BluePower

  override def initialize(): Unit = {
    RedstoneProvider.init()

    BundledRedstone.addProvider(this)
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
