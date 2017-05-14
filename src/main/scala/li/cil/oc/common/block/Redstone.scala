package li.cil.oc.common.block

import java.util

import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Redstone extends RedstoneAware {
  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    if (Mods.ProjectRedTransmission.isModAvailable) {
      tooltip.addAll(Tooltip.get("RedstoneCard.ProjectRed"))
    }
    if (Mods.RedLogic.isModAvailable) {
      tooltip.addAll(Tooltip.get("RedstoneCard.RedLogic"))
    }
    if (Mods.MineFactoryReloaded.isModAvailable) {
      tooltip.addAll(Tooltip.get("RedstoneCard.RedNet"))
    }
  }

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Redstone()
}
