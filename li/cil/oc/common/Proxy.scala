package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.LanguageRegistry
import li.cil.oc._
import li.cil.oc.OpenComputers
import li.cil.oc.api.OpenComputersAPI
import li.cil.oc.server.computer.Drivers
import li.cil.oc.server.drivers._

class Proxy {
  def preInit(e: FMLPreInitializationEvent): Unit = {
    Config.load(e.getSuggestedConfigurationFile)

    LanguageRegistry.instance.loadLocalization(
      "/assets/opencomputers/lang/en_US.lang", "en_US", false)
  }

  def init(e: FMLInitializationEvent): Unit = {
    Blocks.init()
    Items.init()

    NetworkRegistry.instance.registerGuiHandler(OpenComputers, GuiHandler)

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