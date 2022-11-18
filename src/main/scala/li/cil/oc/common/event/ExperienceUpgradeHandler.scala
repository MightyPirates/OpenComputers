package li.cil.oc.common.event

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api.event._
import li.cil.oc.api.internal.Agent
import li.cil.oc.api.internal.Robot
import li.cil.oc.api.network.Node
import li.cil.oc.server.component
import net.minecraft.util.Util
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.convert.ImplicitConversionsToScala._

object ExperienceUpgradeHandler {
  @SubscribeEvent
  def onRobotAnalyze(e: RobotAnalyzeEvent) {
    val (level, experience) = getLevelAndExperience(e.agent)
    // This is basically a 'does it have an experience upgrade' check.
    if (experience != 0.0) {
      e.player.sendMessage(Localization.Analyzer.RobotXp(experience, level), Util.NIL_UUID)
    }
  }

  @SubscribeEvent
  def onRobotComputeDamageRate(e: RobotUsedToolEvent.ComputeDamageRate) {
    e.setDamageRate(e.getDamageRate * math.max(0, 1 - getLevel(e.agent) * Settings.get.toolEfficiencyPerLevel))
  }

  @SubscribeEvent
  def onRobotBreakBlockPre(e: RobotBreakBlockEvent.Pre) {
    val boost = math.max(0, 1 - getLevel(e.agent) * Settings.get.harvestSpeedBoostPerLevel)
    e.setBreakTime(e.getBreakTime * boost)
  }

  @SubscribeEvent
  def onRobotAttackEntityPost(e: RobotAttackEntityEvent.Post) {
    e.agent match {
      case robot: Robot =>
        if (robot.equipmentInventory.getItem(0) != null && !e.target.isAlive) {
          addExperience(robot, Settings.get.robotActionXp)
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def onRobotBreakBlockPost(e: RobotBreakBlockEvent.Post) {
    addExperience(e.agent, e.experience * Settings.get.robotOreXpRate + Settings.get.robotActionXp)
  }

  @SubscribeEvent
  def onRobotPlaceBlockPost(e: RobotPlaceBlockEvent.Post) {
    addExperience(e.agent, Settings.get.robotActionXp)
  }

  @SubscribeEvent
  def onRobotMovePost(e: RobotMoveEvent.Post) {
    addExperience(e.agent, Settings.get.robotExhaustionXpRate * 0.01)
  }

  @SubscribeEvent
  def onRobotExhaustion(e: RobotExhaustionEvent) {
    addExperience(e.agent, Settings.get.robotExhaustionXpRate * e.exhaustion)
  }

  @SubscribeEvent
  def onRobotRender(e: RobotRenderEvent) {
    val level = e.agent match {
      case robot: Robot =>
        var acc = 0
        for (index <- 0 until robot.getContainerSize) {
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
      e.multiplyColors(0.4f, 1, 1)
    }
    else if (level > 9) {
      e.multiplyColors(1, 1, 0.4f)
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
    node.reachableNodes.foreach(_.host match {
      case upgrade: component.UpgradeExperience => f(upgrade)
      case _ =>
    })
  }
}
