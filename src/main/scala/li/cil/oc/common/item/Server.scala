package li.cil.oc.common.item

import java.util

import li.cil.oc.OpenComputers
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.GuiType
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.World

import scala.collection.mutable

class Server(val parent: Delegator, val tier: Int) extends traits.Delegate {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override def rarity(stack: ItemStack) = Rarity.byTier(tier)

  override def maxStackSize = 1

  private object HelperInventory extends ServerInventory {
    var container: ItemStack = null
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]) {
    super.tooltipExtended(stack, tooltip)
    if (KeyBindings.showExtendedTooltips) {
      HelperInventory.container = stack
      HelperInventory.reinitialize()
      val items = mutable.Map.empty[String, Int]
      for (item <- (0 until HelperInventory.getSizeInventory).map(HelperInventory.getStackInSlot) if item != null) {
        val itemName = item.getDisplayName
        items += itemName -> (if (items.contains(itemName)) items(itemName) + 1 else 1)
      }
      if (items.nonEmpty) {
        tooltip.addAll(Tooltip.get("Server.Components"))
        for (itemName <- items.keys.toArray.sorted) {
          tooltip.add("- " + items(itemName) + "x " + itemName)
        }
      }
    }
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ActionResult[ItemStack] = {
    if (!player.isSneaking) {
      // Open the GUI immediately on the client, too, to avoid the player
      // changing the current slot before it actually opens, which can lead to
      // desynchronization of the player inventory.
      player.openGui(OpenComputers, GuiType.Server.id, world, 0, 0, 0)
      player.swingArm(EnumHand.MAIN_HAND)
    }
    ActionResult.newResult(EnumActionResult.SUCCESS, stack)
  }

}
