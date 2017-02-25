package li.cil.oc.common.event

import li.cil.oc.common.EventHandler
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.BlockPos
import net.minecraft.world.IWorldAccess
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
    e.world.addWorldAccess(new Listener(e.world))
  }

  trait ChangeListener {
    def onBlockChanged()
  }

  private class Listener(world: World) extends IWorldAccess {
    override def markBlockForUpdate(pos: BlockPos): Unit = {
      val current = BlockPosition(pos, world)
      for ((listener, coord) <- changeListeners) if (coord.equals(current)) {
        listener.onBlockChanged()
      }
    }

    override def playRecord(recordName: String, blockPosIn: BlockPos): Unit = {}

    override def playAuxSFX(player: EntityPlayer, sfxType: Int, blockPosIn: BlockPos, data: Int): Unit = {}

    override def onEntityAdded(entityIn: Entity): Unit = {}

    override def spawnParticle(particleID: Int, ignoreRange: Boolean, xCoord: Double, yCoord: Double, zCoord: Double, xOffset: Double, yOffset: Double, zOffset: Double, parameters: Int*): Unit = {}

    override def playSound(soundName: String, x: Double, y: Double, z: Double, volume: Float, pitch: Float): Unit = {}

    override def onEntityRemoved(entityIn: Entity): Unit = {}

    override def broadcastSound(soundID: Int, pos: BlockPos, data: Int): Unit = {}

    override def playSoundToNearExcept(except: EntityPlayer, soundName: String, x: Double, y: Double, z: Double, volume: Float, pitch: Float): Unit = {}

    override def markBlockRangeForRenderUpdate(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Unit = {}

    override def sendBlockBreakProgress(breakerId: Int, pos: BlockPos, progress: Int): Unit = {}

    override def notifyLightSet(pos: BlockPos): Unit = {}
  }

}
