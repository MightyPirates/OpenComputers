package li.cil.oc.gui

import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.util.StatCollector
import com.sun.xml.internal.bind.annotation.OverrideAnnotationOf
import org.lwjgl.opengl.GL11
import net.minecraft.util.ResourceLocation
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiTextField
import li.cil.oc.common.container.ContainerComputer

class GuiComputer(inventory: InventoryPlayer, tile: TileEntityComputer) extends GuiContainer(new ContainerComputer(inventory, tile)) {
  val tileEntity = tile
	var b = new GuiButton(1,5,4,"test") 
  var t:GuiMultilineTextField = null
  
 
  override def drawGuiContainerForegroundLayer(param1: Int, par2: Int) = {
    //draw text and stuff here
    //the parameters for drawString are: string, x, y, color
    fontRenderer.drawString("Computer ", 8, 6, 4210752);
    //draws "Inventory" or your regional equivalent
    fontRenderer.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 4210752);
  }

  override def drawGuiContainerBackgroundLayer(f: Float, i: Int, j: Int) = {
    //draw your Gui here, only thing you need to change is the path

    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    this.mc.renderEngine.func_110577_a(new ResourceLocation(""));
    var x = (width - xSize*2) / 2;
    var y = (height - ySize*2) / 2;
    this.drawTexturedModalRect(x, y, 0, 0, xSize*2, ySize*2);
  }
  
  override def initGui()={
    super.initGui()
    System.out.println(" FONTRENDERER != NULL???"+this.fontRenderer)
     t = new GuiMultilineTextField(this.fontRenderer,20,0,200,200)
    t.setText("Hallo das ist ein TEst\n über mehrere zeilen?")
    
    }
  override def drawScreen(i:Int,j:Int,f:Float)={
    super.drawScreen(i, j, f);
    b.drawButton(this.mc,i,j)
    
    t.drawTextBox()
    t.setCursorPosition(0)
    
  }
}