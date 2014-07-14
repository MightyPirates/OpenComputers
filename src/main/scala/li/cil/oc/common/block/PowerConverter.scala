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
import net.minecraftforge.common.ForgeDirection

class PowerConverter(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "PowerConverter"

  showInItemList = !Settings.get.ignorePower

  private val icons = Array.fill[Icon](6)(null)

  private val formatter = new DecimalFormat("#.#")

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    def addExtension(x: Double) =
      if (x >= 1e9) formatter.format(x / 1e9) + "G"
      else if (x >= 1e6) formatter.format(x / 1e6) + "M"
      else if (x >= 1e3) formatter.format(x / 1e3) + "K"
      else formatter.format(x)
    def addRatio(name: String, ratio: Double) {
      val (a, b) =
        if (ratio > 1) (1.0, ratio)
        else (1.0 / ratio, 1.0)
      tooltip.addAll(Tooltip.get(unlocalizedName + "." + name, addExtension(a), addExtension(b)))
    }
    if (Mods.BuildCraftPower.isAvailable) {
      addRatio("BC", Settings.ratioBC)
    }
    if (Mods.IndustrialCraft2.isAvailable) {
      addRatio("IC2", Settings.ratioIC2)
    }
    if (Mods.ThermalExpansion.isAvailable) {
      addRatio("TE", Settings.ratioTE)
    }
    if (Mods.UniversalElectricity.isAvailable) {
      addRatio("UE", Settings.ratioUE)
    }
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":power_converter")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.PowerConverter)
}
