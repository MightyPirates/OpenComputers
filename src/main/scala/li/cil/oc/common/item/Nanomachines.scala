package li.cil.oc.common.item

import java.util

import com.google.common.base.Strings
import li.cil.oc.api
import li.cil.oc.common.item.data.NanomachineData
import li.cil.oc.common.nanomachines.ControllerImpl
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.item.Rarity
import net.minecraft.item.UseAction
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.extensions.IForgeItem

class Nanomachines(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  @Deprecated
  override def getRarity(stack: ItemStack): Rarity = Rarity.UNCOMMON

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag) {
    super.appendHoverText(stack, world, tooltip, flag)
    if (stack.hasTag) {
      val data = new NanomachineData(stack)
      if (!Strings.isNullOrEmpty(data.uuid)) {
        tooltip.add(new StringTextComponent("§8" + data.uuid.substring(0, 13) + "...§7"))
      }
    }
  }

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    player.startUsingItem(if (player.getItemInHand(Hand.MAIN_HAND) == stack) Hand.MAIN_HAND else Hand.OFF_HAND)
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }

  override def getUseAnimation(stack: ItemStack): UseAction = UseAction.EAT

  override def getUseDuration(stack: ItemStack): Int = 32

  override def finishUsingItem(stack: ItemStack, world: World, entity: LivingEntity): ItemStack = {
    entity match {
      case player: PlayerEntity =>
        if (!world.isClientSide) {
          val data = new NanomachineData(stack)

          // Re-install to get new address, make sure we're configured.
          api.Nanomachines.uninstallController(player)
          api.Nanomachines.installController(player) match {
            case controller: ControllerImpl =>
              data.configuration match {
                case Some(nbt) =>
                  if (!Strings.isNullOrEmpty(data.uuid)) {
                    controller.uuid = data.uuid
                  }
                  controller.configuration.loadData(nbt)
                case _ => controller.reconfigure()
              }
            case controller => controller.reconfigure() // Huh.
          }
        }
        stack.shrink(1)
        if (stack.getCount > 0) stack
        else ItemStack.EMPTY
      case _ => stack
    }
  }
}
