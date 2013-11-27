package li.cil.oc.common.block

import java.util
import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Icon
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection
import cpw.mods.fml.common.Loader

class PowerConverter(val parent: SimpleDelegator) extends SimpleDelegate {
  val unlocalizedName = "PowerConverter"

  private val icons = Array.fill[Icon](6)(null)

  // ----------------------------------------------------------------------- //

  override def addInformation(player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    if (Loader.isModLoaded("IC2")) {
      val ratio = Settings.get.ratioIndustrialCraft2
      val (a, b) =
        if (ratio > 1) (1f, ratio.ceil)
        else ((1f / ratio).ceil, 1f)
      tooltip.addAll(Tooltip.get(unlocalizedName + ".IC2", a.toInt, b.toInt))
    }
    if (Loader.isModLoaded("BuildCraft|Energy")) {
      val ratio = Settings.get.ratioBuildCraft
      val (a, b) =
        if (ratio > 1) (1f, ratio.ceil)
        else ((1f / ratio).ceil, 1f)
      tooltip.addAll(Tooltip.get(unlocalizedName + ".BC", a.toInt, b.toInt))
    }
    {
      val ratio = Settings.get.ratioUniversalElectricity
      val (a, b) =
        if (ratio > 1) (1f, ratio.ceil)
        else ((1f / ratio).ceil, 1f)
      tooltip.addAll(Tooltip.get(unlocalizedName + ".UE", a.toInt, b.toInt))
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
