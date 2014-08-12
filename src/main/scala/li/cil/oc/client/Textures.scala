package li.cil.oc.client

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.util.{IIcon, ResourceLocation}

object Textures {
  val fontAntiAliased = new ResourceLocation(Settings.resourceDomain, "textures/font/chars.png")
  val fontAliased = new ResourceLocation(Settings.resourceDomain, "textures/font/chars_aliased.png")

  val guiBackground = new ResourceLocation(Settings.resourceDomain, "textures/gui/background.png")
  val guiBar = new ResourceLocation(Settings.resourceDomain, "textures/gui/bar.png")
  val guiBorders = new ResourceLocation(Settings.resourceDomain, "textures/gui/borders.png")
  val guiButtonPower = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_power.png")
  val guiButtonRange = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_range.png")
  val guiButtonRun = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_run.png")
  val guiButtonScroll = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_scroll.png")
  val guiButtonSide = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_side.png")
  val guiButtonSwitch = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_switch.png")
  val guiComputer = new ResourceLocation(Settings.resourceDomain, "textures/gui/computer.png")
  val guiDisassembler = new ResourceLocation(Settings.resourceDomain, "textures/gui/disassembler.png")
  val guiKeyboardMissing = new ResourceLocation(Settings.resourceDomain, "textures/gui/keyboard_missing.png")
  val guiRange = new ResourceLocation(Settings.resourceDomain, "textures/gui/range.png")
  val guiRobot = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot.png")
  val guiRobotNoScreen = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot_noscreen.png")
  val guiRobotAssembler = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot_assembler.png")
  val guiRobotSelection = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot_selection.png")
  val guiServer = new ResourceLocation(Settings.resourceDomain, "textures/gui/server.png")
  val guiSlot = new ResourceLocation(Settings.resourceDomain, "textures/gui/slot.png")

  val blockCaseFrontOn = new ResourceLocation(Settings.resourceDomain, "textures/blocks/CaseFrontOn.png")
  val blockHologram = new ResourceLocation(Settings.resourceDomain, "textures/blocks/HologramEffect.png")
  val blockRackFrontOn = new ResourceLocation(Settings.resourceDomain, "textures/blocks/ServerRackFrontOn.png")
  val blockRobot = new ResourceLocation(Settings.resourceDomain, "textures/blocks/robot.png")
  val blockScreenUpIndicator = new ResourceLocation(Settings.resourceDomain, "textures/blocks/screen/up_indicator.png")

  val upgradeCrafting = new ResourceLocation(Settings.resourceDomain, "textures/model/UpgradeCrafting.png")
  val upgradeGenerator = new ResourceLocation(Settings.resourceDomain, "textures/model/UpgradeGenerator.png")
  val upgradeInventory = new ResourceLocation(Settings.resourceDomain, "textures/model/UpgradeInventory.png")

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

  object ServerRack {
    val icons = Array.fill[IIcon](6)(null)
  }

  object RobotAssembler {
    var iconSideAssembling: IIcon = _
    var iconSideOn: IIcon = _
    var iconTopOn: IIcon = _
  }

  object Switch {
    var iconSideActivity: IIcon = _
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
    tm.bindTexture(guiRange)
    tm.bindTexture(guiRobot)
    tm.bindTexture(guiRobotAssembler)
    tm.bindTexture(guiRobotSelection)
    tm.bindTexture(guiServer)
    tm.bindTexture(guiSlot)

    tm.bindTexture(blockCaseFrontOn)
    tm.bindTexture(blockHologram)
    tm.bindTexture(blockRackFrontOn)
    tm.bindTexture(blockRobot)
    tm.bindTexture(blockScreenUpIndicator)

    tm.bindTexture(upgradeCrafting)
    tm.bindTexture(upgradeGenerator)
  }
}
