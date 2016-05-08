package li.cil.oc.client.gui

import java.text.DecimalFormat

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats

/* TODO NEI
import codechicken.nei.VisiblityData
import codechicken.nei.api.INEIGuiHandler
import codechicken.nei.api.TaggedInventoryArea
*/

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import net.minecraftforge.fml.common.Optional
import org.lwjgl.opengl.GL11
import org.lwjgl.util.Rectangle

@Optional.Interface(iface = "codechicken.nei.api.INEIGuiHandler", modid = Mods.IDs.NotEnoughItems)
class Relay(playerInventory: InventoryPlayer, val relay: tileentity.Relay) extends DynamicGuiContainer(new container.Relay(playerInventory, relay)) /* TODO NEI with INEIGuiHandler */ {
  private val format = new DecimalFormat("#.##hz")

  private val tabPosition = new Rectangle(xSize, 10, 23, 26)

  override protected def drawSecondaryBackgroundLayer(): Unit = {
    super.drawSecondaryBackgroundLayer()

    // Tab background.
    GlStateManager.color(1, 1, 1, 1)
    Minecraft.getMinecraft.getTextureManager.bindTexture(Textures.GUI.UpgradeTab)
    val x = windowX + tabPosition.getX
    val y = windowY + tabPosition.getY
    val w = tabPosition.getWidth
    val h = tabPosition.getHeight
    val t = Tessellator.getInstance
    val r = t.getBuffer
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
    r.pos(x, y + h, zLevel).tex(0, 1).endVertex()
    r.pos(x + w, y + h, zLevel).tex(1, 1).endVertex()
    r.pos(x + w, y, zLevel).tex(1, 0).endVertex()
    r.pos(x, y, zLevel).tex(0, 0).endVertex()
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

  override def mouseReleased(mouseX: Int, mouseY: Int, button: Int): Unit = {
    // So MC doesn't throw away the item in the upgrade slot when we're trying to pick it up...
    val originalWidth = xSize
    try {
      xSize += tabPosition.getWidth
      super.mouseReleased(mouseX, mouseY, button)
    }
    finally {
      xSize = originalWidth
    }
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(relay.getName),
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

  /* TODO NEI
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
  */
}
