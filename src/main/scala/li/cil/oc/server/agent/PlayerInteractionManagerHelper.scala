package li.cil.oc.server.agent

import net.minecraft.network.play.client.CPlayerDiggingPacket
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import li.cil.oc.OpenComputers
import li.cil.oc.api.network.Node
import net.minecraft.server.management.PlayerInteractionManager
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.world.BlockEvent
import net.minecraftforge.fml.common.ObfuscationReflectionHelper
import net.minecraftforge.eventbus.api.{EventPriority, SubscribeEvent}

import scala.collection.convert.ImplicitConversionsToScala._

object PlayerInteractionManagerHelper {
  private val isDestroyingBlock = ObfuscationReflectionHelper.findField(classOf[PlayerInteractionManager], "field_73088_d")

  private def isDestroyingBlock(player: Player): Boolean = {
    try {
      isDestroyingBlock.getBoolean(player.gameMode)
    } catch {
      case _: Exception => true
    }
  }

  def onBlockClicked(player: Player, pos: BlockPos, side: Direction): Boolean = {
    if (isDestroyingBlock(player)) {
      player.gameMode.handleBlockBreakAction(pos, CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK, side, 0)
    }
    player.gameMode.handleBlockBreakAction(pos, CPlayerDiggingPacket.Action.START_DESTROY_BLOCK, side, 0)
    isDestroyingBlock(player)
  }

  def updateBlockRemoving(player: Player): Boolean = {
    if (!isDestroyingBlock(player))
      return false
    player.gameMode.tick()
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
            node.host.isInstanceOf[li.cil.oc.common.item.UpgradeExperience] ||
            node.host.isInstanceOf[li.cil.oc.server.component.UpgradeExperience]
          case _ => false
        }
      }

      @SubscribeEvent(priority = EventPriority.LOWEST)
      def onBreakSpeedEvent(breakSpeedEvent: PlayerEvent.BreakSpeed): Unit = {
        if (player == breakSpeedEvent.getPlayer)
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
      player.gameMode.handleBlockBreakAction(pos, CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK, null, 0)
      infBreaker.expToDrop
    } catch {
      case e: Exception => {
        OpenComputers.log.info(s"an exception was thrown while trying to call blockRemoving: ${e.getMessage}")
        player.gameMode.handleBlockBreakAction(pos, CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK, null, 0)
        -1
      }
    } finally {
      MinecraftForge.EVENT_BUS.unregister(infBreaker)
    }
  }
}
