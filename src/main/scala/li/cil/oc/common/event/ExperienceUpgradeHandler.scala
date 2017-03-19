package li.cil.oc.common.event

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api.event._
import li.cil.oc.api.internal.Agent
import li.cil.oc.api.internal.Robot
import li.cil.oc.api.network.Node
import li.cil.oc.server.component
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsScala._

object ExperienceUpgradeHandler {
  @SubscribeEvent
  def onRobotAnalyze(e: RobotAnalyzeEvent) {
    val (level, experience) = getLevelAndExperience(e.agent)
    // This is basically a 'does it have an experience upgrade' check.
    if (experience != 0.0) {
      e.player.sendMessage(Localization.Analyzer.RobotXp(experience, level))
    }
  }

  @SubscribeEvent
  def onRobotComputeDamageRate(e: RobotUsedToolEvent.ComputeDamageRate) {
    e.setDamageRate(e.getDamageRate * math.max(0, 1 - getLevel(e.agent) * Settings.Robot.Experience.toolEfficiencyPerLevel))
  }

  @SubscribeEvent
  def onRobotBreakBlockPre(e: RobotBreakBlockEvent.Pre) {
    val boost = math.max(0, 1 - getLevel(e.agent) * Settings.Robot.Experience.harvestSpeedBoostPerLevel)
    e.setBreakTime(e.getBreakTime * boost)
  }

  @SubscribeEvent
  def onRobotAttackEntityPost(e: RobotAttackEntityEvent.Post) {
    e.agent match {
      case robot: Robot =>
        if (robot.equipmentInventory.getStackInSlot(0) != null && e.target.isDead) {
          addExperience(robot, Settings.Robot.Experience.actionXp)
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotBreakBlockPost(e: RobotBreakBlockEvent.Post) {
    addExperience(e.agent, e.experience * Settings.Robot.Experience.oreXpRate + Settings.Robot.Experience.actionXp)
  }

  @SubscribeEvent
  def onRobotPlaceBlockPost(e: RobotPlaceBlockEvent.Post) {
    addExperience(e.agent, Settings.Robot.Experience.actionXp)
  }

  @SubscribeEvent
  def onRobotMovePost(e: RobotMoveEvent.Post) {
    addExperience(e.agent, Settings.Robot.Experience.exhaustionXpRate * 0.01)
  }

  @SubscribeEvent
  def onRobotExhaustion(e: RobotExhaustionEvent) {
    addExperience(e.agent, Settings.Robot.Experience.exhaustionXpRate * e.exhaustion)
  }

  @SubscribeEvent
  def onRobotRender(e: RobotRenderEvent) {
    val level = e.agent match {
      case robot: Robot =>
        var acc = 0
        for (index <- 0 until robot.getSizeInventory) {
          robot.getComponentInSlot(index) match {
            case upgrade: component.UpgradeExperience =>
              acc += upgrade.level
            case _ =>
          }
        }
        acc
      case _ => 0
    }
    if (level > 19) {
      GlStateManager.color(0.4f, 1, 1)
    }
    else if (level > 9) {
      GlStateManager.color(1, 1, 0.4f)
    }
    else {
      GlStateManager.color(0.5f, 0.5f, 0.5f)
    }
  }

  private def getLevel(agent: Agent) = {
    var level = 0
    foreachUpgrade(agent.machine.node, upgrade => level += upgrade.level)
    level
  }

  private def getLevelAndExperience(agent: Agent) = {
    var level = 0
    var experience = 0.0
    foreachUpgrade(agent.machine.node, upgrade => {
      level += upgrade.level
      experience += upgrade.experience
    })
    (level, experience)
  }

  private def addExperience(agent: Agent, amount: Double) {
    foreachUpgrade(agent.machine.node, upgrade => upgrade.addExperience(amount))
  }

  private def foreachUpgrade(node: Node, f: (component.UpgradeExperience) => Unit): Unit = {
    node.getReachableNodes.foreach(_.getContainer match {
      case upgrade: component.UpgradeExperience => f(upgrade)
      case _ =>
    })
  }
}
