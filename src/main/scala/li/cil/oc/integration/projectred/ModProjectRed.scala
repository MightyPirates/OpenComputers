package li.cil.oc.integration.projectred

import li.cil.oc.api
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.util.BlockPosition
import mrtjp.projectred.api.ProjectRedAPI
import net.minecraftforge.common.util.ForgeDirection

object ModProjectRed extends ModProxy with RedstoneProvider {
  override def getMod = Mods.ProjectRedTransmission

  override def initialize(): Unit = {
    api.IMC.registerWrenchTool("li.cil.oc.integration.projectred.EventHandlerProjectRed.useWrench")
    api.IMC.registerWrenchToolCheck("li.cil.oc.integration.projectred.EventHandlerProjectRed.isWrench")

    BundledRedstone.addProvider(this)
  }

  override def computeInput(pos: BlockPosition, side: ForgeDirection): Int = 0

  def computeBundledInput(pos: BlockPosition, side: ForgeDirection): Array[Int] = {
    Option(ProjectRedAPI.transmissionAPI.getBundledInput(pos.world.get, pos.x, pos.y, pos.z, side.ordinal)).
      fold(null: Array[Int])(_.map(_ & 0xFF))
  }
}
