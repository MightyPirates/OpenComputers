package li.cil.oc.common.block

import java.text.DecimalFormat
import java.util

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.TileEntityPowerConverter
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class PowerConverter extends AbstractBlock with traits.PowerAcceptor {
  if (Settings.Power.shouldIgnorePower()) {
    setCreativeTab(null)
    ItemBlacklist.hide(this)
  }

  private val formatter = new DecimalFormat("#.#")

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
// TODO more generic way of integration modules of power providing mods to provide tooltip lines
//    if (Mods.Factorization.isAvailable) {
//      addRatio(tooltip, "Factorization", Settings.get.ratioFactorization)
//    }
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
    tooltip.addAll(Tooltip.get(getClass.getSimpleName + "." + name, addExtension(a), addExtension(b)))
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.Power.Rate.powerConverterRate

  override def createNewTileEntity(world: World, metadata: Int) = new TileEntityPowerConverter()
}
