package li.cil.oc.common.item

import java.util

import com.google.common.base.Strings
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.api
import li.cil.oc.common.item.data.NanomachineData
import li.cil.oc.common.nanomachines.ControllerImpl
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumAction
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Nanomachines(val parent: Delegator) extends traits.Delegate {
  override def rarity(stack: ItemStack): EnumRarity = EnumRarity.uncommon

  @SideOnly(Side.CLIENT)
  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean): Unit = {
    super.tooltipLines(stack, player, tooltip, advanced)
    if (stack.hasTagCompound) {
      val data = new NanomachineData(stack)
      if (!Strings.isNullOrEmpty(data.uuid)) {
        tooltip.add("§8" + data.uuid.substring(0, 13) + "...§7")
      }
    }
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    player.setItemInUse(stack, getMaxItemUseDuration(stack))
    stack
  }

  override def getItemUseAction(stack: ItemStack): EnumAction = EnumAction.eat

  override def getMaxItemUseDuration(stack: ItemStack): Int = 32

  override def onEaten(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
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
    stack.stackSize -= 1
    if (stack.stackSize > 0) stack
    else null
  }
}
