package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.{GameRegistry, TickRegistry}
import cpw.mods.fml.relauncher.Side
import li.cil.oc._
import li.cil.oc.common.asm.SimpleComponentTickHandler
import li.cil.oc.common.event._
import li.cil.oc.common.item.Tablet
import li.cil.oc.common.multipart.MultiPart
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.server._
import li.cil.oc.server.component.machine.{LuaJLuaArchitecture, NativeLuaArchitecture}
import li.cil.oc.server.component.{Keyboard, machine}
import li.cil.oc.server.network.WirelessNetwork
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.util.mods.{ComputerCraft16, Mods}
import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.common.{ForgeChunkManager, MinecraftForge}
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._

class Proxy {
  def preInit(e: FMLPreInitializationEvent) {
    Settings.load(e.getSuggestedConfigurationFile)

    OpenComputers.log.info("Initializing blocks and items.")

    Blocks.init()
    Items.init()

    OpenComputers.log.info("Initializing additional OreDict entries.")

    registerExclusive("craftingPiston", new ItemStack(Block.pistonBase), new ItemStack(Block.pistonStickyBase))
    registerExclusive("torchRedstoneActive", new ItemStack(Block.torchRedstoneActive, 1, 0))
    registerExclusive("nuggetGold", new ItemStack(Item.goldNugget))
    registerExclusive("nuggetIron", Items.ironNugget.createItemStack())

    if (OreDictionary.getOres("nuggetIron").exists(Items.ironNugget.createItemStack().isItemEqual)) {
      Recipes.addItem(Items.ironNugget, "nuggetIron")
      Recipes.addItem(Item.ingotIron, "ingotIron")
    }

    OpenComputers.log.info("Initializing OpenComputers API.")

    api.CreativeTab.instance = CreativeTab
    api.Driver.instance = driver.Registry
    api.FileSystem.instance = fs.FileSystem
    api.Items.instance = Items
    api.Machine.instance = machine.Machine
    api.Machine.LuaArchitecture =
      if (LuaStateFactory.isAvailable && !Settings.get.forceLuaJ) classOf[NativeLuaArchitecture]
      else classOf[LuaJLuaArchitecture]
    api.Network.instance = network.Network

    if (Mods.ForgeMultipart.isAvailable) {
      OpenComputers.log.info("Initializing Forge MultiPart support.")
      MultiPart.init()
    }
    if (Mods.ComputerCraft16.isAvailable) {
      OpenComputers.log.info("Initializing ComputerCraft support.")
      ComputerCraft16.init()
    }
  }

