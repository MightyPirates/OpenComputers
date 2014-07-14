package li.cil.oc.common.block

import java.util

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.Mods
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Redstone(val parent: SimpleDelegator) extends RedstoneAware with SimpleDelegate {
  val unlocalizedName = "Redstone"

  private val icons = Array.fill[Icon](6)(null)

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
    if (Mods.RedLogic.isAvailable) {
      tooltip.addAll(Tooltip.get("RedstoneCard.RedLogic"))
    }
    if (Mods.MineFactoryReloaded.isAvailable) {
      tooltip.addAll(Tooltip.get("RedstoneCard.RedNet"))
    }
  }

  override def icon(side: ForgeDirection) = Some(icons(side.ordinal()))

  override def registerIcons(iconRegister: IconRegister) = {
    icons(ForgeDirection.DOWN.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":redstone_top")
    icons(ForgeDirection.UP.ordinal) = icons(ForgeDirection.DOWN.ordinal)

    icons(ForgeDirection.NORTH.ordinal) = iconRegister.registerIcon(Settings.resourceDomain + ":redstone")
    icons(ForgeDirection.SOUTH.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.WEST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
    icons(ForgeDirection.EAST.ordinal) = icons(ForgeDirection.NORTH.ordinal)
  }

  override def createTileEntity(world: World) = Some(new tileentity.Redstone())
}
