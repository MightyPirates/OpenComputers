package li.cil.oc.common.item

import java.util

import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.{GuiType, Tier}
import li.cil.oc.util.{Rarity, Tooltip}
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

import scala.collection.mutable

class Server(val parent: Delegator, val tier: Int) extends Delegate {
  override val unlocalizedName = super.unlocalizedName + tier

  override protected def tooltipName = Option(super.unlocalizedName)

  override protected def tooltipData = Seq(Settings.get.terminalsPerTier(math.min(Tier.Three, tier)))

  override def rarity = Rarity.byTier(tier)

  override def maxStackSize = 1

  private object HelperInventory extends ServerInventory {
    def tier = Server.this.tier

    var container: ItemStack = null
  }

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]) {
    super.tooltipExtended(stack, tooltip)
    HelperInventory.container = stack
    HelperInventory.reinitialize()
    val items = mutable.Map.empty[String, Int]
    for (item <- (0 until HelperInventory.getSizeInventory).map(HelperInventory.getStackInSlot) if item != null) {
      val itemName = item.getDisplayName
      items += itemName -> (if (items.contains(itemName)) items(itemName) + 1 else 1)
    }
    if (items.size > 0) {
      tooltip.addAll(Tooltip.get("Server.Components"))
      for (itemName <- items.keys.toArray.sorted) {
        tooltip.add("- " + items(itemName) + "x " + itemName)
      }
    }
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    if (!player.isSneaking) {
      // Open the GUI immediately on the client, too, to avoid the player
      // changing the current slot before it actually opens, which can lead to
      // desynchronization of the player inventory.
      player.openGui(OpenComputers, GuiType.Server.id, world, 0, 0, 0)
      player.swingItem()
    }
    stack
  }

}
