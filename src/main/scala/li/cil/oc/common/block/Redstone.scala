package li.cil.oc.common.block

import java.util

import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import li.cil.oc.util.mods.Mods
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Redstone(val parent: SimpleDelegator) extends RedstoneAware with SimpleDelegate {
  override protected def customTextures = Array(
    Some("RedstoneTop"),
    Some("RedstoneTop"),
    Some("RedstoneSide"),
    Some("RedstoneSide"),
    Some("RedstoneSide"),
    Some("RedstoneSide")
  )

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipLines(stack, player, tooltip, advanced)
    if (Mods.RedLogic.isAvailable) {
      tooltip.addAll(Tooltip.get("RedstoneCard.RedLogic"))
    }
    if (Mods.MineFactoryReloaded.isAvailable) {
      tooltip.addAll(Tooltip.get("RedstoneCard.RedNet"))
    }
  }

  override def createTileEntity(world: World) = Some(new tileentity.Redstone())
}
