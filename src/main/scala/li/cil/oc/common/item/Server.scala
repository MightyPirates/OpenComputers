package li.cil.oc.common.item

import java.util

import li.cil.oc.OpenComputers
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.item // Rarity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World
import net.minecraftforge.common.extensions.IForgeItem

import scala.collection.mutable
import scala.collection.convert.ImplicitConversionsToScala._

class Server(props: Properties, val tier: Int) extends Item(props) with IForgeItem with traits.SimpleItem {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  override protected def tooltipName = Option(unlocalizedName)

  @Deprecated
  override def getRarity(stack: ItemStack): item.Rarity = Rarity.byTier(tier)

  override def maxStackSize = 1

  private object HelperInventory extends ServerInventory {
    var container = ItemStack.EMPTY

    override def rackSlot = -1
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[ITextComponent]) {
    super.tooltipExtended(stack, tooltip)
    if (KeyBindings.showExtendedTooltips) {
      HelperInventory.container = stack
      HelperInventory.reinitialize()
      val stacks = mutable.Map.empty[String, Int]
      for (aStack <- (0 until HelperInventory.getContainerSize).map(HelperInventory.getItem) if !aStack.isEmpty) {
        val displayName = aStack.getHoverName.getString
        stacks += displayName -> (if (stacks.contains(displayName)) stacks(displayName) + 1 else 1)
      }
      if (stacks.nonEmpty) {
        for (curr <- Tooltip.get("server.Components")) {
          tooltip.add(new StringTextComponent(curr).setStyle(Tooltip.DefaultStyle))
        }
        for (itemName <- stacks.keys.toArray.sorted) {
          tooltip.add(new StringTextComponent("- " + stacks(itemName) + "x " + itemName).setStyle(Tooltip.DefaultStyle))
        }
      }
    }
  }

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (!player.isCrouching) {
      if (!world.isClientSide) player match {
        case srvPlr: ServerPlayerEntity => ContainerTypes.openServerGui(srvPlr, new ServerInventory {
            override def container = stack

            override def rackSlot = -1

            override def stillValid(player: PlayerEntity) = player == srvPlr
          }, -1)
        case _ =>
      }
      player.swing(Hand.MAIN_HAND)
    }
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }

}
