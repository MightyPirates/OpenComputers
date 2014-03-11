package li.cil.oc.client

import cpw.mods.fml.client.FMLClientHandler
import java.util.{TimerTask, Timer, UUID}
import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import net.minecraft.client.Minecraft
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.client.event.sound.SoundLoadEvent
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.{WorldEvent, ChunkEvent}
import paulscode.sound.SoundSystemConfig
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object Sound {
  val sources = mutable.Map.empty[TileEntity, (String, Float)]

  var lastVolume = FMLClientHandler.instance.getClient.gameSettings.soundVolume

  val volumeCheckTimer = new Timer("OpenComputers-VolumeUpdater", true)
  volumeCheckTimer.scheduleAtFixedRate(new TimerTask {
    override def run() {
      val volume = FMLClientHandler.instance.getClient.gameSettings.soundVolume
      if (volume != lastVolume) {
        lastVolume = volume
        val system = Minecraft.getMinecraft.sndManager.sndSystem
        sources.synchronized {
          for ((source, volume) <- sources.values) {
            system.setVolume(source, lastVolume * volume)
          }
        }
      }
    }
  }, 5000, 500)

  def startLoop(tileEntity: TileEntity, name: String, volume: Float = 1f) {
    val resourceName = s"${Settings.resourceDomain}:$name"
    val manager = Minecraft.getMinecraft.sndManager
    val sound = manager.soundPoolSounds.getRandomSoundFromSoundPool(resourceName)
    sources.synchronized {
      val (source, _) = sources.getOrElseUpdate(tileEntity, {
        val source = UUID.randomUUID.toString
        manager.sndSystem.newStreamingSource(false, source, sound.getSoundUrl, sound.getSoundName, true, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, SoundSystemConfig.ATTENUATION_LINEAR, 16.0f)
        manager.sndSystem.setVolume(source, lastVolume * volume)
        (source, volume)
      })
      manager.sndSystem.fadeOutIn(source, sound.getSoundUrl, sound.getSoundName, 50, 500)
      manager.sndSystem.play(source)
    }
  }

  def updatePosition(tileEntity: TileEntity) {
    sources.synchronized {
      sources.get(tileEntity) match {
        case Some((source, _)) => Minecraft.getMinecraft.sndManager.sndSystem.
          setPosition(source, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord)
        case _ =>
      }
    }
  }

  def stopLoop(tileEntity: TileEntity) {
    sources.synchronized {
      sources.get(tileEntity) match {
        case Some((source, _)) => Minecraft.getMinecraft.sndManager.sndSystem.fadeOut(source, null, 500)
        case _ =>
      }
    }
  }

  @ForgeSubscribe
  def onSoundLoad(event: SoundLoadEvent) {
    for (i <- 1 to 6) {
      event.manager.soundPoolSounds.addSound(Settings.resourceDomain + s":floppy_access$i.ogg")
    }
    for (i <- 1 to 7) {
      event.manager.soundPoolSounds.addSound(Settings.resourceDomain + s":hdd_access$i.ogg")
    }
    event.manager.soundPoolSounds.addSound(Settings.resourceDomain + ":floppy_insert.ogg")
    event.manager.soundPoolSounds.addSound(Settings.resourceDomain + ":floppy_eject.ogg")

    event.manager.soundPoolSounds.addSound(Settings.resourceDomain + ":computer_running.ogg")
  }

  @ForgeSubscribe
  def onChunkUnload(event: ChunkEvent.Unload) {
    cleanup(event.getChunk.chunkTileEntityMap.values)
  }

  @ForgeSubscribe
  def onWorldUnload(event: WorldEvent.Unload) {
    cleanup(event.world.loadedTileEntityList)
  }

  def cleanup[_](list: Iterable[_]) {
    val system = Minecraft.getMinecraft.sndManager.sndSystem
    sources.synchronized {
      list.foreach {
        case robot: tileentity.RobotProxy => sources.remove(robot.robot) match {
          case Some((source, _)) =>
            system.stop(source)
            system.removeSource(source)
          case _ =>
        }
        case tileEntity: TileEntity => sources.remove(tileEntity) match {
          case Some((source, _)) =>
            system.stop(source)
            system.removeSource(source)
          case _ =>
        }
        case _ =>
      }
    }
  }
}