  def init(e: FMLInitializationEvent) {
    OpenComputers.log.info("Initializing OpenComputers drivers.")
    api.Driver.add(driver.item.ComponentBus)
    api.Driver.add(driver.item.CPU)
    api.Driver.add(driver.item.DebugCard)
    api.Driver.add(driver.item.FileSystem)
    api.Driver.add(driver.item.GraphicsCard)
    api.Driver.add(driver.item.InternetCard)
    api.Driver.add(driver.item.LinkedCard)
    api.Driver.add(driver.item.Loot)
    api.Driver.add(driver.item.Memory)
    api.Driver.add(driver.item.NetworkCard)
    api.Driver.add(driver.item.Keyboard)
    api.Driver.add(driver.item.RedstoneCard)
    api.Driver.add(driver.item.Screen)
    api.Driver.add(driver.item.UpgradeAngel)
    api.Driver.add(driver.item.UpgradeBattery)
    api.Driver.add(driver.item.UpgradeChunkloader)
    api.Driver.add(driver.item.UpgradeContainerCard)
    api.Driver.add(driver.item.UpgradeContainerFloppy)
    api.Driver.add(driver.item.UpgradeContainerUpgrade)
    api.Driver.add(driver.item.UpgradeCrafting)
    api.Driver.add(driver.item.UpgradeExperience)
    api.Driver.add(driver.item.UpgradeGenerator)
    api.Driver.add(driver.item.UpgradeInventory)
    api.Driver.add(driver.item.UpgradeInventoryController)
    api.Driver.add(driver.item.UpgradeNavigation)
    api.Driver.add(driver.item.UpgradeSign)
    api.Driver.add(driver.item.UpgradeSolarGenerator)
    api.Driver.add(driver.item.UpgradeTractorBeam)
    api.Driver.add(driver.item.WirelessNetworkCard)

    if (Mods.StargateTech2.isAvailable) {
      OpenComputers.log.info("Initializing StargateTech2 converter and driver.")
      api.Driver.add(driver.converter.BusPacketNetScanDevice)
      api.Driver.add(driver.item.AbstractBusCard)
    }
    if (Mods.ComputerCraft15.isAvailable) {
      OpenComputers.log.info("Initializing ComputerCraft 1.5x floppy driver.")
      api.Driver.add(driver.item.CC15Media)
    }
    if (Mods.ComputerCraft16.isAvailable) {
      OpenComputers.log.info("Initializing ComputerCraft 1.6x floppy driver.")
      api.Driver.add(driver.item.CC16Media)
    }

    OpenComputers.log.info("Initializing vanilla converters.")
    api.Driver.add(driver.converter.FluidTankInfo)
    api.Driver.add(driver.converter.ItemStack)

    OpenComputers.log.info("Initializing loot disks.")
    Loot.init()

    OpenComputers.log.info("Initializing recipes.")
    Recipes.init()

    OpenComputers.log.info("Initializing event handlers.")

    GameRegistry.registerCraftingHandler(EventHandler)
    GameRegistry.registerPlayerTracker(Keyboard)

    ForgeChunkManager.setForcedChunkLoadingCallback(OpenComputers, ChunkloaderUpgradeHandler)

    MinecraftForge.EVENT_BUS.register(AngelUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(ChunkloaderUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(ExperienceUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(Loot)
    MinecraftForge.EVENT_BUS.register(RobotCommonHandler)
    MinecraftForge.EVENT_BUS.register(SaveHandler)
    MinecraftForge.EVENT_BUS.register(Tablet)
    MinecraftForge.EVENT_BUS.register(WirelessNetwork)
    MinecraftForge.EVENT_BUS.register(WirelessNetworkCardHandler)

    NetworkRegistry.instance.registerConnectionHandler(EventHandler)

    TickRegistry.registerTickHandler(EventHandler, Side.SERVER)
    TickRegistry.registerTickHandler(SimpleComponentTickHandler.Instance, Side.SERVER)
    TickRegistry.registerTickHandler(Tablet, Side.CLIENT)
    TickRegistry.registerTickHandler(Tablet, Side.SERVER)

    if (Mods.ThermalExpansion.isAvailable) {
      OpenComputers.log.info("Initializing Redstone Flux tool support.")
      MinecraftForge.EVENT_BUS.register(RedstoneFluxToolHandler)
    }
    if (Mods.TinkersConstruct.isAvailable) {
      OpenComputers.log.info("Initializing Tinker's Construct tool support.")
      MinecraftForge.EVENT_BUS.register(TinkersConstructToolHandler)
    }
    if (Mods.UniversalElectricity.isAvailable) {
      OpenComputers.log.info("Initializing electric tool support.")
      MinecraftForge.EVENT_BUS.register(UniversalElectricityToolHandler)
    }
    if (Mods.Waila.isAvailable) {
      OpenComputers.log.info("Initializing Waila support.")
      FMLInterModComms.sendMessage("Waila", "register", "li.cil.oc.util.mods.Waila.init")
    }
  }

  def postInit(e: FMLPostInitializationEvent) {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true
  }

  private def registerExclusive(name: String, items: ItemStack*) {
    if (OreDictionary.getOres(name).isEmpty) {
      for (item <- items) {
        OreDictionary.registerOre(name, item)
      }
    }
  }
}