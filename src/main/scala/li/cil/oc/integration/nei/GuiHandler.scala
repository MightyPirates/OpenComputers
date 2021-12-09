package li.cil.oc.integration.nei

import codechicken.nei.api.INEIGuiAdapter
import li.cil.oc.client.PacketSender
import li.cil.oc.client.gui.Database
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack

class GuiHandler extends INEIGuiAdapter {
  private def getSlot(db: Database, mousex: Int, mousey: Int): Option[Int] = {
    val slotSize = 18
    val offset = 8 + Array(3, 2, 0)(db.databaseInventory.tier) * slotSize
    val row = (mousey - offset - db.guiTop)/slotSize
    val column = (mousex - offset - db.guiLeft)/slotSize
    val size = math.sqrt(db.databaseInventory.getSizeInventory).ceil.toInt
    val validSlots = 0 until size
    if ((validSlots contains row) && (validSlots contains column))
      Option(row * size + column)
    else
      None
  }
  override def handleDragNDrop(gui: GuiContainer, mousex: Int, mousey: Int, draggedStack: ItemStack, button: Int):Boolean = {
    gui match {
      case db: Database =>
        getSlot(db, mousex, mousey) match {
          case Some(slot) =>
            val stack = draggedStack.copy()
            stack.stackSize = 1 // packet writing will "optimize" out empty stack
            PacketSender.sendDatabaseSetSlot(slot, stack)
            true
          case _ => false
        }
      case _ => super.handleDragNDrop(gui, mousex, mousey, draggedStack, button)
    }
  }
}
