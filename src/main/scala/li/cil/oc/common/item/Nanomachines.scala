package li.cil.oc.common.item

import java.util

import com.google.common.base.Strings
import li.cil.oc.api
import li.cil.oc.common.item.data.NanomachineData
import li.cil.oc.common.nanomachines.ControllerImpl
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumAction
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Nanomachines(val parent: Delegator) extends traits.Delegate {
  override def rarity(stack: ItemStack): EnumRarity = EnumRarity.UNCOMMON

  @SideOnly(Side.CLIENT)
  override def tooltipLines(stack: ItemStack, world: World, tooltip: util.List[String], flag: ITooltipFlag): Unit = {
    super.tooltipLines(stack, world, tooltip, flag)
    if (stack.hasTagCompound) {
      val data = new NanomachineData(stack)
      if (!Strings.isNullOrEmpty(data.uuid)) {
        tooltip.add("§8" + data.uuid.substring(0, 13) + "...§7")
      }
    }
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ActionResult[ItemStack] = {
    player.setActiveHand(if (player.getHeldItemMainhand == stack) EnumHand.MAIN_HAND else EnumHand.OFF_HAND)
    ActionResult.newResult(EnumActionResult.SUCCESS, stack)
  }

  override def getItemUseAction(stack: ItemStack): EnumAction = EnumAction.EAT

  override def getMaxItemUseDuration(stack: ItemStack): Int = 32

  override def onItemUseFinish(stack: ItemStack, world: World, entity: EntityLivingBase): ItemStack = {
    entity match {
      case player: EntityPlayer =>
        if (!world.isRemote) {
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
                  controller.configuration.load(nbt)
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
