package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.container.Player
import li.cil.oc.integration.Mods
import li.cil.oc.integration.jei.ModJEI
import li.cil.oc.integration.util.ItemSearch
import li.cil.oc.util.RenderState
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.Optional
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

abstract class DynamicGuiContainer[C <: Container](container: C) extends CustomGuiContainer(container) {
  protected var hoveredSlot: Option[Slot] = None

  protected var hoveredStackNEI: StackOption = EmptyStack

  protected def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) {
    fontRenderer.drawString(
      Localization.localizeImmediately("container.inventory"),
      8, ySize - 96 + 2, 0x404040)
  }

  override protected def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    RenderState.pushAttrib()

    drawSecondaryForegroundLayer(mouseX, mouseY)

    for (slot <- 0 until inventorySlots.inventorySlots.size()) {
      drawSlotHighlight(inventorySlots.inventorySlots.get(slot))
    }

    RenderState.popAttrib()
  }

  protected def drawSecondaryBackgroundLayer() {}

  override protected def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GlStateManager.color(1, 1, 1, 1)
    Textures.bind(Textures.GUI.Background)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    drawSecondaryBackgroundLayer()

    RenderState.makeItBlend()
    GlStateManager.disableLighting()

    drawInventorySlots()
  }

  protected def drawInventorySlots(): Unit = {
    GlStateManager.pushMatrix()
    GlStateManager.translate(guiLeft, guiTop, 0)
    GlStateManager.disableDepth()
    for (slot <- 0 until inventorySlots.inventorySlots.size()) {
      drawSlotInventory(inventorySlots.inventorySlots.get(slot))
    }
    GlStateManager.enableDepth()
    GlStateManager.popMatrix()
    RenderState.makeItBlend()
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    hoveredSlot = (inventorySlots.inventorySlots collect {
      case slot: Slot if isPointInRegion(slot.xPos, slot.yPos, 16, 16, mouseX, mouseY) => slot
    }).headOption
    hoveredStackNEI = ItemSearch.hoveredStack(this, mouseX, mouseY)

    super.drawScreen(mouseX, mouseY, dt)

    if (Mods.JustEnoughItems.isModAvailable) {
      drawJEIHighlights()
    }
  }

  protected def drawSlotInventory(slot: Slot) {
    GlStateManager.enableBlend()
    slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None =>
        if (!slot.getHasStack && slot.xPos >= 0 && slot.yPos >= 0 && component.tierIcon != null) {
          drawDisabledSlot(component)
        }
      case _ =>
        zLevel += 1
        if (!isInPlayerInventory(slot)) {
          drawSlotBackground(slot.xPos - 1, slot.yPos - 1)
        }
        if (!slot.getHasStack) {
          slot match {
            case component: ComponentSlot =>
              if (component.tierIcon != null) {
                Textures.bind(component.tierIcon)
                Gui.drawModalRectWithCustomSizedTexture(slot.xPos, slot.yPos, 0, 0, 16, 16, 16, 16)
              }
              if (component.hasBackground) {
                Textures.bind(slot.getBackgroundLocation)
                Gui.drawModalRectWithCustomSizedTexture(slot.xPos, slot.yPos, 0, 0, 16, 16, 16, 16)
              }
            case _ =>
          }
          zLevel -= 1
        }
    }
    GlStateManager.disableBlend()
  }

  protected def drawSlotHighlight(slot: Slot) {
    if (mc.player.inventory.getItemStack.isEmpty) slot match {
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
            case SomeStack(stack) => !currentIsInPlayerInventory && isSelectiveSlot(slot) && slot.isItemValid(stack)
            case _ => false
          }
        }
        if (drawHighlight) {
          zLevel += 100
          drawGradientRect(
            slot.xPos, slot.yPos,
            slot.xPos + 16, slot.yPos + 16,
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
    GlStateManager.color(1, 1, 1, 1)
    Textures.bind(slot.tierIcon)
    Gui.drawModalRectWithCustomSizedTexture(slot.xPos, slot.yPos, 0, 0, 16, 16, 16, 16)
  }

  protected def drawSlotBackground(x: Int, y: Int) {
    GlStateManager.color(1, 1, 1, 1)
    Textures.bind(Textures.GUI.Slot)
    val t = Tessellator.getInstance
    val r = t.getBuffer
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
    r.pos(x, y + 18, zLevel + 1).tex(0, 1).endVertex()
    r.pos(x + 18, y + 18, zLevel + 1).tex(1, 1).endVertex()
    r.pos(x + 18, y, zLevel + 1).tex(1, 0).endVertex()
    r.pos(x, y, zLevel + 1).tex(0, 0).endVertex()
    t.draw()
  }

  private def isInPlayerInventory(slot: Slot) = container match {
    case player: Player => slot.inventory == player.playerInventory
    case _ => false
  }

  override def onGuiClosed(): Unit = {
    super.onGuiClosed()
    if(Mods.JustEnoughItems.isModAvailable) {
      resetJEIHighlights()
    }
  }

  @Optional.Method(modid = Mods.IDs.JustEnoughItems)
  private def drawJEIHighlights(): Unit = {
    ModJEI.runtime.foreach { runtime =>
      val overlay = runtime.getItemListOverlay
      hoveredSlot match {
        case Some(hovered) if !isInPlayerInventory(hovered) && isSelectiveSlot(hovered) =>
          overlay.highlightStacks(overlay.getVisibleStacks.filter(hovered.isItemValid))
        case _ => overlay.highlightStacks(List[Nothing]())
      }
    }
  }

  @Optional.Method(modid = Mods.IDs.JustEnoughItems)
  private def resetJEIHighlights() = ModJEI.runtime.foreach(_.getItemListOverlay.highlightStacks(List[Nothing]()))
}
