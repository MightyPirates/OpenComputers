package li.cil.oc.common.event

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api.event._
import li.cil.oc.api.internal
import li.cil.oc.api.internal.Robot
import li.cil.oc.server.component
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

object ExperienceUpgradeHandler {
  @SubscribeEvent
  def onRobotAnalyze(e: RobotAnalyzeEvent) {
    e.agent match {
      case robot: internal.Robot =>
        val (level, experience) = getLevelAndExperience(robot)
        // This is basically a 'does it have an experience upgrade' check.
        if (experience != 0.0) {
          e.player.addChatMessage(Localization.Analyzer.RobotXp(experience, level))
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotComputeDamageRate(e: RobotUsedToolEvent.ComputeDamageRate) {
    e.agent match {
      case robot: internal.Robot =>
        e.setDamageRate(e.getDamageRate * math.max(0, 1 - getLevel(robot) * Settings.get.toolEfficiencyPerLevel))
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotBreakBlockPre(e: RobotBreakBlockEvent.Pre) {
    e.agent match {
      case robot: internal.Robot =>
        val boost = math.max(0, 1 - getLevel(robot) * Settings.get.harvestSpeedBoostPerLevel)
        e.setBreakTime(e.getBreakTime * boost)
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotAttackEntityPost(e: RobotAttackEntityEvent.Post) {
    e.agent match {
      case robot: internal.Robot =>
        if (robot.getComponentInSlot(robot.selectedSlot()) != null && e.target.isDead) {
          addExperience(robot, Settings.get.robotActionXp)
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotBreakBlockPost(e: RobotBreakBlockEvent.Post) {
    e.agent match {
      case robot: internal.Robot =>
        addExperience(robot, e.experience * Settings.get.robotOreXpRate + Settings.get.robotActionXp)
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotPlaceBlockPost(e: RobotPlaceBlockEvent.Post) {
    e.agent match {
      case robot: internal.Robot =>
        addExperience(robot, Settings.get.robotActionXp)
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotMovePost(e: RobotMoveEvent.Post) {
    e.agent match {
      case robot: internal.Robot =>
        addExperience(robot, Settings.get.robotExhaustionXpRate * 0.01)
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotExhaustion(e: RobotExhaustionEvent) {
    e.agent match {
      case robot: internal.Robot =>
        addExperience(robot, Settings.get.robotExhaustionXpRate * e.exhaustion)
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotRender(e: RobotRenderEvent) {
    e.agent match {
      case robot: internal.Robot =>
        val level = getLevel(robot)
        if (level > 19) {
          GL11.glColor3f(0.4f, 1, 1)
        }
        else if (level > 9) {
          GL11.glColor3f(1, 1, 0.4f)
        }
        else {
          GL11.glColor3f(0.5f, 0.5f, 0.5f)
        }
      case _ =>
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
