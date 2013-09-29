package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.LanguageRegistry
import li.cil.oc._
import li.cil.oc.api.Driver
import li.cil.oc.api.Network
import li.cil.oc.server.computer.Computer
import li.cil.oc.server.driver
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.network
import net.minecraftforge.common.MinecraftForge

class Proxy {
  def preInit(e: FMLPreInitializationEvent): Unit = {
    Config.load(e.getSuggestedConfigurationFile)

    LanguageRegistry.instance.loadLocalization(
      "/assets/opencomputers/lang/en_US.lang", "en_US", false)

    Driver.registry = Some(driver.Registry)
    Network.network = Some(network.Network)
  }

  def init(e: FMLInitializationEvent): Unit = {
    Blocks.init()
    Items.init()

    NetworkRegistry.instance.registerGuiHandler(OpenComputers, GuiHandler)

    Driver.add(driver.GraphicsCard)
    Driver.add(driver.Keyboard)

    MinecraftForge.EVENT_BUS.register(Computer)
    MinecraftForge.EVENT_BUS.register(network.Network)
  }

  def postInit(e: FMLPostInitializationEvent): Unit = {
    // Lock the driver registry to avoid drivers being added after computers
    // may have already started up. This makes sure the driver API won't change
    // over the course of a game, since that could lead to weird effects.
    Registry.locked = true
  }
}