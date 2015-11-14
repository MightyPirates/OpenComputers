package li.cil.oc.client

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.util.IIcon
import net.minecraft.util.ResourceLocation

object Textures {
  val fontAntiAliased = new ResourceLocation(Settings.resourceDomain, "textures/font/chars.png")
  val fontAliased = new ResourceLocation(Settings.resourceDomain, "textures/font/chars_aliased.png")

  val guiBackground = new ResourceLocation(Settings.resourceDomain, "textures/gui/background.png")
  val guiBar = new ResourceLocation(Settings.resourceDomain, "textures/gui/bar.png")
  val guiBorders = new ResourceLocation(Settings.resourceDomain, "textures/gui/borders.png")
  val guiButtonDriveMode = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_drive_mode.png")
  val guiButtonPower = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_power.png")
  val guiButtonRange = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_range.png")
  val guiButtonRun = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_run.png")
  val guiButtonScroll = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_scroll.png")
  val guiButtonSide = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_side.png")
  val guiButtonRelay = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_switch.png")
  val guiComputer = new ResourceLocation(Settings.resourceDomain, "textures/gui/computer.png")
  val guiDatabase = new ResourceLocation(Settings.resourceDomain, "textures/gui/database.png")
  val guiDatabase1 = new ResourceLocation(Settings.resourceDomain, "textures/gui/database1.png")
  val guiDatabase2 = new ResourceLocation(Settings.resourceDomain, "textures/gui/database2.png")
  val guiDisassembler = new ResourceLocation(Settings.resourceDomain, "textures/gui/disassembler.png")
  val guiDrive = new ResourceLocation(Settings.resourceDomain, "textures/gui/drive.png")
  val guiDrone = new ResourceLocation(Settings.resourceDomain, "textures/gui/drone.png")
  val guiKeyboardMissing = new ResourceLocation(Settings.resourceDomain, "textures/gui/keyboard_missing.png")
  val guiManual = new ResourceLocation(Settings.resourceDomain, "textures/gui/manual.png")
  val guiManualHome = new ResourceLocation(Settings.resourceDomain, "textures/gui/manual_home.png")
  val guiManualMissingItem = new ResourceLocation(Settings.resourceDomain, "textures/gui/manual_missing_item.png")
  val guiManualTab = new ResourceLocation(Settings.resourceDomain, "textures/gui/manual_tab.png")
  val guiPrinter = new ResourceLocation(Settings.resourceDomain, "textures/gui/printer.png")
  val guiPrinterInk = new ResourceLocation(Settings.resourceDomain, "textures/gui/printer_ink.png")
  val guiPrinterMaterial = new ResourceLocation(Settings.resourceDomain, "textures/gui/printer_material.png")
  val guiPrinterProgress = new ResourceLocation(Settings.resourceDomain, "textures/gui/printer_progress.png")
  val guiRack = new ResourceLocation(Settings.resourceDomain, "textures/gui/rack.png")
  val guiRaid = new ResourceLocation(Settings.resourceDomain, "textures/gui/raid.png")
  val guiRange = new ResourceLocation(Settings.resourceDomain, "textures/gui/range.png")
  val guiRobot = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot.png")
  val guiRobotNoScreen = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot_noscreen.png")
  val guiRobotAssembler = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot_assembler.png")
  val guiRobotSelection = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot_selection.png")
  val guiServer = new ResourceLocation(Settings.resourceDomain, "textures/gui/server.png")
  val guiSlot = new ResourceLocation(Settings.resourceDomain, "textures/gui/slot.png")
  val guiUpgradeTab = new ResourceLocation(Settings.resourceDomain, "textures/gui/upgrade_tab.png")
  val guiWaypoint = new ResourceLocation(Settings.resourceDomain, "textures/gui/waypoint.png")

