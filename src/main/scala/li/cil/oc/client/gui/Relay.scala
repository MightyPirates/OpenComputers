package li.cil.oc.client.gui

import java.lang.Iterable
import java.text.DecimalFormat
import java.util

import codechicken.nei.VisiblityData
import codechicken.nei.api.INEIGuiHandler
import codechicken.nei.api.TaggedInventoryArea
import cpw.mods.fml.common.Optional
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import org.lwjgl.util.Rectangle

@Optional.Interface(iface = "codechicken.nei.api.INEIGuiHandler", modid = Mods.IDs.NotEnoughItems)
class Relay(playerInventory: InventoryPlayer, val relay: tileentity.Relay) extends DynamicGuiContainer(new container.Relay(playerInventory, relay)) with INEIGuiHandler {
  private val format = new DecimalFormat("#.##hz")

  private val tabPosition = new Rectangle(xSize, 10, 23, 26)

  override protected def drawSecondaryBackgroundLayer(): Unit = {
    super.drawSecondaryBackgroundLayer()

    // Tab background.
    GL11.glColor4f(1, 1, 1, 1)
    Minecraft.getMinecraft.getTextureManager.bindTexture(Textures.guiUpgradeTab)
    val x = windowX + tabPosition.getX
    val y = windowY + tabPosition.getY
    val w = tabPosition.getWidth
    val h = tabPosition.getHeight
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y + h, zLevel, 0, 1)
    t.addVertexWithUV(x + w, y + h, zLevel, 1, 1)
    t.addVertexWithUV(x + w, y, zLevel, 1, 0)
    t.addVertexWithUV(x, y, zLevel, 0, 0)
    t.draw()
  }

  override def mouseClicked(mouseX: Int, mouseY: Int, button: Int): Unit = {
    // So MC doesn't throw away the item in the upgrade slot when we're trying to pick it up...
    val originalWidth = xSize
    try {
      xSize += tabPosition.getWidth
      super.mouseClicked(mouseX, mouseY, button)
    }
    finally {
      xSize = originalWidth
    }
  }

  override def mouseMovedOrUp(mouseX: Int, mouseY: Int, button: Int): Unit = {
    // So MC doesn't throw away the item in the upgrade slot when we're trying to pick it up...
    val originalWidth = xSize
    try {
      xSize += tabPosition.getWidth
      super.mouseMovedOrUp(mouseX, mouseY, button)
    }
    finally {
      xSize = originalWidth
    }
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(relay.getInventoryName),
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

  @Optional.Method(modid = Mods.IDs.NotEnoughItems)
  override def modifyVisiblity(gui: GuiContainer, currentVisibility: VisiblityData): VisiblityData = null

  @Optional.Method(modid = Mods.IDs.NotEnoughItems)
  override def getItemSpawnSlots(gui: GuiContainer, stack: ItemStack): Iterable[Integer] = null

  @Optional.Method(modid = Mods.IDs.NotEnoughItems)
  override def getInventoryAreas(gui: GuiContainer): util.List[TaggedInventoryArea] = null

  @Optional.Method(modid = Mods.IDs.NotEnoughItems)
  override def handleDragNDrop(gui: GuiContainer, mouseX: Int, mouseY: Int, stack: ItemStack, button: Int): Boolean = false

  @Optional.Method(modid = Mods.IDs.NotEnoughItems)
  override def hideItemPanelSlot(gui: GuiContainer, x: Int, y: Int, w: Int, h: Int): Boolean = {
    new Rectangle(x - windowX, y - windowY, w, h).intersects(tabPosition)
  }
}
