package li.cil.oc.common.event

import li.cil.oc.common.EventHandler
import li.cil.oc.util.BlockPosition
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IWorldEventListener
import net.minecraft.world.World
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.mutable

/**
  * @author Vexatos
  */
object BlockChangeHandler {

  def addListener(listener: ChangeListener, coord: BlockPosition) = {
    EventHandler.scheduleServer(() => changeListeners.put(listener, coord))
  }

  def removeListener(listener: ChangeListener) = {
    EventHandler.scheduleServer(() => changeListeners.remove(listener))
  }

  private val changeListeners = mutable.WeakHashMap.empty[ChangeListener, BlockPosition]

  @SubscribeEvent
  def onWorldLoad(e: WorldEvent.Load) {
    e.getWorld.addEventListener(new Listener(e.getWorld))
  }

  trait ChangeListener {
    def onBlockChanged()
  }

  private class Listener(world: World) extends IWorldEventListener {
    override def notifyBlockUpdate(worldIn: World, pos: BlockPos, oldState: IBlockState, newState: IBlockState, flags: Int): Unit = {
      val current = BlockPosition(pos, world)
      for ((listener, coord) <- changeListeners) if (coord.equals(current)) {
        listener.onBlockChanged()
      }
    }

    override def spawnParticle(id: Int, ignoreRange: Boolean, p_190570_3_ : Boolean, x: Double, y: Double, z: Double, xSpeed: Double, ySpeed: Double, zSpeed: Double, parameters: Int*): Unit = {}

    override def playRecord(soundIn: SoundEvent, pos: BlockPos): Unit = {}

    override def playEvent(player: EntityPlayer, `type`: Int, blockPosIn: BlockPos, data: Int): Unit = {}

    override def onEntityAdded(entityIn: Entity): Unit = {}

    override def spawnParticle(particleID: Int, ignoreRange: Boolean, xCoord: Double, yCoord: Double, zCoord: Double, xOffset: Double, yOffset: Double, zOffset: Double, parameters: Int*): Unit = {}

    override def onEntityRemoved(entityIn: Entity): Unit = {}

    override def broadcastSound(soundID: Int, pos: BlockPos, data: Int): Unit = {}

    override def playSoundToAllNearExcept(player: EntityPlayer, soundIn: SoundEvent, category: SoundCategory, x: Double, y: Double, z: Double, volume: Float, pitch: Float): Unit = {}

    override def markBlockRangeForRenderUpdate(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Unit = {}

    override def sendBlockBreakProgress(breakerId: Int, pos: BlockPos, progress: Int): Unit = {}

    override def notifyLightSet(pos: BlockPos): Unit = {}
  }

}
