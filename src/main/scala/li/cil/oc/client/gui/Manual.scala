package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.util.PseudoMarkdown
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution

class Manual extends GuiScreen {
  val document = PseudoMarkdown.parse( """# Headline
                                         |
                                         |The Adapter block is the core of most of OpenComputers' mod integration.
                                         |
                                         |*This* is *italic* text, ~~strikethrough~~ maybe a-ter **some** text **in bold**. Is _this underlined_? Oh, no, _it's also italic!_ Well, this \*isn't bold*.
                                         |
                                         |## Smaller headline
                                         |
                                         |This is *italic
                                         |over two* lines. But *this ... no *this is* **_bold italic_** *text*.
                                         |
                                         |### even smaller
                                         |
                                         |*not italic *because ** why would it be*eh
                                         |
                                         |isn't*.
                                         |
                                         |   # not a header
                                         |
                                         |![](https://avatars1.githubusercontent.com/u/514903)
                                         |
                                         |And finally, [this is a link!](https://avatars1.githubusercontent.com/u/514903).""".stripMargin)

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    val mc = Minecraft.getMinecraft
    val screenSize = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight)
    val guiSize = new ScaledResolution(mc, 256, 192)
    val (midX, midY) = (screenSize.getScaledWidth / 2, screenSize.getScaledHeight / 2)
    val (left, top) = (midX - guiSize.getScaledWidth / 2, midY - guiSize.getScaledHeight / 2)

    mc.renderEngine.bindTexture(Textures.guiManual)
    Gui.func_146110_a(left, top, 0, 0, guiSize.getScaledWidth, guiSize.getScaledHeight, 256, 192)

    super.drawScreen(mouseX, mouseY, dt)

    PseudoMarkdown.render(document, left + 8, top + 8, 220, 176, 0, fontRendererObj)
  }
}
