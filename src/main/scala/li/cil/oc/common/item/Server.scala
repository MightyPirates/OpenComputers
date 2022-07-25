package li.cil.oc.common.item

import java.util

import li.cil.oc.OpenComputers
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.GuiType
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item // Rarity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World

import scala.collection.mutable
import scala.collection.convert.ImplicitConversionsToScala._

class Server(val parent: Delegator, val tier: Int) extends traits.Delegate {
  override val unlocalizedName: String = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override def rarity(stack: ItemStack): item.Rarity = Rarity.byTier(tier)

  override def maxStackSize = 1

  private object HelperInventory extends ServerInventory {
    var container = ItemStack.EMPTY
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[ITextComponent]) {
    super.tooltipExtended(stack, tooltip)
    if (KeyBindings.showExtendedTooltips) {
      HelperInventory.container = stack
      HelperInventory.reinitialize()
      val stacks = mutable.Map.empty[String, Int]
      for (aStack <- (0 until HelperInventory.getContainerSize).map(HelperInventory.getItem) if !aStack.isEmpty) {
        val displayName = aStack.getDisplayName.getString
        stacks += displayName -> (if (stacks.contains(displayName)) stacks(displayName) + 1 else 1)
      }
      if (stacks.nonEmpty) {
        for (curr <- Tooltip.get("server.Components")) {
          tooltip.add(new StringTextComponent(curr))
        }
        for (itemName <- stacks.keys.toArray.sorted) {
          tooltip.add(new StringTextComponent("- " + stacks(itemName) + "x " + itemName))
        }
      }
    }
  }

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (!player.isCrouching) {
      // Open the GUI immediately on the client, too, to avoid the player
      // changing the current slot before it actually opens, which can lead to
      // desynchronization of the player inventory.
      OpenComputers.openGui(player, GuiType.Server.id, world, 0, 0, 0)
      player.swing(Hand.MAIN_HAND)
    }
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }

}
