package li.cil.oc.common.block

import cpw.mods.fml.common.Loader
import java.text.DecimalFormat
import java.util
import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class PowerConverter(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "PowerConverter"

  private val icons = Array.fill[IIcon](6)(null)

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
    if (Loader.isModLoaded("BuildCraft|Energy")) {
      addRatio("BC", 1)
    }
    if (Loader.isModLoaded("IC2")) {
      addRatio("IC2", Settings.ratioIC2 / Settings.ratioBC)
    }
    if (Loader.isModLoaded("ThermalExpansion")) {
      addRatio("TE", Settings.ratioTE / Settings.ratioBC)
    }
    addRatio("UE", 1.0 / Settings.ratioBC)
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal))

  override def registerIcons(iconRegister: IIconRegister) = {
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
