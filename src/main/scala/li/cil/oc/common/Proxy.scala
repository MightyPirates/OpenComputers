package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.{TickRegistry, GameRegistry}
import cpw.mods.fml.relauncher.Side
import java.util.concurrent.Callable
import li.cil.oc._
import li.cil.oc.api.FileSystem
import li.cil.oc.common.asm.SimpleComponentTickHandler
import li.cil.oc.common.multipart.MultiPart
import li.cil.oc.server.component.Keyboard
import li.cil.oc.server.component.machine
import li.cil.oc.server.component.machine.{LuaJLuaArchitecture, NativeLuaArchitecture}
import li.cil.oc.server.network.WirelessNetwork
import li.cil.oc.server.{TickHandler, driver, fs, network}
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.util.mods.{Mods, ComputerCraft16}
import net.minecraftforge.common.MinecraftForge

class Proxy {
  def preInit(e: FMLPreInitializationEvent) {
    Settings.load(e.getSuggestedConfigurationFile)

    Blocks.init()
    Items.init()

    if (Mods.ForgeMultipart.isAvailable) {
      MultiPart.init()
    }
    if (Mods.ComputerCraft16.isAvailable) {
      ComputerCraft16.init()
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
    api.Driver.add(driver.item.UpgradeBlockPlacerAir)
    api.Driver.add(driver.item.WirelessNetworkCard)

    if (Mods.ComputerCraft15.isAvailable) {
      api.Driver.add(driver.item.CC15Media)
    }
    if (Mods.ComputerCraft16.isAvailable) {
      api.Driver.add(driver.item.CC16Media)
    }

    api.Driver.add(driver.converter.FluidTankInfo)
    api.Driver.add(driver.converter.ItemStack)

    Recipes.init()
    GameRegistry.registerCraftingHandler(CraftingHandler)

    Loot.init()

    FMLInterModComms.sendMessage("Waila", "register", "li.cil.oc.util.mods.Waila.init")
  }

  def postInit(e: FMLPostInitializationEvent) {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true

    TickRegistry.registerTickHandler(TickHandler, Side.SERVER)
    TickRegistry.registerTickHandler(SimpleComponentTickHandler.Instance, Side.SERVER)
    GameRegistry.registerPlayerTracker(Keyboard)
    NetworkRegistry.instance.registerConnectionHandler(ConnectionHandler)
    MinecraftForge.EVENT_BUS.register(WirelessNetwork)
    MinecraftForge.EVENT_BUS.register(SaveHandler)
  }
}