  val blockCaseFrontOn = new ResourceLocation(Settings.resourceDomain, "textures/blocks/CaseFrontOn.png")
  val blockCaseFrontError = new ResourceLocation(Settings.resourceDomain, "textures/blocks/CaseFrontError.png")
  val blockCaseFrontActivity = new ResourceLocation(Settings.resourceDomain, "textures/blocks/CaseFrontActivity.png")
  val blockDiskDriveFrontActivity = new ResourceLocation(Settings.resourceDomain, "textures/blocks/DiskDriveFrontActivity.png")
  val blockHologram = new ResourceLocation(Settings.resourceDomain, "textures/blocks/HologramEffect.png")
  val blockMicrocontrollerFrontLight = new ResourceLocation(Settings.resourceDomain, "textures/blocks/MicrocontrollerFrontLight.png")
  val blockMicrocontrollerFrontOn = new ResourceLocation(Settings.resourceDomain, "textures/blocks/MicrocontrollerFrontOn.png")
  val blockMicrocontrollerFrontError = new ResourceLocation(Settings.resourceDomain, "textures/blocks/MicrocontrollerFrontError.png")
  val blockRaidFrontError = new ResourceLocation(Settings.resourceDomain, "textures/blocks/RaidFrontError.png")
  val blockRaidFrontActivity = new ResourceLocation(Settings.resourceDomain, "textures/blocks/RaidFrontActivity.png")
  val blockRobot = new ResourceLocation(Settings.resourceDomain, "textures/blocks/robot.png")
  val blockScreenUpIndicator = new ResourceLocation(Settings.resourceDomain, "textures/blocks/screen/up_indicator.png")
  val blockServerFrontOn = new ResourceLocation(Settings.resourceDomain, "textures/blocks/ServerFrontOn.png")
  val blockServerFrontError = new ResourceLocation(Settings.resourceDomain, "textures/blocks/ServerFrontError.png")
  val blockServerFrontActivity = new ResourceLocation(Settings.resourceDomain, "textures/blocks/ServerFrontActivity.png")
  val blockTerminalServerFrontOn = new ResourceLocation(Settings.resourceDomain, "textures/blocks/TerminalServerFrontOn.png")
  val blockTerminalServerFrontPresence = new ResourceLocation(Settings.resourceDomain, "textures/blocks/TerminalServerFrontPresence.png")

  val upgradeCrafting = new ResourceLocation(Settings.resourceDomain, "textures/model/UpgradeCrafting.png")
  val upgradeGenerator = new ResourceLocation(Settings.resourceDomain, "textures/model/UpgradeGenerator.png")
  val upgradeInventory = new ResourceLocation(Settings.resourceDomain, "textures/model/UpgradeInventory.png")

  val overlayNanomachines = new ResourceLocation(Settings.resourceDomain, "textures/gui/nanomachines_power.png")
  val overlayNanomachinesBar = new ResourceLocation(Settings.resourceDomain, "textures/gui/nanomachines_power_bar.png")

  object Cable {
    var iconCap: IIcon = _
  }

  object Charger {
    var iconFrontCharging: IIcon = _
    var iconSideCharging: IIcon = _
  }

  object Disassembler {
    var iconSideOn: IIcon = _
    var iconTopOn: IIcon = _
  }

  object Geolyzer {
    var iconTopOn: IIcon = _
  }

  object PowerDistributor {
    var iconSideOn: IIcon = _
    var iconTopOn: IIcon = _
  }

  object Rack {
    val icons = Array.fill[IIcon](6)(null)
    var server: IIcon = _
    var terminal: IIcon = _
  }

  object Assembler {
    var iconSideAssembling: IIcon = _
    var iconSideOn: IIcon = _
    var iconTopOn: IIcon = _
  }

  object Switch {
    var iconSideActivity: IIcon = _
  }

  object NetSplitter {
    var iconOn: IIcon = _
  }

  object Transposer {
    var iconOn: IIcon = _
  }

  def init(tm: TextureManager) {
    tm.bindTexture(fontAntiAliased)
    tm.bindTexture(fontAliased)

    tm.bindTexture(guiBackground)
    tm.bindTexture(guiBar)
    tm.bindTexture(guiBorders)
    tm.bindTexture(guiButtonPower)
    tm.bindTexture(guiButtonRange)
    tm.bindTexture(guiButtonRun)
    tm.bindTexture(guiButtonSide)
    tm.bindTexture(guiComputer)
    tm.bindTexture(guiDrone)
    tm.bindTexture(guiKeyboardMissing)
    tm.bindTexture(guiRaid)
    tm.bindTexture(guiRange)
    tm.bindTexture(guiRobot)
    tm.bindTexture(guiRobotAssembler)
    tm.bindTexture(guiRobotSelection)
    tm.bindTexture(guiServer)
    tm.bindTexture(guiSlot)

    tm.bindTexture(blockCaseFrontOn)
    tm.bindTexture(blockCaseFrontActivity)
    tm.bindTexture(blockHologram)
    tm.bindTexture(blockMicrocontrollerFrontLight)
    tm.bindTexture(blockMicrocontrollerFrontOn)
    tm.bindTexture(blockServerFrontOn)
    tm.bindTexture(blockRobot)
    tm.bindTexture(blockScreenUpIndicator)

    tm.bindTexture(upgradeCrafting)
    tm.bindTexture(upgradeGenerator)
    tm.bindTexture(upgradeInventory)
  }
}
