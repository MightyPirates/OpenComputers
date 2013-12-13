package li.cil.oc.common.block

import cpw.mods.fml.common.Loader
import java.text.DecimalFormat
import java.util
import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Icon
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class PowerConverter(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "PowerConverter"

  private val icons = Array.fill[Icon](6)(null)

  private val formatter = new DecimalFormat("#.#")

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    def addRatio(name: String, ratio: Float) {
      val (a, b) =
        if (ratio > 1) (1f, ratio)
        else (1f / ratio, 1f)
      tooltip.addAll(Tooltip.get(unlocalizedName + "." + name, formatter.format(a), formatter.format(b)))
    }
    if (Loader.isModLoaded("BuildCraft|Energy")) {
      addRatio("BC", Settings.get.ratioBuildCraft)
    }
    if (Loader.isModLoaded("IC2")) {
      addRatio("IC2", Settings.get.ratioIndustrialCraft2)
    }
    if (Loader.isModLoaded("ThermalExpansion")) {
      addRatio("TE", Settings.get.ratioThermalExpansion)
    }
    {
      addRatio("UE", Settings.get.ratioUniversalElectricity)
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
