package li.cil.oc.common

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import java.util.concurrent.Callable
import li.cil.oc._
import li.cil.oc.api.FileSystem
import li.cil.oc.common.asm.SimpleComponentTickHandler
import li.cil.oc.server
import li.cil.oc.server.component.machine
import li.cil.oc.server.component.machine.{LuaJLuaArchitecture, NativeLuaArchitecture}
import li.cil.oc.server.{driver, fs, network}
import li.cil.oc.util.LuaStateFactory
import net.minecraftforge.common.MinecraftForge
import li.cil.oc.server.network.WirelessNetwork

class Proxy {
  def preInit(e: FMLPreInitializationEvent) {
    Settings.load(e.getSuggestedConfigurationFile)

    Blocks.init()
    Items.init()
    /* TODO FMP
    if (Loader.isModLoaded("ForgeMultipart")) {
      MultiPart.init()
    }
    */

    api.CreativeTab.Instance = CreativeTab
    api.Driver.instance = driver.Registry
    api.FileSystem.instance = fs.FileSystem
    api.Machine.instance = machine.Machine
    api.Machine.LuaArchitecture =
      if (LuaStateFactory.isAvailable) classOf[NativeLuaArchitecture]
      else classOf[LuaJLuaArchitecture]
    api.Network.instance = network.Network

    api.Machine.addRomResource(api.Machine.LuaArchitecture,
      new Callable[api.fs.FileSystem] {
        def call = FileSystem.fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/rom")
      },
      Settings.resourceDomain + "/lua/rom")
  }

  def init(e: FMLInitializationEvent) {
    api.Driver.add(driver.item.AbstractBusCard)
    api.Driver.add(driver.item.FileSystem)
    api.Driver.add(driver.item.GraphicsCard)
    api.Driver.add(driver.item.InternetCard)
    api.Driver.add(driver.item.Loot)
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

    api.Driver.add(driver.converter.ItemStack)

    Recipes.init()

    OpenComputers.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel("OpenComputers")
    OpenComputers.channel.register(server.PacketHandler)

    Loot.init()
  }

  def postInit(e: FMLPostInitializationEvent) {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true

    FMLCommonHandler.instance().bus().register(EventHandler)
    FMLCommonHandler.instance().bus().register(SimpleComponentTickHandler.Instance)
    MinecraftForge.EVENT_BUS.register(WirelessNetwork)
  }
}