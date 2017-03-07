package li.cil.oc.common.nanomachines.provider

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.DisableReason
import li.cil.oc.api.prefab.nanomachines.AbstractBehavior
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.Event

import scala.collection.mutable

object DisintegrationProvider extends ScalaProvider("c4e7e3c2-8069-4fbb-b08e-74b1bddcdfe7") {
  override def createScalaBehaviors(player: EntityPlayer) = Iterable(new DisintegrationBehavior(player))

  override def readBehaviorFromNBT(player: EntityPlayer, nbt: NBTTagCompound) = new DisintegrationBehavior(player)

  class DisintegrationBehavior(p: EntityPlayer) extends AbstractBehavior(p) {
    var breakingMap = mutable.Map.empty[BlockPosition, SlowBreakInfo]
    var breakingMapNew = mutable.Map.empty[BlockPosition, SlowBreakInfo]

    // Note: intentionally not overriding getNameHint. Gotta find this one manually!

    override def onDisable(reason: DisableReason): Unit = {
      val world = player.getEntityWorld
      for (pos <- breakingMap.keys) {
        world.destroyBlockInWorldPartially(pos.hashCode(), pos, -1)
      }
      breakingMap.clear()
    }

    override def update(): Unit = {
      val world = player.getEntityWorld
      if (!world.isRemote) player match {
        case _: FakePlayer => // Nope
        case playerMP: EntityPlayerMP =>
          val now = world.getTotalWorldTime

          // Check blocks in range.
          val blockPos = BlockPosition(player)
          val actualRange = Settings.get.nanomachineDisintegrationRange * api.Nanomachines.getController(player).getInputCount(this)
          for (x <- -actualRange to actualRange; y <- 0 to actualRange * 2; z <- -actualRange to actualRange) {
            val pos = BlockPosition(blockPos.offset(x, y, z))
            breakingMap.get(pos) match {
              case Some(info) if info.checkTool(player) =>
                breakingMapNew += pos -> info
                info.update(world, player, now)
              case None =>
                val event = new PlayerInteractEvent.LeftClickBlock(player, pos.toBlockPos, null, null)
                MinecraftForge.EVENT_BUS.post(event)
                val allowed = !event.isCanceled && event.getUseBlock != Event.Result.DENY && event.getUseItem != Event.Result.DENY
                val adventureOk = !world.getWorldInfo.getGameType.isAdventure || player.canPlayerEdit(pos.toBlockPos, null, player.getHeldItemMainhand)
                if (allowed && adventureOk && !world.isAirBlock(pos)) {
                  val blockState = world.getBlockState(pos.toBlockPos)
                  val hardness = blockState.getBlock.getPlayerRelativeBlockHardness(world.getBlockState(pos.toBlockPos), player, world, pos.toBlockPos)
                  if (hardness > 0) {
                    val timeToBreak = (1 / hardness).toInt
                    if (timeToBreak < 20 * 30) {
                      val info = new SlowBreakInfo(now, now + timeToBreak, pos, Option(player.getHeldItemMainhand).map(_.copy()), blockState)
                      world.destroyBlockInWorldPartially(pos.hashCode(), pos, 0)
                      breakingMapNew += pos -> info
                    }
                  }
                }
              case _ => // Tool changed, pretend block doesn't exist for this tick.
            }
          }

          // Handle completed breaks.
          for ((pos, info) <- breakingMap) {
            if (info.timeBroken < now) {
              breakingMapNew -= pos
              info.finish(world, playerMP)
            }
          }

          // Handle aborted / incomplete breaks.
          for (pos <- breakingMap.keySet -- breakingMapNew.keySet) {
            world.destroyBlockInWorldPartially(pos.hashCode(), pos, -1)
          }

          val tmp = breakingMap
          breakingMap.clear()
          breakingMap = breakingMapNew
          breakingMapNew = tmp
        case _ => // Not available for fake players, sorry :P
      }
    }
  }

  class SlowBreakInfo(val timeStarted: Long, val timeBroken: Long, val pos: BlockPosition, val originalTool: Option[ItemStack], val blockState: IBlockState) {
    var lastDamageSent = 0

    def checkTool(player: EntityPlayer): Boolean = {
      val currentTool = Option(player.getHeldItemMainhand).map(_.copy())
      (currentTool, originalTool) match {
        case (Some(stackA), Some(stackB)) => stackA.getItem == stackB.getItem && (stackA.isItemStackDamageable || stackA.getItemDamage == stackB.getItemDamage)
        case (None, None) => true
        case _ => false
      }
    }

    def update(world: World, player: EntityPlayer, now: Long): Unit = {
      val timeTotal = timeBroken - timeStarted
      if (timeTotal > 0) {
        val timeTaken = now - timeStarted
        val damage = 10 * timeTaken / timeTotal
        if (damage != lastDamageSent) {
          lastDamageSent = damage.toInt
          world.destroyBlockInWorldPartially(pos.hashCode(), pos, lastDamageSent)
        }
      }
    }

    def finish(world: World, player: EntityPlayerMP): Unit = {
      val sameBlock = world.getBlockState(pos.toBlockPos) == blockState
      if (sameBlock) {
        world.destroyBlockInWorldPartially(pos.hashCode(), pos, -1)
        if (player.interactionManager.tryHarvestBlock(pos.toBlockPos)) {
          world.playAuxSFX(2001, pos, Block.getIdFromBlock(blockState.getBlock) + (blockState.getBlock.getMetaFromState(blockState) << 12))
        }
      }
    }
  }

}
