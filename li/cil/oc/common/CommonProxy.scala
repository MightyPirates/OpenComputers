package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.registry.LanguageRegistry
import li.cil.oc._
import li.cil.oc.api.OpenComputersAPI
import li.cil.oc.server.computer.Drivers
import li.cil.oc.server.drivers._
import cpw.mods.fml.common.network.NetworkRegistry
import li.cil.oc.gui.GuiHandler
import li.cil.oc.OpenComputers

class CommonProxy {
  def preInit(e: FMLPreInitializationEvent): Unit = {
    val config = new net.minecraftforge.common.Configuration(e.getSuggestedConfigurationFile())

    Config.blockComputerId = config.getBlock("computer", Config.blockComputerId,
      "The block ID used for computers.").getInt(Config.blockComputerId)
    Config.blockScreenId = config.getBlock("screen", Config.blockScreenId,
      "The block ID used for screens.").getInt(Config.blockScreenId)
  }

  def init(e: FMLInitializationEvent): Unit = {
    Blocks.init()
    Items.init()

    // TODO Figure out how resource pack based localization works.
    LanguageRegistry.addName(Blocks.computer, "Computer")
    NetworkRegistry.instance().registerGuiHandler(OpenComputers, new GuiHandler());
    OpenComputersAPI.addDriver(GraphicsCardDriver)
    OpenComputersAPI.addDriver(ScreenDriver)
  }

  def postInit(e: FMLPostInitializationEvent): Unit = {
    // Lock the driver registry to avoid drivers being added after computers
    // may have already started up. This makes sure the driver API won't change
    // over the course of a game, since that could lead to weird effects.
    Drivers.locked = true
  }
}