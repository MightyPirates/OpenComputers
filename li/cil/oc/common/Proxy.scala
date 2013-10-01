package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.LanguageRegistry
import li.cil.oc._
import li.cil.oc.server.component.Computer
import li.cil.oc.server.driver
import li.cil.oc.server.fs
import li.cil.oc.server.network
import net.minecraftforge.common.MinecraftForge
import scala.Some

class Proxy {
  def preInit(e: FMLPreInitializationEvent): Unit = {
    Config.load(e.getSuggestedConfigurationFile)

    // Note: en_US is loaded automatically.
    // TODO Are others as well?
    LanguageRegistry.instance.loadLocalization(
      "/assets/" + Config.resourceDomain + "/lang/de_DE.lang", "de_DE", false)

    api.Driver.instance = Some(driver.Registry)
    api.FileSystem.instance = Some(fs.FileSystem)
    api.Network.instance = Some(network.Network)
  }

  def init(e: FMLInitializationEvent): Unit = {
    Blocks.init()
    Items.init()

    NetworkRegistry.instance.registerGuiHandler(OpenComputers, GuiHandler)

    api.Driver.add(driver.Disk)
    api.Driver.add(driver.GraphicsCard)
    api.Driver.add(driver.Keyboard)
    api.Driver.add(driver.Memory)
    api.Driver.add(driver.Redstone)

    MinecraftForge.EVENT_BUS.register(Computer)
    MinecraftForge.EVENT_BUS.register(network.Network)
  }

  def postInit(e: FMLPostInitializationEvent): Unit = {
    // Lock the driver registry to avoid drivers being added after computers
    // may have already started up. This makes sure the driver API won't change
    // over the course of a game, since that could lead to weird effects.
    driver.Registry.locked = true
  }
}