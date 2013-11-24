package li.cil.oc.client

import cpw.mods.fml.client.registry.{RenderingRegistry, ClientRegistry}
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.TickRegistry
import cpw.mods.fml.relauncher.Side
import li.cil.oc.{Config, OpenComputers}
import li.cil.oc.client.renderer.tileentity.{KeyboardRenderer, PowerDistributorRenderer, ScreenRenderer, ComputerRenderer}
import li.cil.oc.common.tileentity
import li.cil.oc.common.{Proxy => CommonProxy}
import net.minecraftforge.common.MinecraftForge

private[oc] class Proxy extends CommonProxy {
  override def init(e: FMLInitializationEvent) = {
    super.init(e)

    NetworkRegistry.instance.registerGuiHandler(OpenComputers, GuiHandler)

    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Screen], ScreenRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Computer], ComputerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.PowerDistributor], PowerDistributorRenderer)
    //ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Keyboard], KeyboardRenderer)
      //   RenderingRegistry.registerBlockHandler()
    TickRegistry.registerTickHandler(ScreenRenderer, Side.CLIENT)

    Config.blockRenderId =RenderingRegistry.getNextAvailableRenderId
    RenderingRegistry.registerBlockHandler(KeyboardRenderer)
    MinecraftForge.EVENT_BUS.register(gui.Icons)
  }
}