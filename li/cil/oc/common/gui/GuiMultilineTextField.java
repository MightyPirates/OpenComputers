package li.cil.oc.common.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiMultilineTextField extends Gui {
  /**
   * Have the font renderer from GuiScreen to render the textbox text into the
   * screen.
   */
  private final FontRenderer fontRenderer;
  private int x;
  private int y;

  /** The width of this text field. */
  private int width;
  private int height;

  /** Have the current text beign edited on the textbox. */
  private String text = "";

  private int enabledColor = 0xFFFFFF;

  public GuiMultilineTextField(FontRenderer fontRenderer) {
    this.fontRenderer = fontRenderer;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setBounds(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  /**
   * Draws the textbox
   */
  public void drawTextBox() {
    final int padding = 2;
    drawRect(x, y, x + width, y + height, 0xFF000000);

    final int color = this.enabledColor;
    String[] lines = this.text.split("\n");

    int xStart = this.x + padding;
    int yStart = this.y + padding;
    int currentX = xStart;

    for (String line : lines) {
      boolean completeLinePrinted = false;
      while (!completeLinePrinted) {
        String s = fontRenderer.trimStringToWidth(line, width - padding * 2);
        if (s.length() != line.length()) {

          int end = s.lastIndexOf(" ");
          if (end == -1) {
            end = s.length();
          }
          s = s.substring(0, end);
          line = line.substring(end + 1);
        } else {
          completeLinePrinted = true;
        }

        if (s.length() > 0) {
          int heightOld = fontRenderer.FONT_HEIGHT;
          currentX = fontRenderer.drawString(s, xStart, yStart, color);
          yStart += heightOld;

        }
        if (yStart > height + y) {
          return;
        }
      }
    }
  }

  /**
   * Sets the text colour for this textbox (disabled text will not use this
   * colour)
   */
  public void setTextColor(int value) {
    this.enabledColor = value;
  }
}
