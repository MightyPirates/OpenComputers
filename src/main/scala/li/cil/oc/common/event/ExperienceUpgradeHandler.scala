package li.cil.oc.common.event

import li.cil.oc.api.event._
import li.cil.oc.api.machine.Robot
import li.cil.oc.server.component
import li.cil.oc.{Localization, Settings}
import net.minecraftforge.event.ForgeSubscribe
import org.lwjgl.opengl.GL11

object ExperienceUpgradeHandler {
  @ForgeSubscribe
  def onRobotAnalyze(e: RobotAnalyzeEvent) {
    val (level, experience) = getLevelAndExperience(e.robot)
    // This is basically a 'does it have an experience upgrade' check.
    if (experience != 0.0) {
      e.player.sendChatToPlayer(Localization.Analyzer.RobotXp(experience, level))
    }
  }

  @ForgeSubscribe
  def onRobotComputeDamageRate(e: RobotUsedTool.ComputeDamageRate) {
    e.setDamageRate(e.getDamageRate * math.max(0, 1 - getLevel(e.robot) * Settings.get.toolEfficiencyPerLevel))
  }

  @ForgeSubscribe
  def onRobotBreakBlockPre(e: RobotBreakBlockEvent.Pre) {
    val boost = math.max(0, 1 - getLevel(e.robot) * Settings.get.harvestSpeedBoostPerLevel)
    e.setBreakTime(e.getBreakTime * boost)
  }

  @ForgeSubscribe
  def onRobotAttackEntityPost(e: RobotAttackEntityEvent.Post) {
    if (e.robot.getComponentInSlot(e.robot.selectedSlot()) != null && e.target.isDead) {
      addExperience(e.robot, Settings.get.robotActionXp)
    }
  }

  @ForgeSubscribe
  def onRobotBreakBlockPost(e: RobotBreakBlockEvent.Post) {
    addExperience(e.robot, e.experience * Settings.get.robotOreXpRate + Settings.get.robotActionXp)
  }

  @ForgeSubscribe
  def onRobotPlaceBlockPost(e: RobotPlaceBlockEvent.Post) {
    addExperience(e.robot, Settings.get.robotActionXp)
  }

  @ForgeSubscribe
  def onRobotMovePost(e: RobotMoveEvent.Post) {
    addExperience(e.robot, Settings.get.robotExhaustionXpRate * 0.01)
  }

  @ForgeSubscribe
  def onRobotExhaustion(e: RobotExhaustionEvent) {
    addExperience(e.robot, Settings.get.robotExhaustionXpRate * e.exhaustion)
  }

  @ForgeSubscribe
  def onRobotRender(e: RobotRenderEvent) {
    val level = if (e.robot != null) getLevel(e.robot) else 0
    if (level > 19) {
      GL11.glColor3f(0.4f, 1, 1)
    }
    else if (level > 9) {
      GL11.glColor3f(1, 1, 0.4f)
    }
    else {
      GL11.glColor3f(0.5f, 0.5f, 0.5f)
    }
  }

  private def getLevel(robot: Robot) = {
    var level = 0
    for (index <- 0 until robot.getSizeInventory) {
      robot.getComponentInSlot(index) match {
        case upgrade: component.UpgradeExperience =>
          level += upgrade.level
        case _ =>
      }
    }
    level
  }

  private def getLevelAndExperience(robot: Robot) = {
    var level = 0
    var experience = 0.0
    for (index <- 0 until robot.getSizeInventory) {
      robot.getComponentInSlot(index) match {
        case upgrade: component.UpgradeExperience =>
          level += upgrade.level
          experience += upgrade.experience
        case _ =>
      }
    }
    (level, experience)
  }

  private def addExperience(robot: Robot, amount: Double) {
    for (index <- 0 until robot.getSizeInventory) {
      robot.getComponentInSlot(index) match {
        case upgrade: component.UpgradeExperience =>
          upgrade.addExperience(amount)
        case _ =>
      }
    }
  }
}
