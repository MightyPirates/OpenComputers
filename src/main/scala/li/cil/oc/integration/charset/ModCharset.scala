package li.cil.oc.integration.charset

import li.cil.oc.integration.{ModProxy, Mods}
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.util.BlockPosition
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import pl.asie.charset.api.wires.{IBundledEmitter, IBundledReceiver, IRedstoneEmitter}

object ModCharset extends ModProxy with RedstoneProvider {
  class BundledRedstoneView(val data: Array[Int], val onChange: () => Unit) extends IBundledEmitter with IBundledReceiver {
    override def getBundledSignal: Array[Byte] = data.map(i => i.toByte)

    override def onBundledInputChange(): Unit = { onChange() }
  }

  override def getMod = Mods.Charset

  override def initialize(): Unit = {
    BundledRedstone.addProvider(this)
  }

  override def computeInput(pos: BlockPosition, side: EnumFacing): Int = {
    val world = pos.world.get
    val npos = pos.toBlockPos.offset(side)
    world.getTileEntity(npos) match {
      case tile: TileEntity =>
        if (tile.hasCapability(CapabilitiesCharset.REDSTONE_EMITTER, side.getOpposite)) {
          tile.getCapability(CapabilitiesCharset.REDSTONE_EMITTER, side.getOpposite) match {
            case emitter: IRedstoneEmitter => return math.min(emitter.getRedstoneSignal, 15)
          }
        }
        0
      case _ => 0
    }
  }

  def computeBundledInput(pos: BlockPosition, side: EnumFacing): Array[Int] = {
    val world = pos.world.get
    val npos = pos.toBlockPos.offset(side)
    world.getTileEntity(npos) match {
      case tile: TileEntity =>
        if (tile.hasCapability(CapabilitiesCharset.BUNDLED_EMITTER, side.getOpposite)) {
          tile.getCapability(CapabilitiesCharset.BUNDLED_EMITTER, side.getOpposite) match {
            case emitter: IBundledEmitter =>
              return emitter.getBundledSignal.map(i => i.toInt & 0xFF)
          }
        }
        null
      case _ => null
    }
  }
}
