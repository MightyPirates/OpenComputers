package li.cil.oc.common.item

import java.util

import li.cil.oc.util.BlockPosition
import li.cil.oc.Localization
import li.cil.oc.Settings
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent

class UpgradeMF(val parent: Delegator) extends traits.Delegate with traits.ItemTier {
  override def onItemUseFirst(stack: ItemStack, player: PlayerEntity, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float): ActionResultType = {
    if (!player.level.isClientSide && player.isCrouching) {
      val data = stack.getOrCreateTag
      data.putString(Settings.namespace + "dimension", player.level.dimension.location.toString)
      data.putIntArray(Settings.namespace + "coord", Array(position.x, position.y, position.z, side.ordinal()))
      return ActionResultType.sidedSuccess(player.level.isClientSide)
    }
    super.onItemUseFirst(stack, player, position, side, hitX, hitY, hitZ)
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[ITextComponent]) {
    tooltip.add(new StringTextComponent(Localization.Tooltip.MFULinked(stack.getTag match {
      case data: CompoundNBT => data.contains(Settings.namespace + "coord")
      case _ => false
    })))
  }
}
