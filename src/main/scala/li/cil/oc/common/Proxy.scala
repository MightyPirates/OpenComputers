package li.cil.oc.common

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc._
import li.cil.oc.common.asm.SimpleComponentTickHandler
import li.cil.oc.common.event._
import li.cil.oc.common.init.Blocks
import li.cil.oc.common.init.Items
import li.cil.oc.common.item.Tablet
import li.cil.oc.common.multipart.MultiPart
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.common.template.RobotTemplate
import li.cil.oc.common.template.TabletTemplate
import li.cil.oc.integration.Mods
import li.cil.oc.integration.appeng.ModAppEng
import li.cil.oc.integration.buildcraft.ModBuildCraft
import li.cil.oc.integration.cofh.energy.ModCoFHEnergy
import li.cil.oc.integration.cofh.tileentity.ModCoFHTileEntity
import li.cil.oc.integration.cofh.transport.ModCoFHTransport
import li.cil.oc.integration.computercraft.ModComputerCraft
import li.cil.oc.integration.enderio.ModEnderIO
import li.cil.oc.integration.enderstorage.ModEnderStorage
import li.cil.oc.integration.forestry.ModForestry
import li.cil.oc.integration.gregtech.ModGregtech
import li.cil.oc.integration.ic2.ModIndustrialCraft2
import li.cil.oc.integration.mystcraft.ModMystcraft
import li.cil.oc.integration.opencomputers.ModOpenComputers
import li.cil.oc.integration.railcraft.ModRailcraft
import li.cil.oc.integration.thaumcraft.ModThaumcraft
import li.cil.oc.integration.thermalexpansion.ModThermalExpansion
import li.cil.oc.integration.tmechworks.ModTMechworks
import li.cil.oc.integration.util.ComputerCraft
import li.cil.oc.integration.vanilla.ModVanilla
import li.cil.oc.server._
import li.cil.oc.server.machine
import li.cil.oc.server.machine.luac.NativeLuaArchitecture
import li.cil.oc.server.machine.luaj.LuaJLuaArchitecture
import li.cil.oc.server.network.WirelessNetwork
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.util.UpdateCheck
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._
import scala.concurrent.ExecutionContext.Implicits.global

