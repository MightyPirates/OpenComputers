package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import li.cil.oc._
import li.cil.oc.server
import li.cil.oc.server.component.Keyboard
import li.cil.oc.server.driver
import li.cil.oc.server.fs
import li.cil.oc.server.network
import li.cil.oc.server.network.Network
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
    api.Driver.add(driver.item.AbstractBusCard)
    api.Driver.add(driver.item.FileSystem)
    api.Driver.add(driver.item.GraphicsCard)
    api.Driver.add(driver.item.InternetCard)
    api.Driver.add(driver.item.Memory)
    api.Driver.add(driver.item.NetworkCard)
    api.Driver.add(driver.item.Processor)
    api.Driver.add(driver.item.RedstoneCard)
    api.Driver.add(driver.item.UpgradeCrafting)
    api.Driver.add(driver.item.UpgradeGenerator)
    api.Driver.add(driver.item.UpgradeNavigation)
    api.Driver.add(driver.item.UpgradeSign)
    api.Driver.add(driver.item.UpgradeSolarGenerator)
    api.Driver.add(driver.item.WirelessNetworkCard)

    Recipes.init()

    MinecraftForge.EVENT_BUS.register(CraftingHandler)
    OpenComputers.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel("OpenComputers")
    OpenComputers.channel.register(server.PacketHandler)
  }

  def postInit(e: FMLPostInitializationEvent): Unit = {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true

    MinecraftForge.EVENT_BUS.register(Keyboard)
    MinecraftForge.EVENT_BUS.register(ConnectionHandler)
    MinecraftForge.EVENT_BUS.register(Network)
    MinecraftForge.EVENT_BUS.register(WirelessNetwork)
  }
}