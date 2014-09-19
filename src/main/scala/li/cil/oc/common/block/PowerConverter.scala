package li.cil.oc.common.block

import java.text.DecimalFormat
import java.util

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.Mods
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class PowerConverter extends SimpleBlock {
  showInItemList = !Settings.get.ignorePower

  private val formatter = new DecimalFormat("#.#")

  // ----------------------------------------------------------------------- //

  override protected def customTextures = Array(
    None,
    None,
    Some("PowerConverterSide"),
    Some("PowerConverterSide"),
    Some("PowerConverterSide"),
    Some("PowerConverterSide")
  )

  // ----------------------------------------------------------------------- //

  override def addInformation(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.addInformation(metadata, stack, player, tooltip, advanced)
    def addExtension(x: Double) =
      if (x >= 1e9) formatter.format(x / 1e9) + "G"
      else if (x >= 1e6) formatter.format(x / 1e6) + "M"
      else if (x >= 1e3) formatter.format(x / 1e3) + "K"
      else formatter.format(x)
    def addRatio(name: String, ratio: Double) {
      val (a, b) =
        if (ratio > 1) (1.0, ratio)
        else (1.0 / ratio, 1.0)
      tooltip.addAll(Tooltip.get(getUnlocalizedName + "." + name, addExtension(a), addExtension(b)))
    }
    if (Mods.BuildCraftPower.isAvailable) {
      addRatio("BuildCraft", Settings.get.ratioBuildCraft)
    }
    if (Mods.Factorization.isAvailable) {
      addRatio("Factorization", Settings.get.ratioFactorization)
    }
    if (Mods.IndustrialCraft2.isAvailable || Mods.IndustrialCraft2Classic.isAvailable) {
      addRatio("IndustrialCraft2", Settings.get.ratioIndustrialCraft2)
    }
    if (Mods.Mekanism.isAvailable) {
      addRatio("Mekanism", Settings.get.ratioMekanism)
    }
    if (Mods.RedstoneFlux.isAvailable) {
      addRatio("ThermalExpansion", Settings.get.ratioRedstoneFlux)
    }
    if (Mods.UniversalElectricity.isAvailable) {
      addRatio("UniversalElectricity", Settings.get.ratioUniversalElectricity)
    }
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.PowerConverter()
}
