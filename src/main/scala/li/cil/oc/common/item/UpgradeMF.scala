package li.cil.oc.common.item

import java.util

import li.cil.oc.CreativeTab
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.common.extensions.IForgeItem

class UpgradeMF(props: Properties = new Properties().tab(CreativeTab)) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier {
  override def onItemUseFirst(stack: ItemStack, player: PlayerEntity, world: World, pos: BlockPos, side: Direction, hitX: Float, hitY: Float, hitZ: Float, hand: Hand): ActionResultType = {
    if (!player.level.isClientSide && player.isCrouching) {
      val data = stack.getOrCreateTag
      data.putString(Settings.namespace + "dimension", world.dimension.location.toString)
      data.putIntArray(Settings.namespace + "coord", Array(pos.getX, pos.getY, pos.getZ, side.ordinal()))
      return ActionResultType.sidedSuccess(player.level.isClientSide)
    }
    super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand)
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[ITextComponent]) {
    tooltip.add(new StringTextComponent(Localization.Tooltip.MFULinked(stack.getTag match {
      case data: CompoundNBT => data.contains(Settings.namespace + "coord")
      case _ => false
    })).setStyle(Tooltip.DefaultStyle))
  }
}
