package li.cil.oc.client

import li.cil.oc.Settings
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.event.ForgeSubscribe

object TexturePreloader {
  val fontAntiAliased = new ResourceLocation(Settings.resourceDomain, "textures/font/chars.png")
  val fontAliased = new ResourceLocation(Settings.resourceDomain, "textures/font/chars_aliased.png")

  val guiBackground = new ResourceLocation(Settings.resourceDomain, "textures/gui/background.png")
  val guiBorders = new ResourceLocation(Settings.resourceDomain, "textures/gui/borders.png")
  val guiButtonPower = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_power.png")
  val guiButtonRange = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_range.png")
  val guiButtonSide = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_side.png")
  val guiComputer = new ResourceLocation(Settings.resourceDomain, "textures/gui/computer.png")
  val guiRange = new ResourceLocation(Settings.resourceDomain, "textures/gui/range.png")
  val guiRobot = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot.png")
  val guiRobotSelection = new ResourceLocation(Settings.resourceDomain, "textures/gui/robot_selection.png")
  val guiServer = new ResourceLocation(Settings.resourceDomain, "textures/gui/server.png")
  val guiSlot = new ResourceLocation(Settings.resourceDomain, "textures/gui/slot.png")

  val blockCable = new ResourceLocation(Settings.resourceDomain, "textures/blocks/cable.png")
  val blockCaseFrontOn = new ResourceLocation(Settings.resourceDomain, "textures/blocks/case_front_on.png")
  val blockPowerDistributorOn = new ResourceLocation(Settings.resourceDomain, "textures/blocks/power_distributor_on.png")
  val blockRackFrontOn = new ResourceLocation(Settings.resourceDomain, "textures/blocks/rack_front_on.png")
  val blockRobot = new ResourceLocation(Settings.resourceDomain, "textures/blocks/robot.png")
  val blockScreenUpIndicator = new ResourceLocation(Settings.resourceDomain, "textures/blocks/screen/up_indicator.png")

  val upgradeCrafting = new ResourceLocation(Settings.resourceDomain, "textures/items/upgrade_crafting_equipped.png")
  val upgradeGenerator = new ResourceLocation(Settings.resourceDomain, "textures/items/upgrade_generator_equipped.png")

  @ForgeSubscribe
  def onItemIconRegister(e: TextureStitchEvent.Pre) {
    val iconRegister = e.map
    if (iconRegister.textureType == 1) {
      iconRegister.registerIcon(fontAntiAliased.toString)
      iconRegister.registerIcon(fontAliased.toString)

      iconRegister.registerIcon(guiBackground.toString)
      iconRegister.registerIcon(guiBorders.toString)
      iconRegister.registerIcon(guiButtonPower.toString)
      iconRegister.registerIcon(guiButtonRange.toString)
      iconRegister.registerIcon(guiButtonSide.toString)
      iconRegister.registerIcon(guiComputer.toString)
      iconRegister.registerIcon(guiRange.toString)
      iconRegister.registerIcon(guiRobot.toString)
      iconRegister.registerIcon(guiRobotSelection.toString)
      iconRegister.registerIcon(guiServer.toString)
      iconRegister.registerIcon(guiSlot.toString)

      iconRegister.registerIcon(blockCable.toString)
      iconRegister.registerIcon(blockCaseFrontOn.toString)
      iconRegister.registerIcon(blockPowerDistributorOn.toString)
      iconRegister.registerIcon(blockRackFrontOn.toString)
      iconRegister.registerIcon(blockRobot.toString)
      iconRegister.registerIcon(blockScreenUpIndicator.toString)

      iconRegister.registerIcon(upgradeCrafting.toString)
      iconRegister.registerIcon(upgradeGenerator.toString)
    }
  }

}
