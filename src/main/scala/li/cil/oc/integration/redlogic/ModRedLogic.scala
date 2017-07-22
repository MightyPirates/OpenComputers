package li.cil.oc.integration.redlogic

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import mods.immibis.redlogic.api.wiring.IBundledEmitter
import mods.immibis.redlogic.api.wiring.IInsulatedRedstoneWire
import mods.immibis.redlogic.api.wiring.IRedstoneEmitter
import net.minecraftforge.common.util.ForgeDirection

object ModRedLogic extends ModProxy with RedstoneProvider {
  override def getMod = Mods.RedLogic

  override def initialize(): Unit = {
    BundledRedstone.addProvider(this)
  }

  override def computeInput(pos: BlockPosition, side: ForgeDirection): Int = {
    pos.world.get.getTileEntity(pos.offset(side)) match {
      case emitter: IRedstoneEmitter =>
        var strength = 0
        for (i <- -1 to 5) {
          strength = math.max(strength, emitter.getEmittedSignalStrength(i, side.getOpposite.ordinal()))
        }
        strength
      case _ => 0
    }
  }

  def computeBundledInput(pos: BlockPosition, side: ForgeDirection): Array[Int] = {
    val world = pos.world.get
    val npos = pos.offset(side)
    world.getTileEntity(npos) match {
      case wire: IInsulatedRedstoneWire =>
        var strength: Array[Int] = null
        for (face <- -1 to 5 if wire.wireConnectsInDirection(face, side.ordinal())) {
          if (strength == null) strength = Array.fill(16)(0)
          strength(wire.getInsulatedWireColour) = math.max(strength(wire.getInsulatedWireColour), wire.getEmittedSignalStrength(face, side.ordinal()))
        }
        strength
      case emitter: IBundledEmitter =>
        var strength: Array[Int] = null
        for (i <- -1 to 5 if strength == null) {
          strength = Option(emitter.getBundledCableStrength(i, side.getOpposite.ordinal())).fold(null: Array[Int])(_.map(_ & 0xFF))
        }
        strength
      case _ => null
    }
  }
}