class Proxy {
  def preInit(e: FMLPreInitializationEvent) {
    Settings.load(e.getSuggestedConfigurationFile)

    OpenComputers.log.info("Initializing blocks and items.")

    Blocks.init()
    Items.init()

    OpenComputers.log.info("Initializing additional OreDict entries.")
    registerExclusive("craftingPiston", new ItemStack(net.minecraft.init.Blocks.piston), new ItemStack(net.minecraft.init.Blocks.sticky_piston))
    registerExclusive("torchRedstoneActive", new ItemStack(net.minecraft.init.Blocks.redstone_torch))
    registerExclusive("nuggetGold", new ItemStack(net.minecraft.init.Items.gold_nugget))
    registerExclusive("nuggetIron", Items.ironNugget.createItemStack())

    if (OreDictionary.getOres("nuggetIron").exists(Items.ironNugget.createItemStack().isItemEqual)) {
      Recipes.addItem(Items.ironNugget, "nuggetIron")
      Recipes.addItem(net.minecraft.init.Items.iron_ingot, "ingotIron")
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
    api.Machine.add(api.Machine.LuaArchitecture)
    api.Network.instance = network.Network

    if (Mods.ForgeMultipart.isAvailable) {
      OpenComputers.log.info("Initializing Forge MultiPart support.")
      MultiPart.init()
    }
    if (Mods.ComputerCraft.isAvailable) {
      OpenComputers.log.info("Initializing ComputerCraft support.")
      ComputerCraft.init()
    }
  }

  def init(e: FMLInitializationEvent) {
    OpenComputers.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel("OpenComputers")
    OpenComputers.channel.register(server.PacketHandler)

    OpenComputers.log.info("Initializing OpenComputers drivers.")
    Mods.integrate(ModAppEng)
    Mods.integrate(ModBuildCraft)
    Mods.integrate(ModCoFHEnergy)
    Mods.integrate(ModCoFHTileEntity)
    Mods.integrate(ModCoFHTransport)
    Mods.integrate(ModEnderIO)
    Mods.integrate(ModEnderStorage)
    Mods.integrate(ModForestry)
    Mods.integrate(ModGregtech)
    Mods.integrate(ModIndustrialCraft2)
    Mods.integrate(ModMystcraft)
    Mods.integrate(ModOpenComputers)
    Mods.integrate(ModRailcraft)
    Mods.integrate(ModThaumcraft)
    Mods.integrate(ModThermalExpansion)
    Mods.integrate(ModTMechworks)
    Mods.integrate(ModVanilla)

    // Register the general IPeripheral driver last, if at all, to avoid it
    // being used rather than other more concrete implementations, such as
    // is the case in the Redstone in Motion driver (replaces 'move').
    Mods.integrate(ModComputerCraft)

    OpenComputers.log.info("Initializing assembler templates.")
    RobotTemplate.register()
    TabletTemplate.register()

    OpenComputers.log.info("Initializing loot disks.")
    Loot.init()

    OpenComputers.log.info("Initializing recipes.")
    Recipes.init()

    OpenComputers.log.info("Initializing event handlers.")

    ForgeChunkManager.setForcedChunkLoadingCallback(OpenComputers, ChunkloaderUpgradeHandler)

    FMLCommonHandler.instance.bus.register(EventHandler)
    FMLCommonHandler.instance.bus.register(SimpleComponentTickHandler.Instance)
    FMLCommonHandler.instance.bus.register(Tablet)

    MinecraftForge.EVENT_BUS.register(AngelUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(ChunkloaderUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(EventHandler)
    MinecraftForge.EVENT_BUS.register(ExperienceUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(Loot)
    MinecraftForge.EVENT_BUS.register(RobotCommonHandler)
    MinecraftForge.EVENT_BUS.register(SaveHandler)
    MinecraftForge.EVENT_BUS.register(Tablet)
    MinecraftForge.EVENT_BUS.register(WirelessNetwork)
    MinecraftForge.EVENT_BUS.register(WirelessNetworkCardHandler)

    if (Mods.CoFHEnergy.isAvailable) {
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
    if (Mods.VersionChecker.isAvailable) {
      UpdateCheck.info onSuccess {
        case Some(release) =>
          val nbt = new NBTTagCompound()
          nbt.setString("newVersion", release.tag_name)
          nbt.setString("updateUrl", "https://github.com/MightyPirates/OpenComputers/releases")
          nbt.setBoolean("isDirectLink", false)
          if (release.body != null) {
            nbt.setString("changeLog", release.body.replaceAll("\r\n", "\n"))
          }
          FMLInterModComms.sendRuntimeMessage(OpenComputers.ID, Mods.IDs.VersionChecker, "addUpdate", nbt)
      }
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

  // Yes, this could be boiled down even further, but I like to keep it
  // explicit like this, because it makes it a) clearer, b) easier to
  // extend, in case that should ever be needed.

  private val blockRenames = Map(
    OpenComputers.ID + ":" + Settings.namespace + "simple" -> "simple",
    OpenComputers.ID + ":" + Settings.namespace + "simple_redstone" -> "simple_redstone",
    OpenComputers.ID + ":" + Settings.namespace + "special" -> "special",
    OpenComputers.ID + ":" + Settings.namespace + "special_redstone" -> "special_redstone",
    OpenComputers.ID + ":" + Settings.namespace + "keyboard" -> "keyboard",
    OpenComputers.ID + ":rack" -> "serverRack"
  )

  private val itemRenames = Map(
    OpenComputers.ID + ":" + Settings.namespace + "item" -> "item",
    OpenComputers.ID + ":" + Settings.namespace + "simple" -> "simple",
    OpenComputers.ID + ":" + Settings.namespace + "simple_redstone" -> "simple_redstone",
    OpenComputers.ID + ":" + Settings.namespace + "special" -> "special",
    OpenComputers.ID + ":" + Settings.namespace + "special_redstone" -> "special_redstone",
    OpenComputers.ID + ":" + Settings.namespace + "keyboard" -> "keyboard",
    OpenComputers.ID + ":rack" -> "serverRack"
  )

  def missingMappings(e: FMLMissingMappingsEvent) {
    for (missing <- e.get()) {
      if (missing.`type` == GameRegistry.Type.BLOCK) {
        blockRenames.get(missing.name) match {
          case Some(name) => missing.remap(GameRegistry.findBlock(OpenComputers.ID, name))
          case _ => missing.fail()
        }
      }
      else if (missing.`type` == GameRegistry.Type.ITEM) {
        itemRenames.get(missing.name) match {
          case Some(name) => missing.remap(GameRegistry.findItem(OpenComputers.ID, name))
          case _ => missing.fail()
        }
      }
      else missing.fail()
    }
  }
}