package li.cil.oc.server.component

import cpw.mods.fml.common.FMLCommonHandler
import li.cil.oc.api.Network
import li.cil.oc.api.driver.Host
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network._
import li.cil.oc.api.tileentity.Robot
import li.cil.oc.common.component
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent

import scala.collection.mutable

class UpgradeCrafting(val host: Host with Robot) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("crafting").
    create()

  @Callback(doc = """function([count:number]):number -- Tries to craft the specified number of items in the top left area of the inventory.""")
  def craft(context: Context, args: Arguments): Array[AnyRef] = {
    val count = if (args.count > 0) args.checkInteger(0) else Int.MaxValue
    result(CraftingInventory.craft(count): _*)
  }

  private object CraftingInventory extends inventory.InventoryCrafting(new inventory.Container {
    override def canInteractWith(player: EntityPlayer) = true
  }, 3, 3) {
    var amountPossible = 0

    def craft(wantedCount: Int): Seq[_] = {
      load()
      val manager = CraftingManager.getInstance
      val result = manager.findMatchingRecipe(CraftingInventory, host.world)
      if (result == null) return Seq(false, 0)
      val targetStackSize = if (result.isStackable) math.min(wantedCount, result.getMaxStackSize) else result.stackSize
      val timesCrafted = math.min(targetStackSize / result.stackSize, amountPossible)
      if (timesCrafted <= 0) return Seq(true, 0)
      FMLCommonHandler.instance.firePlayerCraftingEvent(host.player, result, this)
      val surplus = mutable.ArrayBuffer.empty[ItemStack]
      for (slot <- 0 until getSizeInventory) {
        val stack = getStackInSlot(slot)
        if (stack != null) {
          decrStackSize(slot, timesCrafted)
          val item = stack.getItem
          if (item.hasContainerItem(stack)) {
            val container = item.getContainerItem(stack)
            if (container.isItemStackDamageable && container.getItemDamage > container.getMaxDamage) {
              MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(host.player, container))
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
      save()
      result.stackSize *= timesCrafted
      val countCrafted = result.stackSize
      val inventory = host.player.inventory
      inventory.addItemStackToInventory(result)
      for (stack <- surplus) {
        inventory.addItemStackToInventory(stack)
      }
      Seq(true, countCrafted)
    }

    def load() {
      val inventory = host.player.inventory
      amountPossible = Int.MaxValue
      for (slot <- 0 until getSizeInventory) {
        val stack = inventory.getStackInSlot(toParentSlot(slot))
        setInventorySlotContents(slot, stack)
        if (stack != null) {
          amountPossible = math.min(amountPossible, stack.stackSize)
        }
      }
    }

    def save() {
      val inventory = host.player.inventory
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
