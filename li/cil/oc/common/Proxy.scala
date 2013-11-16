package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc._
import li.cil.oc.server.component.Keyboard
import li.cil.oc.server.driver
import li.cil.oc.server.fs
import li.cil.oc.server.network

class Proxy {
  def preInit(e: FMLPreInitializationEvent): Unit = {
    Config.load(e.getSuggestedConfigurationFile)

    api.Driver.instance = driver.Registry
    api.FileSystem.instance = fs.FileSystem
    api.Network.instance = network.Network
  }

  def init(e: FMLInitializationEvent): Unit = {
    Blocks.init()
    Items.init()

    api.Driver.add(driver.block.Carriage)
    api.Driver.add(driver.block.CommandBlock)
    // api.Driver.add(driver.block.Peripheral) // Can cause severe issues (deadlocks).

    api.Driver.add(driver.item.FileSystem)
    api.Driver.add(driver.item.GraphicsCard)
    api.Driver.add(driver.item.Memory)
    api.Driver.add(driver.item.NetworkCard)
    api.Driver.add(driver.item.PowerSupply)
    api.Driver.add(driver.item.RedstoneCard)
    api.Driver.add(driver.item.WirelessNetworkCard)

    GameRegistry.registerPlayerTracker(Keyboard)
  }

  def postInit(e: FMLPostInitializationEvent): Unit = {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true
  }
}