package li.cil.oc.common.block

import java.text.DecimalFormat
import java.util

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.util.Tooltip
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class PowerConverter extends SimpleBlock with traits.PowerAcceptor {
  if (Settings.get.ignorePower) {
    setCreativeTab(null)
    ItemBlacklist.hide(this)
  }

  private val formatter = new DecimalFormat("#.#")

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, world: World, tooltip: util.List[String], advanced: ITooltipFlag) {
    super.tooltipTail(metadata, stack, world, tooltip, advanced)
// TODO more generic way of integration modules of power providing mods to provide tooltip lines
//    if (Mods.Factorization.isAvailable) {
//      addRatio(tooltip, "Factorization", Settings.get.ratioFactorization)
//    }
    if (Mods.IndustrialCraft2.isModAvailable) {
      addRatio(tooltip, "IndustrialCraft2", Settings.get.ratioIndustrialCraft2)
    }
  }

  private def addExtension(x: Double) =
    if (x >= 1e9) formatter.format(x / 1e9) + "G"
    else if (x >= 1e6) formatter.format(x / 1e6) + "M"
    else if (x >= 1e3) formatter.format(x / 1e3) + "K"
    else formatter.format(x)

  private def addRatio(tooltip: util.List[String], name: String, ratio: Double) {
    val (a, b) =
      if (ratio > 1) (1.0, ratio)
      else (1.0 / ratio, 1.0)
    tooltip.addAll(Tooltip.get(getClass.getSimpleName.toLowerCase + "." + name, addExtension(a), addExtension(b)))
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput: Double = Settings.get.powerConverterRate

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.PowerConverter()
}
