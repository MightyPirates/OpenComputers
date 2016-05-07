package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.common.EventHandler
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.{IWorldAccess, World}
import net.minecraftforge.event.world.WorldEvent

import scala.collection.mutable

/**
  *
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

    override def markBlockForUpdate(x: Int, y: Int, z: Int): Unit = {
      val current = BlockPosition(x, y, z, world)
      for ((listener, coord) <- changeListeners) if (coord.equals(current)) {
        listener.onBlockChanged()
      }
    }

    override def playRecord(recordName: String, x: Int, y: Int, z: Int) {}

    override def playAuxSFX(player: EntityPlayer, sfxType: Int, x: Int, y: Int, z: Int, data: Int) {}

    override def onEntityDestroy(entity: Entity) {}

    override def destroyBlockPartially(breakerId: Int, x: Int, y: Int, z: Int, progress: Int) {}

    override def markBlockForRenderUpdate(x: Int, y: Int, z: Int) {}

    override def spawnParticle(particleType: String, x: Double, y: Double, z: Double, velX: Double, velY: Double, velZ: Double) {}

    override def playSound(soundName: String, x: Double, y: Double, z: Double, volume: Float, pitch: Float) {}

    override def broadcastSound(soundID: Int, x: Int, y: Int, z: Int, data: Int) {}

    override def playSoundToNearExcept(player: EntityPlayer, soundName: String, x: Double, y: Double, z: Double, volume: Float, pitch: Float) {}

    override def markBlockRangeForRenderUpdate(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) {}

    override def onEntityCreate(entity: Entity) {}

    override def onStaticEntitiesChanged() {}
  }

}
