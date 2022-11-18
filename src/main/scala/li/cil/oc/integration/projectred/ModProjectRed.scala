package li.cil.oc.integration.projectred

import li.cil.oc.api
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.util.BlockPosition
import mrtjp.projectred.api.ProjectRedAPI
import net.minecraft.util.Direction
import net.minecraft.world.World

object ModProjectRed extends ModProxy with RedstoneProvider {
  override def getMod = Mods.ProjectRedTransmission

  override def initialize(): Unit = {
    api.IMC.registerWrenchTool("li.cil.oc.integration.projectred.EventHandlerProjectRed.useWrench")
    api.IMC.registerWrenchToolCheck("li.cil.oc.integration.projectred.EventHandlerProjectRed.isWrench")

    BundledRedstone.addProvider(this)
    BundledProviderProjectRed.install()
  }

  override def computeInput(pos: BlockPosition, side: Direction): Int = 0

  def computeBundledInput(pos: BlockPosition, side: Direction): Array[Int] = {
    Option(ProjectRedAPI.transmissionAPI.getBundledInput(pos.world.get, pos.toBlockPos, side)).
      fold(null: Array[Int])(_.map(_ & 0xFF))
  }
}
