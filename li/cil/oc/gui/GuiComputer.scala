package li.cil.oc.gui

import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.client.gui.inventory.GuiContainer
import li.cil.oc.container.ContainerComputer
import net.minecraft.util.StatCollector
import com.sun.xml.internal.bind.annotation.OverrideAnnotationOf
import org.lwjgl.opengl.GL11
import net.minecraft.util.ResourceLocation

class GuiComputer(inventory: InventoryPlayer, tile: TileEntityComputer) extends GuiContainer(new ContainerComputer(inventory, tile)) {
  val tileEntity = tile

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
    var x = (width - xSize) / 2;
    var y = (height - ySize) / 2;
    this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
  }
  
  override def initGui()={
    super.initGui()
    
    
  }
}