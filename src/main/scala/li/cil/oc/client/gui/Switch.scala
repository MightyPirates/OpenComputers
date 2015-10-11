package li.cil.oc.client.gui

import java.text.DecimalFormat

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.InventoryPlayer

// TODO Remove in 1.7
class Switch(playerInventory: InventoryPlayer, val switch: tileentity.Switch) extends DynamicGuiContainer(new container.Switch(playerInventory, switch)) {
  private val format = new DecimalFormat("#.##hz")

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(switch.getInventoryName),
      8, 6, 0x404040)

    fontRendererObj.drawString(
      Localization.Switch.TransferRate,
      14, 20, 0x404040)
    fontRendererObj.drawString(
      Localization.Switch.PacketsPerCycle,
      14, 39, 0x404040)
    fontRendererObj.drawString(
      Localization.Switch.QueueSize,
      14, 58, 0x404040)

    fontRendererObj.drawString(
      format.format(20f / inventoryContainer.relayDelay),
      108, 20, 0x404040)
    fontRendererObj.drawString(
      inventoryContainer.packetsPerCycleAvg + " / " + inventoryContainer.relayAmount,
      108, 39, thresholdBasedColor(inventoryContainer.packetsPerCycleAvg, math.ceil(inventoryContainer.relayAmount / 2f).toInt, inventoryContainer.relayAmount))
    fontRendererObj.drawString(
      inventoryContainer.queueSize + " / " + inventoryContainer.maxQueueSize,
      108, 58, thresholdBasedColor(inventoryContainer.queueSize, inventoryContainer.maxQueueSize / 2, inventoryContainer.maxQueueSize))
  }

  private def thresholdBasedColor(value: Int, yellow: Int, red: Int) = {
    if (value < yellow) 0x009900
    else if (value < red) 0x999900
    else 0x990000
  }
}
