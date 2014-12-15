package li.cil.oc.integration.opencomputers

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.event.FMLInterModComms
import cpw.mods.fml.common.registry.EntityRegistry
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.common.EventHandler
import li.cil.oc.common.Loot
import li.cil.oc.common.SaveHandler
import li.cil.oc.common.asm.SimpleComponentTickHandler
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.event._
import li.cil.oc.common.item.Tablet
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.common.template.DroneTemplate
import li.cil.oc.common.template.MicrocontrollerTemplate
import li.cil.oc.common.template.RobotTemplate
import li.cil.oc.common.template.TabletTemplate
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.WirelessRedstone
import li.cil.oc.server.network.WirelessNetwork
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.MinecraftForge

object ModOpenComputers extends ModProxy {
  override def getMod = Mods.OpenComputers

  override def initialize() {
    DroneTemplate.register()
    MicrocontrollerTemplate.register()
    RobotTemplate.register()
    TabletTemplate.register()

    Loot.init()
    Recipes.init()

    EntityRegistry.registerModEntity(classOf[Drone], "Drone", 0, OpenComputers, 80, 1, true)

    ForgeChunkManager.setForcedChunkLoadingCallback(OpenComputers, ChunkloaderUpgradeHandler)

    FMLCommonHandler.instance.bus.register(EventHandler)
    FMLCommonHandler.instance.bus.register(SimpleComponentTickHandler.Instance)
    FMLCommonHandler.instance.bus.register(Tablet)

    MinecraftForge.EVENT_BUS.register(AngelUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(ChunkloaderUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(EventHandler)
    MinecraftForge.EVENT_BUS.register(ExperienceUpgradeHandler)
    MinecraftForge.EVENT_BUS.register(FileSystemAccessHandler)
    MinecraftForge.EVENT_BUS.register(GeolyzerHandler)
    MinecraftForge.EVENT_BUS.register(Loot)
    MinecraftForge.EVENT_BUS.register(RobotCommonHandler)
    MinecraftForge.EVENT_BUS.register(SaveHandler)
    MinecraftForge.EVENT_BUS.register(Tablet)
    MinecraftForge.EVENT_BUS.register(WirelessNetwork)
    MinecraftForge.EVENT_BUS.register(WirelessNetworkCardHandler)
    MinecraftForge.EVENT_BUS.register(li.cil.oc.client.ComponentTracker)
    MinecraftForge.EVENT_BUS.register(li.cil.oc.server.ComponentTracker)

    api.Driver.add(DriverBlockEnvironments)

    api.Driver.add(DriverComponentBus)
    api.Driver.add(DriverCPU)
    api.Driver.add(DriverDebugCard)
    api.Driver.add(DriverEEPROM)
    api.Driver.add(DriverFileSystem)
    api.Driver.add(DriverGeolyzer)
    api.Driver.add(DriverGraphicsCard)
    api.Driver.add(DriverInternetCard)
    api.Driver.add(DriverLinkedCard)
    api.Driver.add(DriverLootDisk)
    api.Driver.add(DriverMemory)
    api.Driver.add(DriverNetworkCard)
    api.Driver.add(DriverKeyboard)
    api.Driver.add(DriverRedstoneCard)
    api.Driver.add(DriverScreen)
    api.Driver.add(DriverTablet)
    api.Driver.add(DriverWirelessNetworkCard)

    api.Driver.add(DriverContainerCard)
    api.Driver.add(DriverContainerFloppy)
    api.Driver.add(DriverContainerUpgrade)

    api.Driver.add(DriverUpgradeAngel)
    api.Driver.add(DriverUpgradeBattery)
    api.Driver.add(DriverUpgradeChunkloader)
    api.Driver.add(DriverUpgradeCrafting)
    api.Driver.add(DriverUpgradeDatabase)
    api.Driver.add(DriverUpgradeExperience)
    api.Driver.add(DriverUpgradeGenerator)
    api.Driver.add(DriverUpgradeInventory)
    api.Driver.add(DriverUpgradeInventoryController)
    api.Driver.add(DriverUpgradeNavigation)
    api.Driver.add(DriverUpgradePiston)
    api.Driver.add(DriverUpgradeSign)
    api.Driver.add(DriverUpgradeSolarGenerator)
    api.Driver.add(DriverUpgradeTank)
    api.Driver.add(DriverUpgradeTankController)
    api.Driver.add(DriverUpgradeTractorBeam)

    blacklistHost(classOf[internal.Adapter], "geolyzer", "keyboard", "screen1", "angelUpgrade", "batteryUpgrade1", "batteryUpgrade2", "batteryUpgrade3", "chunkloaderUpgrade", "craftingUpgrade", "experienceUpgrade", "generatorUpgrade", "inventoryUpgrade", "navigationUpgrade", "pistonUpgrade", "solarGeneratorUpgrade", "tankUpgrade", "tractorBeamUpgrade")
    blacklistHost(classOf[internal.Microcontroller], "graphicsCard1", "graphicsCard2", "graphicsCard3", "keyboard", "screen1", "angelUpgrade", "chunkloaderUpgrade", "craftingUpgrade", "databaseUpgrade1", "databaseUpgrade2", "databaseUpgrade3", "experienceUpgrade", "generatorUpgrade", "inventoryUpgrade", "inventoryControllerUpgrade", "navigationUpgrade", "tankUpgrade", "tankControllerUpgrade", "tractorBeamUpgrade")
    blacklistHost(classOf[internal.Drone], "graphicsCard1", "graphicsCard2", "graphicsCard3", "keyboard", "lanCard", "redstoneCard1", "screen1", "angelUpgrade", "craftingUpgrade", "experienceUpgrade")
    blacklistHost(classOf[internal.Tablet], "lanCard", "redstoneCard1", "screen1", "angelUpgrade", "chunkloaderUpgrade", "craftingUpgrade", "databaseUpgrade1", "databaseUpgrade2", "databaseUpgrade3", "experienceUpgrade", "generatorUpgrade", "inventoryUpgrade", "inventoryControllerUpgrade", "tankUpgrade", "tankControllerUpgrade")

    if (!WirelessRedstone.isAvailable) {
      blacklistHost(classOf[internal.Drone], "redstoneCard2")
      blacklistHost(classOf[internal.Tablet], "redstoneCard2")
    }
  }

  private def blacklistHost(host: Class[_], itemNames: String*) {
    for (itemName <- itemNames) {
      val nbt = new NBTTagCompound()
      nbt.setString("name", itemName)
      nbt.setString("host", host.getName)
      nbt.setNewCompoundTag("item", api.Items.get(itemName).createItemStack(1).writeToNBT)
      FMLInterModComms.sendMessage("OpenComputers", "blacklistHost", nbt)
    }
  }
}
