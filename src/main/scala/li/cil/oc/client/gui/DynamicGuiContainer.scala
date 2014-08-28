package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.container.{ComponentSlot, Player}
import li.cil.oc.util.RenderState
import li.cil.oc.util.mods.NEI
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.inventory.{Container, Slot}
import net.minecraft.item.ItemStack
import net.minecraft.util.{IIcon, StatCollector}
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsScala._

abstract class DynamicGuiContainer(container: Container) extends CustomGuiContainer(container) {
  protected var hoveredSlot: Option[Slot] = None

  protected var hoveredStackNEI: Option[ItemStack] = None

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
    fontRendererObj.drawString(
      StatCollector.translateToLocal("container.inventory"),
      8, ySize - 96 + 2, 0x404040)
  }

  protected def drawSecondaryBackgroundLayer() {}

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    mc.renderEngine.bindTexture(Textures.guiBackground)
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
      case component: ComponentSlot if component.tier == Tier.None => // Ignore.
      case _ =>
        if (!isInPlayerInventory(slot)) {
          drawSlotBackground(slot.xDisplayPosition - 1, slot.yDisplayPosition - 1)
        }
        if (!slot.getHasStack) slot match {
          case component: ComponentSlot if component.tierIcon != null =>
            mc.getTextureManager.bindTexture(TextureMap.locationItemsTexture)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            drawTexturedModelRectFromIcon(slot.xDisplayPosition, slot.yDisplayPosition, component.tierIcon, 16, 16)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
          case _ =>
        }
    }
  }

  protected def drawSlotHighlight(slot: Slot) {
    slot match {
      case component: ComponentSlot if component.tier == Tier.None => // Ignore.
      case _ =>
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
    mc.getTextureManager.bindTexture(Textures.guiSlot)
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y + 18, zLevel + 1, 0, 1)
    t.addVertexWithUV(x + 18, y + 18, zLevel + 1, 1, 1)
    t.addVertexWithUV(x + 18, y, zLevel + 1, 1, 0)
    t.addVertexWithUV(x, y, zLevel + 1, 0, 0)
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
}
