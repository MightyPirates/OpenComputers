package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.container.Player
import li.cil.oc.integration.util.NEI
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsScala._

abstract class DynamicGuiContainer(container: Container) extends CustomGuiContainer(container) {
  protected var hoveredSlot: Option[Slot] = None

  protected var hoveredStackNEI: Option[ItemStack] = None

  override protected def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    fontRendererObj.drawString(
      Localization.localizeImmediately("container.inventory"),
      8, ySize - 96 + 2, 0x404040)
  }

  protected def drawSecondaryBackgroundLayer() {}

  override protected def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor4f(1, 1, 1, 1)
    mc.renderEngine.bindTexture(Textures.GUI.Background)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    drawSecondaryBackgroundLayer()

    RenderState.makeItBlend()
    GL11.glDisable(GL11.GL_LIGHTING)

    GL11.glPushMatrix()
    GL11.glTranslatef(guiLeft, guiTop, 0)
    for (slot <- 0 until inventorySlots.inventorySlots.size()) {
      drawSlotInventory(inventorySlots.inventorySlots.get(slot).asInstanceOf[Slot])
    }
    GL11.glPopMatrix()
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    hoveredSlot = (inventorySlots.inventorySlots collect {
      case slot: Slot if isPointInRegion(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY) => slot
    }).headOption
    hoveredStackNEI = NEI.hoveredStack(this, mouseX, mouseY)

    super.drawScreen(mouseX, mouseY, dt)

    GL11.glPushMatrix()
    GL11.glTranslatef(guiLeft, guiTop, 0)
    for (slot <- 0 until inventorySlots.inventorySlots.size()) {
      drawSlotHighlight(inventorySlots.inventorySlots.get(slot).asInstanceOf[Slot])
    }
    GL11.glPopMatrix()
  }

  protected def drawSlotInventory(slot: Slot) {
    slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None =>
        if (!slot.getHasStack && slot.xDisplayPosition >= 0 && slot.yDisplayPosition >= 0 && component.tierIcon != null) {
          drawDisabledSlot(component)
        }
      case _ =>
        if (!isInPlayerInventory(slot)) {
          drawSlotBackground(slot.xDisplayPosition - 1, slot.yDisplayPosition - 1)
        }
        if (!slot.getHasStack) {
          slot match {
            case component: ComponentSlot =>
              GL11.glDisable(GL11.GL_DEPTH_TEST)
              if (component.tierIcon != null) {
                mc.getTextureManager.bindTexture(component.tierIcon)
                Gui.drawModalRectWithCustomSizedTexture(slot.xDisplayPosition, slot.yDisplayPosition, 0, 0, 16, 16, 16, 16)
              }
              mc.getTextureManager.bindTexture(slot.getBackgroundLocation)
              Gui.drawModalRectWithCustomSizedTexture(slot.xDisplayPosition, slot.yDisplayPosition, 0, 0, 16, 16, 16, 16)
              GL11.glEnable(GL11.GL_DEPTH_TEST)
            case _ =>
          }
        }
    }
  }

  protected def drawSlotHighlight(slot: Slot) {
    slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None => // Ignore.
      case _ =>
        if (mc.thePlayer.inventory.getItemStack == null) {
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
            GlStateManager.disableLighting()
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            drawGradientRect(slot.xDisplayPosition, slot.yDisplayPosition, slot.xDisplayPosition + 16, slot.yDisplayPosition + 16, 0x80FFFFFF, 0x80FFFFFF)
            GlStateManager.enableLighting()
            GL11.glEnable(GL11.GL_DEPTH_TEST)
          }
        }
    }
  }

  private def isSelectiveSlot(slot: Slot) = slot match {
    case component: ComponentSlot => component.slot != common.Slot.Any && component.slot != common.Slot.Tool
    case _ => false
  }

  protected def drawDisabledSlot(slot: ComponentSlot) {
    GL11.glColor4f(1, 1, 1, 1)
    mc.getTextureManager.bindTexture(slot.tierIcon)
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    GL11.glDisable(GL11.GL_LIGHTING)
    Gui.drawModalRectWithCustomSizedTexture(slot.xDisplayPosition, slot.yDisplayPosition, 0, 0, 16, 16, 16, 16)
    GL11.glEnable(GL11.GL_LIGHTING)
    GL11.glEnable(GL11.GL_DEPTH_TEST)
  }

  protected def drawSlotBackground(x: Int, y: Int) {
    GL11.glColor4f(1, 1, 1, 1)
    mc.getTextureManager.bindTexture(Textures.GUI.Slot)
    val t = Tessellator.getInstance
    val r = t.getWorldRenderer
    r.startDrawingQuads()
    r.addVertexWithUV(x, y + 18, zLevel + 1, 0, 1)
    r.addVertexWithUV(x + 18, y + 18, zLevel + 1, 1, 1)
    r.addVertexWithUV(x + 18, y, zLevel + 1, 1, 0)
    r.addVertexWithUV(x, y, zLevel + 1, 0, 0)
    t.draw()
  }

  protected override def drawGradientRect(par1: Int, par2: Int, par3: Int, par4: Int, par5: Int, par6: Int) {
    super.drawGradientRect(par1, par2, par3, par4, par5, par6)
    RenderState.makeItBlend()
    GL11.glDisable(GL11.GL_LIGHTING)
  }

  private def isInPlayerInventory(slot: Slot) = container match {
    case player: Player => slot.inventory == player.playerInventory
    case _ => false
  }
}
