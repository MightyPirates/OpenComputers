package li.cil.oc.server.agent

import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

import li.cil.oc.OpenComputers
import li.cil.oc.api.network.Node
import li.cil.oc.common.item.UpgradeExperience
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fml.common.eventhandler.{EventPriority, SubscribeEvent}

import scala.collection.convert.WrapAsScala._

object PlayerInteractionManagerHelper {

  private def isDestroyingBlock(player: Player): Boolean = {
    val manager = player.interactionManager
    val f = manager.getClass.getDeclaredField("isDestroyingBlock") //NoSuchFieldException
    f.setAccessible(true)
    f.get(manager).asInstanceOf[Boolean] //IllegalAccessException
  }

  def onBlockClicked(player: Player, pos: BlockPos, side: EnumFacing): Boolean = {
    if (isDestroyingBlock(player)) {
      player.interactionManager.cancelDestroyingBlock()
    }
    player.interactionManager.onBlockClicked(pos, side)
    isDestroyingBlock(player)
  }

  def updateBlockRemoving(player: Player): Boolean = {
    if (!isDestroyingBlock(player))
      return false
    player.interactionManager.updateBlockRemoving()
    isDestroyingBlock(player)
  }

  // returns exp gained from removing the block, -1 if block not removed
  // redone here because the interaction manager just drops the xp on the ground
  def blockRemoving(player: Player, pos: BlockPos): Int = {
    if (!isDestroyingBlock(player)) {
      return -1
    }

    //PlayerEvent.BreakSpeed
    val infBreaker = new {
      var expToDrop: Int = 0

      val hasExperienceUpgrade: Boolean = {
        val machineNode = player.agent.machine.node
        machineNode.reachableNodes.exists {
          case node: Node if node.canBeReachedFrom(machineNode) =>
            node.host.isInstanceOf[UpgradeExperience]
          case _ => false
        }
      }

      @SubscribeEvent(priority = EventPriority.LOWEST)
      def onBreakSpeedEvent(breakSpeedEvent: PlayerEvent.BreakSpeed): Unit = {
        if (player == breakSpeedEvent.getEntityPlayer)
          breakSpeedEvent.setNewSpeed(scala.Float.MaxValue)
      }

      @SubscribeEvent(priority = EventPriority.LOWEST)
      def onExperienceBreakEvent(experienceBreakEvent: BlockEvent.BreakEvent): Unit = {
        if (player == experienceBreakEvent.getPlayer) {
          if (hasExperienceUpgrade) {
            expToDrop += experienceBreakEvent.getExpToDrop
            experienceBreakEvent.setExpToDrop(0)
          }
        }
      }
    }

    MinecraftForge.EVENT_BUS.register(infBreaker)
    try {
      player.interactionManager.blockRemoving(pos)
      infBreaker.expToDrop
    } catch {
      case e: Exception => {
        OpenComputers.log.info(s"an exception was thrown while trying to call blockRemoving: ${e.getMessage}")
        player.interactionManager.cancelDestroyingBlock()
        -1
      }
    } finally {
      MinecraftForge.EVENT_BUS.unregister(infBreaker)
    }
  }
}
