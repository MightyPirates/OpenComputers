package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.container.{ComponentSlot, Player}
import li.cil.oc.util.RenderState
import li.cil.oc.util.mods.NEI
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.inventory.{Container, Slot}
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsScala._

abstract class DynamicGuiContainer(container: Container) extends CustomGuiContainer(container) {
  protected var hoveredSlot: Option[Slot] = None

  protected var hoveredStackNEI: Option[ItemStack] = None

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    fontRenderer.drawString(
      StatCollector.translateToLocal("container.inventory"),
      8, ySize - 96 + 2, 0x404040)
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor4f(1, 1, 1, 1)
    mc.renderEngine.bindTexture(Textures.guiBackground)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    hoveredSlot = (inventorySlots.inventorySlots collect {
      case slot: Slot if isPointInRegion(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY) => slot
    }).headOption
    hoveredStackNEI = NEI.hoveredStack(this, mouseX, mouseY)
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def drawSlotInventory(slot: Slot) {
    slot match {
      case component: ComponentSlot if component.tier == common.Tier.None || component.slot == common.Slot.None => // Ignore.
      case _ =>
        if (!isInPlayerInventory(slot)) {
          GL11.glDisable(GL11.GL_DEPTH_TEST)
          GL11.glDisable(GL11.GL_LIGHTING)
          drawSlotBackground(slot.xDisplayPosition - 1, slot.yDisplayPosition - 1)
          GL11.glEnable(GL11.GL_LIGHTING)
          GL11.glEnable(GL11.GL_DEPTH_TEST)
        }

        RenderState.makeItBlend()
        super.drawSlotInventory(slot)
        GL11.glDisable(GL11.GL_BLEND)
        if (!slot.getHasStack) slot match {
          case component: ComponentSlot if component.tierIcon != null =>
            mc.getTextureManager.bindTexture(TextureMap.locationItemsTexture)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDisable(GL11.GL_LIGHTING)
            drawTexturedModelRectFromIcon(slot.xDisplayPosition, slot.yDisplayPosition, component.tierIcon, 16, 16)
            GL11.glEnable(GL11.GL_LIGHTING)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
          case _ =>
        }

        if (mc.thePlayer.inventory.getItemStack == null) {
          val currentIsInPlayerInventory = isInPlayerInventory(slot)
          val drawHighlight = hoveredSlot match {
            case Some(hovered) =>
              val hoveredIsInPlayerInventory = isInPlayerInventory(hovered)
              (currentIsInPlayerInventory != hoveredIsInPlayerInventory) &&
                ((currentIsInPlayerInventory && slot.getHasStack && hovered.isItemValid(slot.getStack)) ||
                  (hoveredIsInPlayerInventory && hovered.getHasStack && slot.isItemValid(hovered.getStack)))
            case _ => hoveredStackNEI match {
              case Some(stack) => !currentIsInPlayerInventory && slot.isItemValid(stack)
              case _ => false
            }
          }
          if (drawHighlight) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDisable(GL11.GL_LIGHTING)
            drawGradientRect(slot.xDisplayPosition, slot.yDisplayPosition, slot.xDisplayPosition + 16, slot.yDisplayPosition + 16, 0x80FFFFFF, 0x80FFFFFF)
            GL11.glEnable(GL11.GL_LIGHTING)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
          }
        }
    }
  }

  private def drawSlotBackground(x: Int, y: Int) {
    GL11.glColor4f(1, 1, 1, 1)
    mc.renderEngine.bindTexture(Textures.guiSlot)
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y + 18, zLevel, 0, 1)
    t.addVertexWithUV(x + 18, y + 18, zLevel, 1, 1)
    t.addVertexWithUV(x + 18, y, zLevel, 1, 0)
    t.addVertexWithUV(x, y, zLevel, 0, 0)
    t.draw()
  }

  private def isInPlayerInventory(slot: Slot) = container match {
    case player: Player => slot.inventory == player.playerInventory
    case _ => false
  }
}
