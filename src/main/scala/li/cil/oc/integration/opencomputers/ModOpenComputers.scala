package li.cil.oc.integration.opencomputers

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.common.EventHandler
import li.cil.oc.common.Loot
import li.cil.oc.common.SaveHandler
import li.cil.oc.common.asm.SimpleComponentTickHandler
import li.cil.oc.common.event._
import li.cil.oc.common.item.Analyzer
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.RedstoneCard
import li.cil.oc.common.item.Tablet
import li.cil.oc.common.template._
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
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
    NavigationUpgradeTemplate.register()
    RobotTemplate.register()
    ServerTemplate.register()
    TabletTemplate.register()
    TemplateBlacklist.register()

    ForgeChunkManager.setForcedChunkLoadingCallback(OpenComputers, ChunkloaderUpgradeHandler)

    FMLCommonHandler.instance.bus.register(EventHandler)
    FMLCommonHandler.instance.bus.register(SimpleComponentTickHandler.Instance)
    FMLCommonHandler.instance.bus.register(Tablet)

    MinecraftForge.EVENT_BUS.register(Analyzer)
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
    api.Driver.add(DriverUpgradeLeash)
    api.Driver.add(DriverUpgradeNavigation)
    api.Driver.add(DriverUpgradePiston)
    api.Driver.add(DriverUpgradeSign)
    api.Driver.add(DriverUpgradeSolarGenerator)
    api.Driver.add(DriverUpgradeTank)
    api.Driver.add(DriverUpgradeTankController)
    api.Driver.add(DriverUpgradeTractorBeam)

    blacklistHost(classOf[internal.Adapter],
      Constants.BlockName.Geolyzer,
      Constants.BlockName.Keyboard,
      Constants.BlockName.ScreenTier1,
      Constants.ItemName.AngelUpgrade,
      Constants.ItemName.BatteryUpgradeTier1,
      Constants.ItemName.BatteryUpgradeTier2,
      Constants.ItemName.BatteryUpgradeTier3,
      Constants.ItemName.ChunkloaderUpgrade,
      Constants.ItemName.CraftingUpgrade,
      Constants.ItemName.ExperienceUpgrade,
      Constants.ItemName.GeneratorUpgrade,
      Constants.ItemName.InventoryUpgrade,
      Constants.ItemName.NavigationUpgrade,
      Constants.ItemName.PistonUpgrade,
      Constants.ItemName.SolarGeneratorUpgrade,
      Constants.ItemName.TankUpgrade,
      Constants.ItemName.TractorBeamUpgrade,
      Constants.ItemName.LeashUpgrade)
    blacklistHost(classOf[internal.Drone],
      Constants.ItemName.GraphicsCardTier1,
      Constants.ItemName.GraphicsCardTier2,
      Constants.ItemName.GraphicsCardTier3,
      Constants.BlockName.Keyboard,
      Constants.ItemName.NetworkCard,
      Constants.ItemName.RedstoneCardTier1,
      Constants.BlockName.ScreenTier1,
      Constants.ItemName.AngelUpgrade,
      Constants.ItemName.CraftingUpgrade,
      Constants.ItemName.ExperienceUpgrade)
    blacklistHost(classOf[internal.Microcontroller],
      Constants.ItemName.GraphicsCardTier1,
      Constants.ItemName.GraphicsCardTier2,
      Constants.ItemName.GraphicsCardTier3,
      Constants.BlockName.Keyboard,
      Constants.BlockName.ScreenTier1,
      Constants.ItemName.AngelUpgrade,
      Constants.ItemName.ChunkloaderUpgrade,
      Constants.ItemName.CraftingUpgrade,
      Constants.ItemName.DatabaseUpgradeTier1,
      Constants.ItemName.DatabaseUpgradeTier2,
      Constants.ItemName.DatabaseUpgradeTier3,
      Constants.ItemName.ExperienceUpgrade,
      Constants.ItemName.GeneratorUpgrade,
      Constants.ItemName.InventoryUpgrade,
      Constants.ItemName.InventoryControllerUpgrade,
      Constants.ItemName.NavigationUpgrade,
      Constants.ItemName.TankUpgrade,
      Constants.ItemName.TankControllerUpgrade,
      Constants.ItemName.TractorBeamUpgrade,
      Constants.ItemName.LeashUpgrade)
    blacklistHost(classOf[internal.Robot],
      Constants.ItemName.LeashUpgrade)
    blacklistHost(classOf[internal.Tablet],
      Constants.ItemName.NetworkCard,
      Constants.ItemName.RedstoneCardTier1,
      Constants.BlockName.ScreenTier1,
      Constants.ItemName.AngelUpgrade,
      Constants.ItemName.ChunkloaderUpgrade,
      Constants.ItemName.CraftingUpgrade,
      Constants.ItemName.DatabaseUpgradeTier1,
      Constants.ItemName.DatabaseUpgradeTier2,
      Constants.ItemName.DatabaseUpgradeTier3,
      Constants.ItemName.ExperienceUpgrade,
      Constants.ItemName.GeneratorUpgrade,
      Constants.ItemName.InventoryUpgrade,
      Constants.ItemName.InventoryControllerUpgrade,
      Constants.ItemName.TankUpgrade,
      Constants.ItemName.TankControllerUpgrade,
      Constants.ItemName.LeashUpgrade)

    if (!WirelessRedstone.isAvailable) {
      blacklistHost(classOf[internal.Drone], Constants.ItemName.RedstoneCardTier2)
      blacklistHost(classOf[internal.Tablet], Constants.ItemName.RedstoneCardTier2)
    }

    // Note: kinda nasty, but we have to check for availability for extended
    // redstone mods after integration init, so we have to set tier two
    // redstone card availability here, after all other mods were inited.
    if (BundledRedstone.isAvailable || WirelessRedstone.isAvailable) {
      OpenComputers.log.info("Found extended redstone mods, enabling tier two redstone card.")
      Delegator.subItem(api.Items.get(Constants.ItemName.RedstoneCardTier2).createItemStack(1)) match {
        case Some(redstone: RedstoneCard) => redstone.showInItemList = true
        case _ =>
      }
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
