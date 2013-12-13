package li.cil.oc.server.component

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.api
import li.cil.oc.api.network._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.{Container, InventoryCrafting}
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.tileentity.{TileEntity => MCTileEntity}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent
import scala.collection.mutable

class Crafting(val owner: MCTileEntity) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("crafting").
    create()

  @LuaCallback("craft")
  def craft(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val count = if (args.count > 0) args.checkInteger(0) else Int.MaxValue
    result(CraftingInventory.craft(context, count))
  }

  private object CraftingInventory extends InventoryCrafting(new Container {
    def canInteractWith(player: EntityPlayer) = true
  }, 3, 3) {
    var amountPossible = 0

    def craft(context: RobotContext, wantedCount: Int): Boolean = {
      load(context)
      val manager = CraftingManager.getInstance
      val result = manager.findMatchingRecipe(CraftingInventory, owner.getWorldObj)
      if (result == null) return false
      val targetStackSize = if (result.isStackable) math.min(wantedCount, result.getMaxStackSize) else result.stackSize
      val timesCrafted = math.min(targetStackSize / result.stackSize, amountPossible)
      if (timesCrafted <= 0) return true
      GameRegistry.onItemCrafted(context.player, result, this)
      val surplus = mutable.ArrayBuffer.empty[ItemStack]
      for (slot <- 0 until getSizeInventory) {
        val stack = getStackInSlot(slot)
        if (stack != null) {
          decrStackSize(slot, timesCrafted)
          val item = stack.getItem
          if (item.hasContainerItem) {
            val container = item.getContainerItemStack(stack)
            if (container.isItemStackDamageable && container.getItemDamage > container.getMaxDamage) {
              MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(context.player, container))
            }
            else if (container.getItem.doesContainerItemLeaveCraftingGrid(container) || getStackInSlot(slot) != null) {
              surplus += container
            }
            else {
              container.stackSize *= timesCrafted
              setInventorySlotContents(slot, container)
            }
          }
        }
      }
      save(context)
      result.stackSize *= timesCrafted
      val inventory = context.player.inventory
      inventory.addItemStackToInventory(result)
      for (stack <- surplus) {
        inventory.addItemStackToInventory(stack)
      }
      true
    }

    def load(context: RobotContext) {
      val inventory = context.player.inventory
      amountPossible = Int.MaxValue
      for (slot <- 0 until getSizeInventory) {
        val stack = inventory.getStackInSlot(toParentSlot(slot))
        setInventorySlotContents(slot, stack)
        if (stack != null) {
          amountPossible = math.min(amountPossible, stack.stackSize)
        }
      }
    }

    def save(context: RobotContext) {
      val inventory = context.player.inventory
      for (slot <- 0 until getSizeInventory) {
        inventory.setInventorySlotContents(toParentSlot(slot), getStackInSlot(slot))
      }
    }

    private def toParentSlot(slot: Int) = {
      val col = slot % 3
      val row = slot / 3
      row * 4 + col + 4 // first four are always: tool, card, disk, upgrade
    }
  }

}
