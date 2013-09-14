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
  private final int xPos;
  private final int yPos;

  /** The width of this text field. */
  private final int width;
  private final int height;

  /** Have the current text beign edited on the textbox. */
  private String text = "";
  private boolean enableBackgroundDrawing = true;

  private int enabledColor = 0xFFFFFF;

  public GuiMultilineTextField(FontRenderer fontRenderer, int xPos, int yPos,
      int width, int height) {
    this.fontRenderer = fontRenderer;
    this.xPos = xPos;
    this.yPos = yPos;
    this.width = width;
    this.height = height;
  }

  /**
   * Sets the text of the textbox.
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * Returns the text beign edited on the textbox.
   */
  public String getText() {
    return this.text;
  }

  /**
   * Draws the textbox
   */
  public void drawTextBox() {
    if (this.getEnableBackgroundDrawing()) {
      drawRect(this.xPos, this.yPos, this.xPos + this.width, this.yPos
          + this.height, 0xFF000000);
    }

    final int color = this.enabledColor;
    String[] lines = this.text.split("\n");

    int xStart = this.xPos + 2;
    int yStart = this.yPos + 2;
    int currentX = xStart;

    for (String line : lines) {
      boolean completeLinePrinted = false;
      while (!completeLinePrinted) {
        String s = fontRenderer.trimStringToWidth(line, getWidth());
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
        if (yStart > height + yPos) {
          return;
        }
      }
    }
  }

  /**
   * get enable drawing background and outline
   */
  public boolean getEnableBackgroundDrawing() {
    return this.enableBackgroundDrawing;
  }

  /**
   * enable drawing background and outline
   */
  public void setEnableBackgroundDrawing(boolean value) {
    this.enableBackgroundDrawing = value;
  }

  /**
   * Sets the text colour for this textbox (disabled text will not use this
   * colour)
   */
  public void setTextColor(int value) {
    this.enabledColor = value;
  }

  /**
   * returns the width of the textbox depending on if the the box is enabled
   */
  public int getWidth() {
    return this.getEnableBackgroundDrawing() ? this.width - 4 : this.width;
  }
}
