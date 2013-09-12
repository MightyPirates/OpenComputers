package li.cil.oc.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class GuiMultilineTextField extends Gui
{
    /**
     * Have the font renderer from GuiScreen to render the textbox text into the screen.
     */
    private final FontRenderer fontRenderer;
    private final int xPos;
    private final int yPos;

    /** The width of this text field. */
    private final int width;
    private final int height;

    /** Have the current text beign edited on the textbox. */
    private String text = "";
    private int maxStringLength = 32;
    private int cursorCounter;
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

    /**
     * The current character index that should be used as start of the rendered text.
     */
    private int lineScrollOffset;
    private int cursorPosition;

    /** other selection position, maybe the same as the cursor */
    private int selectionEnd;
    private int enabledColor = 14737632;
    private int disabledColor = 7368816;

    /** True if this textbox is visible */
    private boolean visible = true;

    public GuiMultilineTextField(FontRenderer par1FontRenderer, int xPos, int yPos, int width, int height)
    {
        this.fontRenderer = par1FontRenderer;
        this.xPos = xPos;
        this.yPos = yPos;
        this.width = width;
        this.height = height;
    }

    /**
     * Increments the cursor counter
     */
    public void updateCursorCounter()
    {
        ++this.cursorCounter;
    }

    /**
     * Sets the text of the textbox.
     */
    public void setText(String text)
    {
        if (text.length() > this.maxStringLength)
        {
            this.text = text.substring(0, this.maxStringLength);
        }
        else
        {
            this.text = text;
        }

        this.setCursorPositionEnd();
    }

    /**
     * Returns the text beign edited on the textbox.
     */
    public String getText()
    {
        return this.text;
    }

    /**
     * @return returns the text between the cursor and selectionEnd
     */
    public String getSelectedtext()
    {
        int i = this.cursorPosition < this.selectionEnd ? this.cursorPosition : this.selectionEnd;
        int j = this.cursorPosition < this.selectionEnd ? this.selectionEnd : this.cursorPosition;
        return this.text.substring(i, j);
    }

    /**
     * replaces selected text, or inserts text at the position on the cursor
     */
    public void writeText(String par1Str)
    {
        String s1 = "";
        String s2 = ChatAllowedCharacters.filerAllowedCharacters(par1Str);
        int i = this.cursorPosition < this.selectionEnd ? this.cursorPosition : this.selectionEnd;
        int j = this.cursorPosition < this.selectionEnd ? this.selectionEnd : this.cursorPosition;
        int k = this.maxStringLength - this.text.length() - (i - this.selectionEnd);
        boolean flag = false;

        if (this.text.length() > 0)
        {
            s1 = s1 + this.text.substring(0, i);
        }

        int l;

        if (k < s2.length())
        {
            s1 = s1 + s2.substring(0, k);
            l = k;
        }
        else
        {
            s1 = s1 + s2;
            l = s2.length();
        }

        if (this.text.length() > 0 && j < this.text.length())
        {
            s1 = s1 + this.text.substring(j);
        }

        this.text = s1;
        this.moveCursorBy(i - this.selectionEnd + l);
    }

    /**
     * Deletes the specified number of words starting at the cursor position. Negative numbers will delete words left of
     * the cursor.
     */
    public void deleteWords(int par1)
    {
        if (this.text.length() != 0)
        {
            if (this.selectionEnd != this.cursorPosition)
            {
                this.writeText("");
            }
            else
            {
                this.deleteFromCursor(this.getNthWordFromCursor(par1) - this.cursorPosition);
            }
        }
    }

    /**
     * delete the selected text, otherwsie deletes characters from either side of the cursor. params: delete num
     */
    public void deleteFromCursor(int par1)
    {
        if (this.text.length() != 0)
        {
            if (this.selectionEnd != this.cursorPosition)
            {
                this.writeText("");
            }
            else
            {
                boolean flag = par1 < 0;
                int j = flag ? this.cursorPosition + par1 : this.cursorPosition;
                int k = flag ? this.cursorPosition : this.cursorPosition + par1;
                String s = "";

                if (j >= 0)
                {
                    s = this.text.substring(0, j);
                }

                if (k < this.text.length())
                {
                    s = s + this.text.substring(k);
                }

                this.text = s;

                if (flag)
                {
                    this.moveCursorBy(par1);
                }
            }
        }
    }

    /**
     * see @getNthNextWordFromPos() params: N, position
     */
    public int getNthWordFromCursor(int par1)
    {
        return this.getNthWordFromPos(par1, this.getCursorPosition());
    }

    /**
     * gets the position of the nth word. N may be negative, then it looks backwards. params: N, position
     */
    public int getNthWordFromPos(int par1, int par2)
    {
        return this.func_73798_a(par1, this.getCursorPosition(), true);
    }

    public int func_73798_a(int par1, int par2, boolean par3)
    {
        int k = par2;
        boolean flag1 = par1 < 0;
        int l = Math.abs(par1);

        for (int i1 = 0; i1 < l; ++i1)
        {
            if (flag1)
            {
                while (par3 && k > 0 && this.text.charAt(k - 1) == 32)
                {
                    --k;
                }

                while (k > 0 && this.text.charAt(k - 1) != 32)
                {
                    --k;
                }
            }
            else
            {
                int j1 = this.text.length();
                k = this.text.indexOf(32, k);

                if (k == -1)
                {
                    k = j1;
                }
                else
                {
                    while (par3 && k < j1 && this.text.charAt(k) == 32)
                    {
                        ++k;
                    }
                }
            }
        }

        return k;
    }

    /**
     * Moves the text cursor by a specified number of characters and clears the selection
     */
    public void moveCursorBy(int par1)
    {
        this.setCursorPosition(this.selectionEnd + par1);
    }

    /**
     * sets the position of the cursor to the provided index
     */
    public void setCursorPosition(int par1)
    {
        this.cursorPosition = par1;
        int j = this.text.length();

        if (this.cursorPosition < 0)
        {
            this.cursorPosition = 0;
        }

        if (this.cursorPosition > j)
        {
            this.cursorPosition = j;
        }

        this.setSelectionPos(this.cursorPosition);
    }

    /**
     * sets the cursors position to the beginning
     */
    public void setCursorPositionZero()
    {
        this.setCursorPosition(0);
    }

    /**
     * sets the cursors position to after the text
     */
    public void setCursorPositionEnd()
    {
        this.setCursorPosition(this.text.length());
    }

    /**
     * Call this method from you GuiScreen to process the keys into textbox.
     */
    public boolean textboxKeyTyped(char par1, int par2)
    {
        if (this.isEnabled && this.isFocused)
        {
            switch (par1)
            {
                case 1:
                    this.setCursorPositionEnd();
                    this.setSelectionPos(0);
                    return true;
                case 3:
                    GuiScreen.setClipboardString(this.getSelectedtext());
                    return true;
                case 22:
                    this.writeText(GuiScreen.getClipboardString());
                    return true;
                case 24:
                    GuiScreen.setClipboardString(this.getSelectedtext());
                    this.writeText("");
                    return true;
                default:
                    switch (par2)
                    {
                        case 14:
                            if (GuiScreen.isCtrlKeyDown())
                            {
                                this.deleteWords(-1);
                            }
                            else
                            {
                                this.deleteFromCursor(-1);
                            }

                            return true;
                        case 199:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                this.setSelectionPos(0);
                            }
                            else
                            {
                                this.setCursorPositionZero();
                            }

                            return true;
                        case 203:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                if (GuiScreen.isCtrlKeyDown())
                                {
                                    this.setSelectionPos(this.getNthWordFromPos(-1, this.getSelectionEnd()));
                                }
                                else
                                {
                                    this.setSelectionPos(this.getSelectionEnd() - 1);
                                }
                            }
                            else if (GuiScreen.isCtrlKeyDown())
                            {
                                this.setCursorPosition(this.getNthWordFromCursor(-1));
                            }
                            else
                            {
                                this.moveCursorBy(-1);
                            }

                            return true;
                        case 205:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                if (GuiScreen.isCtrlKeyDown())
                                {
                                    this.setSelectionPos(this.getNthWordFromPos(1, this.getSelectionEnd()));
                                }
                                else
                                {
                                    this.setSelectionPos(this.getSelectionEnd() + 1);
                                }
                            }
                            else if (GuiScreen.isCtrlKeyDown())
                            {
                                this.setCursorPosition(this.getNthWordFromCursor(1));
                            }
                            else
                            {
                                this.moveCursorBy(1);
                            }

                            return true;
                        case 207:
                            if (GuiScreen.isShiftKeyDown())
                            {
                                this.setSelectionPos(this.text.length());
                            }
                            else
                            {
                                this.setCursorPositionEnd();
                            }

                            return true;
                        case 211:
                            if (GuiScreen.isCtrlKeyDown())
                            {
                                this.deleteWords(1);
                            }
                            else
                            {
                                this.deleteFromCursor(1);
                            }

                            return true;
                        default:
                            if (ChatAllowedCharacters.isAllowedCharacter(par1))
                            {
                                this.writeText(Character.toString(par1));
                                return true;
                            }
                            else
                            {
                                return false;
                            }
                    }
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * Args: x, y, buttonClicked
     */
    public void mouseClicked(int par1, int par2, int par3)
    {
        boolean flag = par1 >= this.xPos && par1 < this.xPos + this.width && par2 >= this.yPos && par2 < this.yPos + this.height;

        if (this.canLoseFocus)
        {
            this.setFocused(this.isEnabled && flag);
        }

        if (this.isFocused && par3 == 0)
        {
            int l = par1 - this.xPos;

            if (this.enableBackgroundDrawing)
            {
                l -= 4;
            }

            String s = this.fontRenderer.trimStringToWidth(this.text.substring(this.lineScrollOffset), this.getWidth());
            this.setCursorPosition(this.fontRenderer.trimStringToWidth(s, l).length() + this.lineScrollOffset);
        }
    }

    /**
     * Draws the textbox
     */
    public void drawTextBox()
    {
        if (this.getVisible())
        {
            if (this.getEnableBackgroundDrawing())
            {
                drawRect(this.xPos - 1, this.yPos - 1, this.xPos + this.width + 1, this.yPos + this.height + 1, -6250336);
                drawRect(this.xPos, this.yPos, this.xPos + this.width, this.yPos + this.height, -16777216);
            }

            int color = this.isEnabled ? this.enabledColor : this.disabledColor;
            int cursorMinusScroll = this.cursorPosition - this.lineScrollOffset;
            int selectionMinusOffset = this.selectionEnd - this.lineScrollOffset;
            String s = this.fontRenderer.trimStringToWidth(this.text.substring(this.lineScrollOffset), this.getWidth());
            boolean flag = cursorMinusScroll >= 0 && cursorMinusScroll <= s.length();
            boolean flag1 = this.isFocused && this.cursorCounter / 6 % 2 == 0 && flag;
            int xStart = this.enableBackgroundDrawing ? this.xPos + 4 : this.xPos;
            int yStart = this.enableBackgroundDrawing ? this.yPos /*+ (this.height - 8) / 2*/ : this.yPos;
            int currentX = xStart;

            if (selectionMinusOffset > s.length())
            {
                selectionMinusOffset = s.length();
            }

            if (s.length() > 0)
            {
                String s1 = flag ? s.substring(0, cursorMinusScroll) : s;
                int heightOld = this.fontRenderer.FONT_HEIGHT;
                this.fontRenderer.FONT_HEIGHT = 3;
                currentX = this.fontRenderer.drawStringWithShadow(s1, xStart, yStart, color);
                this.fontRenderer.FONT_HEIGHT = heightOld;
                
            }

            boolean flag2 = this.cursorPosition < this.text.length() || this.text.length() >= this.getMaxStringLength();
            int k1 = currentX;

            if (!flag)
            {
                k1 = cursorMinusScroll > 0 ? xStart + this.width : xStart;
            }
            else if (flag2)
            {
                k1 = currentX - 1;
                --currentX;
            }

            if (s.length() > 0 && flag && cursorMinusScroll < s.length())
            {
                this.fontRenderer.drawStringWithShadow(s.substring(cursorMinusScroll), currentX, yStart, color);
            }

            if (flag1)
            {
                if (flag2)
                {
                    Gui.drawRect(k1, yStart - 1, k1 + 1, yStart + 1 + this.fontRenderer.FONT_HEIGHT, -3092272);
                }
                else
                {
                    this.fontRenderer.drawStringWithShadow("_", k1, yStart, color);
                }
            }

            if (selectionMinusOffset != cursorMinusScroll)
            {
                int l1 = xStart + this.fontRenderer.getStringWidth(s.substring(0, selectionMinusOffset));
                this.drawCursorVertical(k1, yStart - 1, l1 - 1, yStart + 1 + this.fontRenderer.FONT_HEIGHT);
            }
        }
    }

    /**
     * draws the vertical line cursor in the textbox
     */
    private void drawCursorVertical(int par1, int par2, int par3, int par4)
    {
        int i1;

        if (par1 < par3)
        {
            i1 = par1;
            par1 = par3;
            par3 = i1;
        }

        if (par2 < par4)
        {
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
        tessellator.addVertex((double)par1, (double)par4, 0.0D);
        tessellator.addVertex((double)par3, (double)par4, 0.0D);
        tessellator.addVertex((double)par3, (double)par2, 0.0D);
        tessellator.addVertex((double)par1, (double)par2, 0.0D);
        tessellator.draw();
        GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public void setMaxStringLength(int par1)
    {
        this.maxStringLength = par1;

        if (this.text.length() > par1)
        {
            this.text = this.text.substring(0, par1);
        }
    }

    /**
     * returns the maximum number of character that can be contained in this textbox
     */
    public int getMaxStringLength()
    {
        return this.maxStringLength;
    }

    /**
     * returns the current position of the cursor
     */
    public int getCursorPosition()
    {
        return this.cursorPosition;
    }

    /**
     * get enable drawing background and outline
     */
    public boolean getEnableBackgroundDrawing()
    {
        return this.enableBackgroundDrawing;
    }

    /**
     * enable drawing background and outline
     */
    public void setEnableBackgroundDrawing(boolean par1)
    {
        this.enableBackgroundDrawing = par1;
    }

    /**
     * Sets the text colour for this textbox (disabled text will not use this colour)
     */
    public void setTextColor(int par1)
    {
        this.enabledColor = par1;
    }

    public void setDisabledTextColour(int par1)
    {
        this.disabledColor = par1;
    }

    /**
     * setter for the focused field
     */
    public void setFocused(boolean par1)
    {
        if (par1 && !this.isFocused)
        {
            this.cursorCounter = 0;
        }

        this.isFocused = par1;
    }

    /**
     * getter for the focused field
     */
    public boolean isFocused()
    {
        return this.isFocused;
    }

    public void setEnabled(boolean par1)
    {
        this.isEnabled = par1;
    }

    /**
     * the side of the selection that is not the cursor, maye be the same as the cursor
     */
    public int getSelectionEnd()
    {
        return this.selectionEnd;
    }

    /**
     * returns the width of the textbox depending on if the the box is enabled
     */
    public int getWidth()
    {
        return this.getEnableBackgroundDrawing() ? this.width - 8 : this.width;
    }

    /**
     * Sets the position of the selection anchor (i.e. position the selection was started at)
     */
    public void setSelectionPos(int par1)
    {
        int j = this.text.length();

        if (par1 > j)
        {
            par1 = j;
        }

        if (par1 < 0)
        {
            par1 = 0;
        }

        this.selectionEnd = par1;

        if (this.fontRenderer != null)
        {
            if (this.lineScrollOffset > j)
            {
                this.lineScrollOffset = j;
            }

            int k = this.getWidth();
            String s = this.fontRenderer.trimStringToWidth(this.text.substring(this.lineScrollOffset), k);
            int l = s.length() + this.lineScrollOffset;

            if (par1 == this.lineScrollOffset)
            {
                this.lineScrollOffset -= this.fontRenderer.trimStringToWidth(this.text, k, true).length();
            }

            if (par1 > l)
            {
                this.lineScrollOffset += par1 - l;
            }
            else if (par1 <= this.lineScrollOffset)
            {
                this.lineScrollOffset -= this.lineScrollOffset - par1;
            }

            if (this.lineScrollOffset < 0)
            {
                this.lineScrollOffset = 0;
            }

            if (this.lineScrollOffset > j)
            {
                this.lineScrollOffset = j;
            }
        }
    }

    /**
     * if true the textbox can lose focus by clicking elsewhere on the screen
     */
    public void setCanLoseFocus(boolean par1)
    {
        this.canLoseFocus = par1;
    }

    /**
     * @return {@code true} if this textbox is visible
     */
    public boolean getVisible()
    {
        return this.visible;
    }

    /**
     * Sets whether or not this textbox is visible
     */
    public void setVisible(boolean par1)
    {
        this.visible = par1;
    }
}

