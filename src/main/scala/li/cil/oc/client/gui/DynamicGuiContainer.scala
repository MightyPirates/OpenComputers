package li.cil.oc.client.gui

import codechicken.nei.ItemPanel
import codechicken.nei.LayoutManager
import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.ReflectionHelper
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.container.Player
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.NEI
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsScala._

abstract class DynamicGuiContainer[C <: Container](container: C) extends CustomGuiContainer(container) {
  protected var hoveredSlot: Option[Slot] = None

  protected var hoveredStackNEI: Option[ItemStack] = None

  protected def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) {
    fontRendererObj.drawString(
      Localization.localizeImmediately("container.inventory"),
      8, ySize - 96 + 2, 0x404040)
  }

  override protected def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    drawSecondaryForegroundLayer(mouseX, mouseY)

    for (slot <- 0 until inventorySlots.inventorySlots.size()) {
      drawSlotHighlight(inventorySlots.inventorySlots.get(slot).asInstanceOf[Slot])
    }

    GL11.glPopAttrib()
  }

  protected def drawSecondaryBackgroundLayer() {}

  override protected def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor4f(1, 1, 1, 1)
    mc.renderEngine.bindTexture(Textures.guiBackground)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    drawSecondaryBackgroundLayer()

    RenderState.makeItBlend()
    GL11.glDisable(GL11.GL_LIGHTING)

    drawInventorySlots()
  }

  protected def drawInventorySlots(): Unit = {
    GL11.glPushMatrix()
    GL11.glTranslatef(guiLeft, guiTop, 0)
    for (slot <- 0 until inventorySlots.inventorySlots.size()) {
      drawSlotInventory(inventorySlots.inventorySlots.get(slot).asInstanceOf[Slot])
    }
    GL11.glPopMatrix()
    RenderState.makeItBlend()
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    hoveredSlot = (inventorySlots.inventorySlots collect {
      case slot: Slot if isPointInRegion(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY) => slot
    }).headOption
    hoveredStackNEI = NEI.hoveredStack(this, mouseX, mouseY)

    super.drawScreen(mouseX, mouseY, dt)

    if (Mods.NotEnoughItems.isAvailable) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
      drawNEIHighlights()
      GL11.glPopAttrib()
    }
  }

  protected def drawSlotInventory(slot: Slot) {
    slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None =>
        if (!slot.getHasStack && slot.xDisplayPosition >= 0 && slot.yDisplayPosition >= 0 && component.tierIcon != null) {
          drawDisabledSlot(component)
        }
      case _ =>
        zLevel += 1
        if (!isInPlayerInventory(slot)) {
          drawSlotBackground(slot.xDisplayPosition - 1, slot.yDisplayPosition - 1)
        }
        if (!slot.getHasStack) slot match {
          case component: ComponentSlot if component.tierIcon != null =>
            mc.getTextureManager.bindTexture(TextureMap.locationItemsTexture)
            drawTexturedModelRectFromIcon(slot.xDisplayPosition, slot.yDisplayPosition, component.tierIcon, 16, 16)
          case _ =>
        }
        zLevel -= 1
    }
  }

  protected def drawSlotHighlight(slot: Slot) {
    if (mc.thePlayer.inventory.getItemStack == null) slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None => // Ignore.
      case _ =>
        val currentIsInPlayerInventory = isInPlayerInventory(slot)
        val drawHighlight = hoveredSlot match {
          case Some(hovered) =>
            val hoveredIsInPlayerInventory = isInPlayerInventory(hovered)
            (currentIsInPlayerInventory != hoveredIsInPlayerInventory) &&
              ((currentIsInPlayerInventory && slot.getHasStack && isSelectiveSlot(hovered) && hovered.isItemValid(slot.getStack)) ||
                (hoveredIsInPlayerInventory && hovered.getHasStack && isSelectiveSlot(slot) && slot.isItemValid(hovered.getStack)))
          case _ => hoveredStackNEI match {
            case Some(stack) => !currentIsInPlayerInventory && isSelectiveSlot(slot) && slot.isItemValid(stack)
            case _ => false
          }
        }
        if (drawHighlight) {
          zLevel += 100
          drawGradientRect(
            slot.xDisplayPosition, slot.yDisplayPosition,
            slot.xDisplayPosition + 16, slot.yDisplayPosition + 16,
            0x80FFFFFF, 0x80FFFFFF)
          zLevel -= 100
        }
    }
  }

  private def isSelectiveSlot(slot: Slot) = slot match {
    case component: ComponentSlot => component.slot != common.Slot.Any && component.slot != common.Slot.Tool
    case _ => false
  }

  protected def drawDisabledSlot(slot: ComponentSlot) {
    GL11.glColor4f(1, 1, 1, 1)
    mc.getTextureManager.bindTexture(TextureMap.locationItemsTexture)
    drawTexturedModelRectFromIcon(slot.xDisplayPosition, slot.yDisplayPosition, slot.tierIcon, 16, 16)
  }

  protected def drawSlotBackground(x: Int, y: Int) {
    GL11.glColor4f(1, 1, 1, 1)
    mc.getTextureManager.bindTexture(Textures.guiSlot)
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y + 18, zLevel, 0, 1)
    t.addVertexWithUV(x + 18, y + 18, zLevel, 1, 1)
    t.addVertexWithUV(x + 18, y, zLevel, 1, 0)
    t.addVertexWithUV(x, y, zLevel, 0, 0)
    t.draw()
  }

  protected override def drawGradientRect(par1: Int, par2: Int, par3: Int, par4: Int, par5: Int, par6: Int) {
    super.drawGradientRect(par1, par2, par3, par4, par5, par6)
    RenderState.makeItBlend()
    GL11.glDisable(GL11.GL_LIGHTING)
  }

  override def drawTexturedModelRectFromIcon(x: Int, y: Int, icon: IIcon, width: Int, height: Int) {
    GL11.glColor4f(1, 1, 1, 1)
    RenderState.makeItBlend()
    GL11.glDisable(GL11.GL_LIGHTING)
    super.drawTexturedModelRectFromIcon(x, y, icon, width, height)
  }

  private def isPointInRegion(rx: Int, ry: Int, rw: Int, rh: Int, px: Int, py: Int) = func_146978_c(rx, ry, rw, rh, px, py)

  private def isInPlayerInventory(slot: Slot) = container match {
    case player: Player => slot.inventory == player.playerInventory
    case _ => false
  }

  @Optional.Method(modid = Mods.IDs.NotEnoughItems)
  private def drawNEIHighlights(): Unit = {
    val panel = LayoutManager.itemPanel
    if (panel == null) return
    zLevel += 350
    val itemsPerPage = panel.getGrid.getPerPage
    for (index <- 0 until itemsPerPage) {
      val rect = panel.getGrid.getSlotRect(index)
      val slot = panel.getSlotMouseOver(rect.x, rect.y)
      if (slot != null) hoveredSlot match {
        case Some(hovered) =>
          if (!isInPlayerInventory(hovered) && isSelectiveSlot(hovered) && hovered.isItemValid(slot.item)) {
            drawGradientRect(
              rect.x1 + 1, rect.y1 + 1,
              rect.x2, rect.y2,
              0x40FFFFFF, 0x40FFFFFF)
          }
        case _ =>
      }
    }
    zLevel -= 350
  }
}
