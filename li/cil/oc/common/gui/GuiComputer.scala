package li.cil.oc.common.gui

import org.lwjgl.opengl.GL11

import li.cil.oc.common.container.ContainerComputer
import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.StatCollector

class GuiComputer(inventory: InventoryPlayer, val tileEntity: TileEntityComputer) extends GuiContainer(new ContainerComputer(inventory, tileEntity)) {
  val button = new GuiButton(1, 5, 4, "test")
  System.out.println("new Gui")
  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    //draw text and stuff here
    //the parameters for drawString are: string, x, y, color
    fontRenderer.drawString("Computer ", 8, 6, 4210752);
    //draws "Inventory" or your regional equivalent
    fontRenderer.drawString(StatCollector.translateToLocal("oc.container.computer"), 8, ySize - 96 + 2, 4210752);
  }

  override def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) = {
    //draw your Gui here, only thing you need to change is the path
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    this.mc.renderEngine.func_110577_a(new ResourceLocation(""));
    val x = (width - xSize * 2) / 2;
    val y = (height - ySize * 2) / 2;
    this.drawTexturedModalRect(x, y, 0, 0, xSize * 2, ySize * 2);
  }

  

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) = {
    super.drawScreen(mouseX, mouseY, dt);

    button.drawButton(this.mc, mouseX, mouseY)

  }
}