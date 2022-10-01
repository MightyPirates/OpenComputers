package li.cil.oc.common.item

import java.util

import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.util.BlockPosition
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Direction
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.extensions.IForgeItem

class Manual(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    super.appendHoverText(stack, world, tooltip, flag)
    tooltip.add(new StringTextComponent(TextFormatting.DARK_GRAY.toString + "v" + OpenComputers.get.Version))
  }

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (world.isClientSide) {
      if (player.isCrouching) {
        api.Manual.reset()
      }
      api.Manual.openFor(player)
    }
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }

  override def onItemUse(stack: ItemStack, player: PlayerEntity, position: BlockPosition, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    val world = player.level
    api.Manual.pathFor(world, position.toBlockPos) match {
      case path: String =>
        if (world.isClientSide) {
          api.Manual.openFor(player)
          api.Manual.reset()
          api.Manual.navigate(path)
        }
        true
      case _ => super.onItemUse(stack, player, position, side, hitX, hitY, hitZ)
    }
  }
}
