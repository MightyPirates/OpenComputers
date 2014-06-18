package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.FMLCommonHandler
import java.util.concurrent.Callable
import li.cil.oc._
import li.cil.oc.api.FileSystem
import li.cil.oc.common.asm.SimpleComponentTickHandler
import li.cil.oc.common.multipart.MultiPart
import li.cil.oc.server
import li.cil.oc.server.component.machine
import li.cil.oc.server.component.machine.{LuaJLuaArchitecture, NativeLuaArchitecture}
import li.cil.oc.server.network.WirelessNetwork
import li.cil.oc.server.{driver, fs, network}
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.util.mods.{Mods, ComputerCraft}
import net.minecraftforge.common.MinecraftForge

class Proxy {
  def preInit(e: FMLPreInitializationEvent) {
    Settings.load(e.getSuggestedConfigurationFile)

    Blocks.init()
    Items.init()

    if (Mods.ForgeMultipart.isAvailable) {
      MultiPart.init()
    }
    if (Mods.ComputerCraft.isAvailable) {
      ComputerCraft.init()
    }

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
    api.Driver.add(driver.item.UpgradeAngel)
    api.Driver.add(driver.item.WirelessNetworkCard)

    if (Mods.ComputerCraft.isAvailable) {
      api.Driver.add(driver.item.ComputerCraftMedia)
    }
    if (Mods.StargateTech2.isAvailable) {
      api.Driver.add(server.driver.item.AbstractBusCard)
    }

    api.Driver.add(driver.converter.FluidTankInfo)
    api.Driver.add(driver.converter.ItemStack)
    if (Mods.StargateTech2.isAvailable) {
      api.Driver.add(server.driver.converter.BusPacketNetScanDevice)
    }

    Recipes.init()

    OpenComputers.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel("OpenComputers")
    OpenComputers.channel.register(server.PacketHandler)

    Loot.init()

    FMLInterModComms.sendMessage("Waila", "register", "li.cil.oc.util.mods.Waila.init")
  }

  def postInit(e: FMLPostInitializationEvent) {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true

    FMLCommonHandler.instance().bus().register(EventHandler)
    FMLCommonHandler.instance().bus().register(SimpleComponentTickHandler.Instance)
    MinecraftForge.EVENT_BUS.register(WirelessNetwork)
    MinecraftForge.EVENT_BUS.register(SaveHandler)
  }
}