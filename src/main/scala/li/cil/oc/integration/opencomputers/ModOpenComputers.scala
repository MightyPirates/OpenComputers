package li.cil.oc.integration.opencomputers

import li.cil.oc.api
import li.cil.oc.integration.IMod
import li.cil.oc.server.driver
import li.cil.oc.util.mods.Mods

object ModOpenComputers extends IMod {
  override def getMod = Mods.OpenComputers

  override def initialize() {
    api.Driver.add(driver.block.EnvironmentProvider)
    api.Driver.add(driver.item.ComponentBus)
    api.Driver.add(driver.item.CPU)
    api.Driver.add(driver.item.DebugCard)
    api.Driver.add(driver.item.FileSystem)
    api.Driver.add(driver.item.Geolyzer)
    api.Driver.add(driver.item.GraphicsCard)
    api.Driver.add(driver.item.InternetCard)
    api.Driver.add(driver.item.LinkedCard)
    api.Driver.add(driver.item.Loot)
    api.Driver.add(driver.item.Memory)
    api.Driver.add(driver.item.NetworkCard)
    api.Driver.add(driver.item.Keyboard)
    api.Driver.add(driver.item.RedstoneCard)
    api.Driver.add(driver.item.Screen)
    api.Driver.add(driver.item.Tablet)
    api.Driver.add(driver.item.UpgradeAngel)
    api.Driver.add(driver.item.UpgradeBattery)
    api.Driver.add(driver.item.UpgradeChunkloader)
    api.Driver.add(driver.item.ContainerCard)
    api.Driver.add(driver.item.ContainerFloppy)
    api.Driver.add(driver.item.ContainerUpgrade)
    api.Driver.add(driver.item.UpgradeCrafting)
    api.Driver.add(driver.item.UpgradeExperience)
    api.Driver.add(driver.item.UpgradeGenerator)
    api.Driver.add(driver.item.UpgradeInventory)
    api.Driver.add(driver.item.UpgradeInventoryController)
    api.Driver.add(driver.item.UpgradeNavigation)
    api.Driver.add(driver.item.UpgradePiston)
    api.Driver.add(driver.item.UpgradeSign)
    api.Driver.add(driver.item.UpgradeSolarGenerator)
    api.Driver.add(driver.item.UpgradeTank)
    api.Driver.add(driver.item.UpgradeTankController)
    api.Driver.add(driver.item.UpgradeTractorBeam)
    api.Driver.add(driver.item.WirelessNetworkCard)
  }
}
