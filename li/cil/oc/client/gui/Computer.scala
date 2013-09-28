package li.cil.oc.client.gui

import li.cil.oc.common.container
import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.util.ResourceLocation
import net.minecraft.util.StatCollector
import org.lwjgl.opengl.GL11

class Computer(inventory: InventoryPlayer, val tileEntity: TileEntityComputer) extends GuiContainer(new container.Computer(inventory, tileEntity)) {
  private val background = new ResourceLocation("opencomputers", "textures/gui/computer.png")

  private val iconPsu = new ResourceLocation("opencomputers", "textures/gui/icon_psu.png")
  private val iconPci = new ResourceLocation("opencomputers", "textures/gui/icon_pci.png")
  private val iconRam = new ResourceLocation("opencomputers", "textures/gui/icon_ram.png")
  private val iconHdd = new ResourceLocation("opencomputers", "textures/gui/icon_hdd.png")

  private val icons = Array(iconPsu, iconPci, iconPci, iconPci, iconRam, iconRam, iconHdd, iconHdd)

  private var (x, y) = (0, 0)

  override def initGui() = {
    super.initGui()
    x = (width - xSize) / 2
    y = (height - ySize) / 2
  }

  override def drawSlotInventory(slot: Slot) = {
    super.drawSlotInventory(slot)
    if (slot.slotNumber < 8 && !slot.getHasStack)
      drawSlotIcon(slot, icons(slot.slotNumber))
  }

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    fontRenderer.drawString(
      StatCollector.translateToLocal("oc.container.computer"),
      8, 6, 0x404040)
    fontRenderer.drawString(
      StatCollector.translateToLocal("container.inventory"),
      8, ySize - 96 + 2, 0x404040)
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) = {
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
    setTexture(background)
    drawTexturedModalRect(x, y, 0, 0, xSize, ySize)
  }

  override def doesGuiPauseGame = false

  private def drawSlotIcon(slot: Slot, icon: ResourceLocation) = {
    GL11.glPushAttrib(0xFFFFFF)
    GL11.glDisable(GL11.GL_LIGHTING)
    GL11.glDisable(GL11.GL_DEPTH_TEST)
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    GL11.glColor4f(1, 1, 1, 0.25f)
    setTexture(icon)
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(slot.xDisplayPosition, slot.yDisplayPosition + 16, zLevel, 0, 1)
    t.addVertexWithUV(slot.xDisplayPosition + 16, slot.yDisplayPosition + 16, zLevel, 1, 1)
    t.addVertexWithUV(slot.xDisplayPosition + 16, slot.yDisplayPosition, zLevel, 1, 0)
    t.addVertexWithUV(slot.xDisplayPosition, slot.yDisplayPosition, zLevel, 0, 0)
    t.draw()
    GL11.glPopAttrib()
  }

  private def setTexture(value: ResourceLocation) =
    mc.renderEngine.func_110577_a(value)
}