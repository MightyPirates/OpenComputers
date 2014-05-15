package li.cil.oc.common.container

import cpw.mods.fml.relauncher.{SideOnly, Side}
import cpw.mods.fml.common.FMLCommonHandler
import li.cil.oc.common.{InventorySlots, tileentity}
import li.cil.oc.util.ItemUtils
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.{ICrafting, Slot}
import scala.collection.convert.WrapAsScala._
import li.cil.oc.api
import li.cil.oc.common.InventorySlots.Tier
import li.cil.oc.client.gui.Icons

class RobotAssembler(playerInventory: InventoryPlayer, assembler: tileentity.RobotAssembler) extends Player(playerInventory, assembler) {
  // Computer case.
  {
    val index = inventorySlots.size
    addSlotToContainer(new StaticComponentSlot(this, otherInventory, index, 12, 12, api.driver.Slot.None, Tier.Any) {
      @SideOnly(Side.CLIENT) override
      def func_111238_b() = !isAssembling && super.func_111238_b()

      override def getBackgroundIconIndex = if (isAssembling) Icons.get(Tier.None) else super.getBackgroundIconIndex
    })
  }

  def caseTier = ItemUtils.caseTier(inventorySlots.get(0).asInstanceOf[Slot].getStack)

  // Component containers.
  for (i <- 0 until 3) {
    addSlotToContainer(34 + i * slotSize, 70, InventorySlots.assembler, () => caseTier)
  }

  // Components.
  for (i <- 0 until 9) {
    addSlotToContainer(34 + (i % 3) * slotSize, 12 + (i / 3) * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // Cards.
  for (i <- 0 until 3) {
    addSlotToContainer(104, 12 + i * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // CPU.
  addSlotToContainer(126, 12, InventorySlots.assembler, () => caseTier)

  // RAM.
  for (i <- 0 until 2) {
    addSlotToContainer(126, 30 + i * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // Floppy + HDDs.
  for (i <- 0 until 3) {
    addSlotToContainer(148, 12 + i * slotSize, InventorySlots.assembler, () => caseTier)
  }

  // Show the player's inventory.
  addPlayerInventorySlots(8, 110)

  var isAssembling = false
  var assemblyProgress = 0

  @SideOnly(Side.CLIENT)
  override def updateProgressBar(id: Int, value: Int) {
    super.updateProgressBar(id, value)
    if (id == 0) {
      isAssembling = value == 1
    }

    if (id == 1) {
      assemblyProgress = value
    }
  }

  override def detectAndSendChanges() {
    super.detectAndSendChanges()
    if (FMLCommonHandler.instance.getEffectiveSide.isServer) {
      if (isAssembling != assembler.isAssembling) {
        isAssembling = assembler.isAssembling
        sendProgressBarUpdate(0, if (isAssembling) 1 else 0)
      }
      if (assemblyProgress != assembler.progress) {
        assemblyProgress = assembler.progress
        sendProgressBarUpdate(1, assemblyProgress)
      }
    }
  }

  private def sendProgressBarUpdate(id: Int, value: Int) {
    for (entry <- crafters) entry match {
      case player: ICrafting => player.sendProgressBarUpdate(this, id, value)
      case _ =>
    }
  }
}