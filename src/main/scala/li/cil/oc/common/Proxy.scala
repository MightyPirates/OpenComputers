package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.{TickRegistry, GameRegistry}
import cpw.mods.fml.relauncher.Side
import li.cil.oc._
import li.cil.oc.common.asm.SimpleComponentTickHandler
import li.cil.oc.common.multipart.MultiPart
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.server.component.{Keyboard, machine}
import li.cil.oc.server.component.machine.{LuaJLuaArchitecture, NativeLuaArchitecture}
import li.cil.oc.server.network.WirelessNetwork
import li.cil.oc.server.{TickHandler, driver, fs, network}
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.util.mods.{Mods, ComputerCraft16}
import net.minecraftforge.common.MinecraftForge
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.block.Block
import net.minecraftforge.oredict.OreDictionary
import scala.collection.convert.WrapAsScala._
import li.cil.oc.common.event.{ExperienceUpgradeHandler, UniversalElectricityToolHandler, RobotCommonHandler}

class Proxy {
  def preInit(e: FMLPreInitializationEvent) {
    Settings.load(e.getSuggestedConfigurationFile)

    Blocks.init()
    Items.init()

    registerExclusive("craftingPiston", new ItemStack(Block.pistonBase), new ItemStack(Block.pistonStickyBase))
    registerExclusive("torchRedstoneActive", new ItemStack(Block.torchRedstoneActive, 1, 0))
    registerExclusive("nuggetGold", new ItemStack(Item.goldNugget))
    registerExclusive("nuggetIron", Items.ironNugget.createItemStack())

    if (OreDictionary.getOres("nuggetIron").exists(Items.ironNugget.createItemStack().isItemEqual)) {
      Recipes.addItemDelegate(Items.ironNugget, "nuggetIron")
      Recipes.addItem(Item.ingotIron, "ingotIron")
    }

    if (Mods.ForgeMultipart.isAvailable) {
      MultiPart.init()
    }
    if (Mods.ComputerCraft16.isAvailable) {
      ComputerCraft16.init()
    }

    api.CreativeTab.instance = CreativeTab
    api.Driver.instance = driver.Registry
    api.FileSystem.instance = fs.FileSystem
    api.Items.instance = Items
    api.Machine.instance = machine.Machine
    api.Machine.LuaArchitecture =
      if (LuaStateFactory.isAvailable) classOf[NativeLuaArchitecture]
      else classOf[LuaJLuaArchitecture]
    api.Network.instance = network.Network
  }

  def init(e: FMLInitializationEvent) {
    api.Driver.add(driver.item.AbstractBusCard)
    api.Driver.add(driver.item.FileSystem)
    api.Driver.add(driver.item.GraphicsCard)
    api.Driver.add(driver.item.InternetCard)
    api.Driver.add(driver.item.LinkedCard)
    api.Driver.add(driver.item.Loot)
    api.Driver.add(driver.item.Memory)
    api.Driver.add(driver.item.NetworkCard)
    api.Driver.add(driver.item.Keyboard)
    api.Driver.add(driver.item.Processor)
    api.Driver.add(driver.item.RedstoneCard)
    api.Driver.add(driver.item.Screen)
    api.Driver.add(driver.item.UpgradeContainerCard)
    api.Driver.add(driver.item.UpgradeContainerFloppy)
    api.Driver.add(driver.item.UpgradeContainerUpgrade)
    api.Driver.add(driver.item.UpgradeCrafting)
    api.Driver.add(driver.item.UpgradeExperience)
    api.Driver.add(driver.item.UpgradeGenerator)
    api.Driver.add(driver.item.UpgradeInventory)
    api.Driver.add(driver.item.UpgradeNavigation)
    api.Driver.add(driver.item.UpgradeSign)
    api.Driver.add(driver.item.UpgradeSolarGenerator)
    api.Driver.add(driver.item.UpgradeAngel)
    api.Driver.add(driver.item.WirelessNetworkCard)

    if (Mods.ComputerCraft15.isAvailable) {
      api.Driver.add(driver.item.CC15Media)
    }
    if (Mods.ComputerCraft16.isAvailable) {
      api.Driver.add(driver.item.CC16Media)
    }

    api.Driver.add(driver.converter.FluidTankInfo)
    api.Driver.add(driver.converter.ItemStack)

    MinecraftForge.EVENT_BUS.register(RobotCommonHandler)
    MinecraftForge.EVENT_BUS.register(ExperienceUpgradeHandler)
    if (Mods.UniversalElectricity.isAvailable) {
      MinecraftForge.EVENT_BUS.register(UniversalElectricityToolHandler)
    }

    Loot.init()
    Recipes.init()
    GameRegistry.registerCraftingHandler(CraftingHandler)

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

  private def registerExclusive(name: String, items: ItemStack*) {
    if (OreDictionary.getOres(name).isEmpty) {
      for (item <- items) {
        OreDictionary.registerOre(name, item)
      }
    }
  }
}