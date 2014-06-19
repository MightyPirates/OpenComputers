package li.cil.oc.client

import java.util.logging.Level
import java.util.{Timer, TimerTask, UUID}

import cpw.mods.fml.client.FMLClientHandler
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.client.Minecraft
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.client.event.sound.SoundLoadEvent
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.WorldEvent
import paulscode.sound.SoundSystemConfig

import scala.collection.mutable

object Sound {
  private val sources = mutable.Map.empty[TileEntity, PseudoLoopingStream]

  private val commandQueue = mutable.PriorityQueue.empty[Command]

  private var lastVolume = FMLClientHandler.instance.getClient.gameSettings.soundVolume

  private val updateTimer = new Timer("OpenComputers-SoundUpdater", true)
  if (Settings.get.soundVolume > 0) {
    updateTimer.scheduleAtFixedRate(new TimerTask {
      override def run() {
        updateVolume()
        processQueue()
      }
    }, 500, 50)
  }

  private def soundSystem = Minecraft.getMinecraft.sndManager.sndSystem

  private def updateVolume() {
    val volume = FMLClientHandler.instance.getClient.gameSettings.soundVolume
    if (volume != lastVolume) {
      lastVolume = volume
      sources.synchronized {
        for (sound <- sources.values) {
          sound.updateVolume()
        }
      }
    }
  }

  private def processQueue() {
    if (!commandQueue.isEmpty) {
      commandQueue.synchronized {
        while (!commandQueue.isEmpty && commandQueue.head.when < System.currentTimeMillis()) {
          try commandQueue.dequeue()() catch {
            case t: Throwable => OpenComputers.log.log(Level.WARNING, "Error processing sound command.", t)
          }
        }
      }
    }
  }

  def startLoop(tileEntity: TileEntity, name: String, volume: Float = 1f, delay: Long = 0) {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new StartCommand(System.currentTimeMillis() + delay, tileEntity, name, volume)
      }
    }
  }

  def stopLoop(tileEntity: TileEntity) {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new StopCommand(tileEntity)
      }
    }
  }

  def updatePosition(tileEntity: TileEntity) {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new UpdatePositionCommand(tileEntity)
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
  def onWorldUnload(event: WorldEvent.Unload) {
    commandQueue.synchronized(commandQueue.clear())
    sources.synchronized {
      sources.foreach(_._2.stop())
    }
  }

  private abstract class Command(val when: Long, val tileEntity: TileEntity) extends Ordered[Command] {
    def apply()

    override def compare(that: Command) = (that.when - when).toInt
  }

  private class StartCommand(when: Long, tileEntity: TileEntity, val name: String, val volume: Float) extends Command(when, tileEntity) {
    override def apply() {
      sources.synchronized {
        sources.getOrElseUpdate(tileEntity, new PseudoLoopingStream(tileEntity, volume)).play(name)
      }
    }
  }

  private class StopCommand(tileEntity: TileEntity) extends Command(0, tileEntity) {
    override def apply() {
      sources.synchronized {
        sources.remove(tileEntity) match {
          case Some(sound) => sound.stop()
          case _ =>
        }
      }
      commandQueue.synchronized {
        // Remove all other commands for this tile entity from the queue. This
        // is inefficient, but we generally don't expect the command queue to
        // be very long, so this should be OK.
        commandQueue ++= commandQueue.dequeueAll.filter(_.tileEntity != tileEntity)
      }
    }
  }

  private class UpdatePositionCommand(tileEntity: TileEntity) extends Command(0, tileEntity) {
    override def apply() {
      sources.synchronized {
        sources.get(tileEntity) match {
          case Some(sound) => sound.updatePosition()
          case _ =>
        }
      }
    }
  }

  private class PseudoLoopingStream(val tileEntity: TileEntity, val volume: Float, val source: String = UUID.randomUUID.toString) {
    var initialized = false

    def updateVolume() {
      soundSystem.setVolume(source, lastVolume * volume * Settings.get.soundVolume)
    }

    def updatePosition() {
      soundSystem.setPosition(source, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord)
    }

    def play(name: String) {
      val resourceName = s"${Settings.resourceDomain}:$name"
      val sound = Minecraft.getMinecraft.sndManager.soundPoolSounds.getRandomSoundFromSoundPool(resourceName)
      if (!initialized) {
        initialized = true
        soundSystem.newSource(false, source, sound.getSoundUrl, sound.getSoundName, true, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, SoundSystemConfig.ATTENUATION_LINEAR, 16)
        updateVolume()
        soundSystem.activate(source)
      }
      soundSystem.play(source)
    }

    def stop() {
      if (soundSystem != null) try {
        soundSystem.stop(source)
        soundSystem.removeSource(source)
      }
      catch {
        case _: Throwable =>
      }
    }
  }

}
