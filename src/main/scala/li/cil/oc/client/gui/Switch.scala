package li.cil.oc.client.gui

import java.text.DecimalFormat

import li.cil.oc.Localization
import li.cil.oc.common.{container, tileentity}
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector

class Switch(playerInventory: InventoryPlayer, val switch: tileentity.Switch) extends DynamicGuiContainer(new container.Switch(playerInventory, switch)) {
  private val switchContainer = inventorySlots.asInstanceOf[container.Switch]
  private val format = new DecimalFormat("#.##hz")

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      StatCollector.translateToLocal(switch.getInventoryName),
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
      format.format(20f / switchContainer.relayDelay),
      108, 20, 0x404040)
    fontRendererObj.drawString(
      switchContainer.packetsPerCycleAvg + " / " + switchContainer.relayAmount,
      108, 39, thresholdBasedColor(switchContainer.packetsPerCycleAvg, math.ceil(switchContainer.relayAmount / 2f).toInt, switchContainer.relayAmount))
    fontRendererObj.drawString(
      switchContainer.queueSize + " / " + switchContainer.maxQueueSize,
      108, 58, thresholdBasedColor(switchContainer.queueSize, switchContainer.maxQueueSize / 2, switchContainer.maxQueueSize))
  }

  private def thresholdBasedColor(value: Int, yellow: Int, red: Int) = {
    if (value < yellow) 0x009900
    else if (value < red) 0x999900
    else 0x990000
  }
}
