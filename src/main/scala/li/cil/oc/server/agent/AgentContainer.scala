package li.cil.oc.server.agent

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.{Container, IContainerListener, IInventory, Slot}
import li.cil.oc.server.agent
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList

class AgentContainer(player: agent.Player) extends Container {
  {
    for (slot <- 0 until player.agent.equipmentInventory.getSizeInventory) {
      this.addSlotToContainer(new Slot(player.inventory, -1 - slot, 0, 0))
    }
    for (slot <- 0 until player.agent.mainInventory.getSizeInventory) {
      this.addSlotToContainer(new Slot(player.inventory, slot, 0, 0))
    }

    this.addListener(new IContainerListener {
      override def sendAllContents(containerToSend: Container, itemsList: NonNullList[ItemStack]): Unit = {}

      override def sendWindowProperty(containerIn: Container, varToUpdate: Int, newValue: Int): Unit = {}

      override def sendAllWindowProperties(containerIn: Container, inventory: IInventory): Unit = {}

      override def sendSlotContents(containerToSend: Container, index: Int, stack: ItemStack): Unit = {
        // an action has updated the agent.inventory via slots
        // thus the player.inventory is outdated in this regard
        val relativeIndex: Int = containerToSend.inventorySlots.get(index).getSlotIndex

        if (relativeIndex < 0) {
          if (~relativeIndex < player.inventory.armorInventory.size) {
            player.inventory.armorInventory.set(~relativeIndex, stack)
          }
        }
        else if (relativeIndex < player.inventory.mainInventory.size) {
          player.inventory.mainInventory.set(relativeIndex, stack)
        }
      }
    })
  }

  override def canInteractWith(player: EntityPlayer): Boolean = true
}
