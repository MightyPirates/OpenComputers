package li.cil.oc.client.gui

import java.util
import li.cil.oc.api
import li.cil.oc.Settings
import li.cil.oc.client.{PacketSender => ClientPacketSender, Textures}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector
import org.lwjgl.opengl.GL11

class RobotAssembler(playerInventory: InventoryPlayer, val assembler: tileentity.RobotAssembler) extends DynamicGuiContainer(new container.RobotAssembler(playerInventory, assembler)) {
  xSize = 176
  ySize = 192

  protected var runButton: ImageButton = _

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0 && !assembler.isAssembling && assembler.complexity <= assembler.maxComplexity) {
      ClientPacketSender.sendRobotAssemblerStart(assembler)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    runButton.enabled = assembler.complexity <= assembler.maxComplexity && !assembler.isAssembling
    runButton.toggled = !runButton.enabled
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    runButton = new ImageButton(0, guiLeft + 7, guiTop + 89, 18, 18, Textures.guiButtonRun, canToggle = true)
    add(buttonList, runButton)
  }

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    GL11.glPushAttrib(0xFFFFFFFF) // Me lazy... prevents NEI render glitch.
    if (!assembler.isAssembling) {
      if (!inventorySlots.getSlot(0).getHasStack) {
        val message =
          if (api.Items.get(inventorySlots.getSlot(0).getStack) == api.Items.get("robot"))
            "gui.RobotAssembler.CollectRobot"
          else
            "gui.RobotAssembler.InsertCase"
        fontRenderer.drawString(
          StatCollector.translateToLocal(Settings.namespace + message),
          30, 94, 0x404040)
      }
      else {
        fontRenderer.drawString(
          StatCollector.translateToLocalFormatted(Settings.namespace + "gui.RobotAssembler.Complexity", Int.box(assembler.complexity), Int.box(assembler.maxComplexity)),
          30, 94, if (assembler.complexity <= assembler.maxComplexity) 0x404040 else 0x804040)
      }
    }
    if (runButton.func_82252_a && !assembler.isAssembling) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(StatCollector.translateToLocal(Settings.namespace + "gui.RobotAssembler.Run"))
      drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    GL11.glPopAttrib()
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    super.drawGuiContainerBackgroundLayer(dt, mouseX, mouseY)
    mc.renderEngine.bindTexture(Textures.guiRobotAssembler)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }

  override def doesGuiPauseGame = false
}