package li.cil.oc.common.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.opengl.GL11;

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
	// private int maxStringLength = 32;
	private int maxLineLength = 32;
	private boolean enableBackgroundDrawing = true;

	/**
	 * if true the textbox can lose focus by clicking elsewhere on the screen
	 */
	private boolean canLoseFocus = true;

	/**
	 * If this value is true along isEnabled, keyTyped will process the keys.
	 */
	private boolean isFocused;

	/**
	 * If this value is true along isFocused, keyTyped will process the keys.
	 */
	private boolean isEnabled = true;

	private int enabledColor = 14737632;
	private int disabledColor = 7368816;

	/** True if this textbox is visible */
	private boolean visible = true;

	public GuiMultilineTextField(FontRenderer par1FontRenderer, int xPos,
			int yPos, int width, int height) {
		this.fontRenderer = par1FontRenderer;
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
		if (this.getVisible()) {
			if (this.getEnableBackgroundDrawing()) {
				drawRect(this.xPos - 1, this.yPos - 1, this.xPos + this.width
						+ 1, this.yPos + this.height + 1, -6250336);
				drawRect(this.xPos, this.yPos, this.xPos + this.width,
						this.yPos + this.height, -16777216);
			}

			int color = this.isEnabled ? this.enabledColor : this.disabledColor;
			String[] lines = this.text.split("\n");

			int xStart = this.xPos + 4;
			int yStart = this.yPos + 4;
			int currentX = xStart;
			for (String line : lines)

			{
				boolean completeLinePrinted = false;
				while (!completeLinePrinted) {
					String s = fontRenderer.trimStringToWidth(line, getWidth());
					if (s.length() != line.length()) {
						int end = s.lastIndexOf(" ");
						s = s.substring(0, end);
						line = line.substring(end+1);
					}
					else{
						completeLinePrinted = true;
					}

					if (s.length() > 0) {

						int heightOld = fontRenderer.FONT_HEIGHT;
						currentX = fontRenderer.drawStringWithShadow(s, xStart,
								yStart, color);
						yStart += heightOld;

					}
				}

			}
		}
	}

	/**
	 * draws the vertical line cursor in the textbox
	 */
	private void drawCursorVertical(int par1, int par2, int par3, int par4) {
		int i1;

		if (par1 < par3) {
			i1 = par1;
			par1 = par3;
			par3 = i1;
		}

		if (par2 < par4) {
			i1 = par2;
			par2 = par4;
			par4 = i1;
		}

		Tessellator tessellator = Tessellator.instance;
		GL11.glColor4f(0.0F, 0.0F, 255.0F, 255.0F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
		GL11.glLogicOp(GL11.GL_OR_REVERSE);
		tessellator.startDrawingQuads();
		tessellator.addVertex((double) par1, (double) par4, 0.0D);
		tessellator.addVertex((double) par3, (double) par4, 0.0D);
		tessellator.addVertex((double) par3, (double) par2, 0.0D);
		tessellator.addVertex((double) par1, (double) par2, 0.0D);
		tessellator.draw();
		GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
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
	public void setEnableBackgroundDrawing(boolean par1) {
		this.enableBackgroundDrawing = par1;
	}

	/**
	 * Sets the text colour for this textbox (disabled text will not use this
	 * colour)
	 */
	public void setTextColor(int par1) {
		this.enabledColor = par1;
	}

	public void setDisabledTextColour(int par1) {
		this.disabledColor = par1;
	}

	/**
	 * setter for the focused field
	 */
	public void setFocused(boolean par1) {

		this.isFocused = par1;
	}

	/**
	 * getter for the focused field
	 */
	public boolean isFocused() {
		return this.isFocused;
	}

	public void setEnabled(boolean par1) {
		this.isEnabled = par1;
	}

	/**
	 * returns the width of the textbox depending on if the the box is enabled
	 */
	public int getWidth() {
		return this.getEnableBackgroundDrawing() ? this.width - 8 : this.width;
	}

	/**
	 * if true the textbox can lose focus by clicking elsewhere on the screen
	 */
	public void setCanLoseFocus(boolean par1) {
		this.canLoseFocus = par1;
	}

	/**
	 * @return {@code true} if this textbox is visible
	 */
	public boolean getVisible() {
		return this.visible;
	}

	/**
	 * Sets whether or not this textbox is visible
	 */
	public void setVisible(boolean par1) {
		this.visible = par1;
	}
}
