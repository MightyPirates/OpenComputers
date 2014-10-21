package li.cil.oc.integration.opencomputers

import cpw.mods.fml.common.FMLCommonHandler
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.common.Achievement
import li.cil.oc.common.EventHandler
import li.cil.oc.common.Loot
import li.cil.oc.common.SaveHandler
import li.cil.oc.common.asm.SimpleComponentTickHandler
import li.cil.oc.common.event._
import li.cil.oc.common.item.Tablet
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.common.template.RobotTemplate
import li.cil.oc.common.template.TabletTemplate
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.server.network.WirelessNetwork
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.MinecraftForge

object ModOpenComputers extends ModProxy {
  override def getMod = Mods.OpenComputers

  override def initialize() {
    RobotTemplate.register()
    TabletTemplate.register()

    Loot.init()
    Recipes.init()
    Achievement.init()

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

    api.Driver.add(DriverBlockEnvironments)

    api.Driver.add(DriverComponentBus)
    api.Driver.add(DriverCPU)
    api.Driver.add(DriverDebugCard)
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
  }
}
