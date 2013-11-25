package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc._
import li.cil.oc.server.component.Keyboard
import li.cil.oc.server.driver
import li.cil.oc.server.fs
import li.cil.oc.server.network
import li.cil.oc.util.WirelessNetwork
import net.minecraftforge.common.MinecraftForge

class Proxy {
  def preInit(e: FMLPreInitializationEvent): Unit = {
    Settings.load(e.getSuggestedConfigurationFile)

    Blocks.init()
    Items.init()

    api.Driver.instance = driver.Registry
    api.FileSystem.instance = fs.FileSystem
    api.Network.instance = network.Network
  }

  def init(e: FMLInitializationEvent): Unit = {
    api.Driver.add(driver.block.Carriage)
    api.Driver.add(driver.block.CommandBlock)

    api.Driver.add(driver.item.FileSystem)
    api.Driver.add(driver.item.GraphicsCard)
    api.Driver.add(driver.item.Memory)
    api.Driver.add(driver.item.NetworkCard)
    api.Driver.add(driver.item.PowerSupply)
    api.Driver.add(driver.item.RedstoneCard)
    api.Driver.add(driver.item.WirelessNetworkCard)

    Recipes.init()
  }

  def postInit(e: FMLPostInitializationEvent): Unit = {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true

    GameRegistry.registerPlayerTracker(Keyboard)
    MinecraftForge.EVENT_BUS.register(WirelessNetwork)
  }
}