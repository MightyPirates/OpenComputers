package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
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
import net.minecraft.client.gui.AbstractGui
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.container.Container
import net.minecraft.inventory.container.Slot
import net.minecraft.util.text.ITextComponent
import org.lwjgl.opengl.GL11

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

abstract class DynamicGuiContainer[C <: Container](container: C, inv: PlayerInventory, title: ITextComponent)
  extends CustomGuiContainer(container, inv, title) {

  protected var hoveredStackNEI: StackOption = EmptyStack

  override protected def init() {
    super.init()
    // imageHeight is set in the body of the extending class, so it's not available in ours.
    inventoryLabelY = imageHeight - 96 + 2
  }

  protected def drawSecondaryForegroundLayer(stack: MatrixStack, mouseX: Int, mouseY: Int) {}

  override protected def renderLabels(stack: MatrixStack, mouseX: Int, mouseY: Int) {
    super.renderLabels(stack, mouseX, mouseY)
    RenderState.pushAttrib()

    drawSecondaryForegroundLayer(stack, mouseX, mouseY)

    for (slot <- 0 until menu.slots.size()) {
      drawSlotHighlight(stack, menu.getSlot(slot))
    }

    RenderState.popAttrib()
  }

  protected def drawSecondaryBackgroundLayer(stack: MatrixStack) {}

  override protected def renderBg(stack: MatrixStack, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.color4f(1, 1, 1, 1)
    Textures.bind(Textures.GUI.Background)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    drawSecondaryBackgroundLayer(stack)

    RenderState.makeItBlend()
    RenderSystem.disableLighting()

    drawInventorySlots(stack)
  }

  protected def drawInventorySlots(stack: MatrixStack): Unit = {
    stack.pushPose()
    stack.translate(leftPos, topPos, 0)
    RenderSystem.disableDepthTest()
    for (slot <- 0 until menu.slots.size()) {
      drawSlotInventory(stack, menu.getSlot(slot))
    }
    RenderSystem.enableDepthTest()
    stack.popPose()
    RenderState.makeItBlend()
  }

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, dt: Float) {
    hoveredStackNEI = ItemSearch.hoveredStack(this, mouseX, mouseY)

    super.render(stack, mouseX, mouseY, dt)
  }

  protected def drawSlotInventory(stack: MatrixStack, slot: Slot) {
    RenderSystem.enableBlend()
    slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None =>
        if (!slot.hasItem && slot.x >= 0 && slot.y >= 0 && component.tierIcon != null) {
          drawDisabledSlot(stack, component)
        }
      case _ =>
        setBlitOffset(getBlitOffset + 1)
        if (!isInPlayerInventory(slot)) {
          drawSlotBackground(stack, slot.x - 1, slot.y - 1)
        }
        if (!slot.hasItem) {
          slot match {
            case component: ComponentSlot =>
              if (component.tierIcon != null) {
                Textures.bind(component.tierIcon)
                AbstractGui.blit(stack, slot.x, slot.y, getBlitOffset, 0, 0, 16, 16, 16, 16)
              }
              if (component.hasBackground) {
                Textures.bind(component.getBackgroundLocation)
                AbstractGui.blit(stack, slot.x, slot.y, getBlitOffset, 0, 0, 16, 16, 16, 16)
              }
            case _ =>
          }
          setBlitOffset(getBlitOffset - 1)
        }
    }
    RenderSystem.disableBlend()
  }

  protected def drawSlotHighlight(matrix: MatrixStack, slot: Slot) {
    if (minecraft.player.inventory.getCarried.isEmpty) slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None => // Ignore.
      case _ =>
        val currentIsInPlayerInventory = isInPlayerInventory(slot)
        val drawHighlight = hoveredSlot match {
          case hovered: Slot =>
            val hoveredIsInPlayerInventory = isInPlayerInventory(hovered)
            (currentIsInPlayerInventory != hoveredIsInPlayerInventory) &&
              ((currentIsInPlayerInventory && slot.hasItem && isSelectiveSlot(hovered) && hovered.mayPlace(slot.getItem)) ||
                (hoveredIsInPlayerInventory && hovered.hasItem && isSelectiveSlot(slot) && slot.mayPlace(hovered.getItem)))
          case _ => hoveredStackNEI match {
            case SomeStack(stack) => !currentIsInPlayerInventory && isSelectiveSlot(slot) && slot.mayPlace(stack)
            case _ => false
          }
        }
        if (drawHighlight) {
          setBlitOffset(getBlitOffset + 100)
          fillGradient(matrix,
            slot.x, slot.y,
            slot.x + 16, slot.y + 16,
            0x80FFFFFF, 0x80FFFFFF)
          setBlitOffset(getBlitOffset - 100)
        }
    }
  }

  private def isSelectiveSlot(slot: Slot) = slot match {
    case component: ComponentSlot => component.slot != common.Slot.Any && component.slot != common.Slot.Tool
    case _ => false
  }

  protected def drawDisabledSlot(stack: MatrixStack, slot: ComponentSlot) {
    RenderSystem.color4f(1, 1, 1, 1)
    Textures.bind(slot.tierIcon)
    AbstractGui.blit(stack, slot.x, slot.y, getBlitOffset, 0, 0, 16, 16, 16, 16)
  }

  protected def drawSlotBackground(stack: MatrixStack, x: Int, y: Int) {
    RenderSystem.color4f(1, 1, 1, 1)
    Textures.bind(Textures.GUI.Slot)
    val t = Tessellator.getInstance
    val r = t.getBuilder
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
    r.vertex(stack.last.pose, x, y + 18, getBlitOffset + 1).uv(0, 1).endVertex()
    r.vertex(stack.last.pose, x + 18, y + 18, getBlitOffset + 1).uv(1, 1).endVertex()
    r.vertex(stack.last.pose, x + 18, y, getBlitOffset + 1).uv(1, 0).endVertex()
    r.vertex(stack.last.pose, x, y, getBlitOffset + 1).uv(0, 0).endVertex()
    t.end()
  }

  private def isInPlayerInventory(slot: Slot) = container match {
    case player: Player => slot.container == player.playerInventory
    case _ => false
  }
}
