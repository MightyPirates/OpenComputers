package li.cil.oc.client

import cpw.mods.fml.client.FMLClientHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import java.util.{TimerTask, Timer, UUID}
import li.cil.oc.common.tileentity
import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.client.event.sound.SoundLoadEvent
import net.minecraftforge.event.world.{WorldEvent, ChunkEvent}
import paulscode.sound.{SoundSystem, SoundSystemConfig}
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import net.minecraft.client.audio.{SoundCategory, SoundManager}
import cpw.mods.fml.relauncher.ReflectionHelper
import net.minecraft.util.ResourceLocation
import java.net.{MalformedURLException, URLConnection, URLStreamHandler, URL}

object Sound {
  val sources = mutable.Map.empty[TileEntity, (String, Float)]

  var lastVolume = FMLClientHandler.instance.getClient.gameSettings.getSoundLevel(SoundCategory.BLOCKS)

  // Set in init event.
  var manager: SoundManager = _

  def handler = Minecraft.getMinecraft.getSoundHandler

  def soundSystem: SoundSystem = ReflectionHelper.getPrivateValue(classOf[SoundManager], manager, "sndSystem")

  val volumeCheckTimer = new Timer("OpenComputers-VolumeUpdater", true)
  volumeCheckTimer.scheduleAtFixedRate(new TimerTask {
    override def run() {
      val volume = FMLClientHandler.instance.getClient.gameSettings.getSoundLevel(SoundCategory.BLOCKS)
      if (volume != lastVolume) {
        lastVolume = volume
        sources.synchronized {
          for ((source, volume) <- sources.values) {
            soundSystem.setVolume(source, lastVolume * volume * Settings.get.soundVolume)
          }
        }
      }
    }
  }, 5000, 500)

  def startLoop(tileEntity: TileEntity, name: String, volume: Float = 1f) {
    if (Settings.get.soundVolume > 0) {
      val resourceName = s"${Settings.resourceDomain}:$name"
      val sound = Minecraft.getMinecraft.getSoundHandler.getSound(new ResourceLocation(resourceName))
      val resource = sound.func_148720_g.getSoundPoolEntryLocation
      sources.synchronized {
        val (source, _) = sources.getOrElseUpdate(tileEntity, {
          val source = UUID.randomUUID.toString
          soundSystem.newStreamingSource(false, source, toUrl(resource), resource.toString, true, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, SoundSystemConfig.ATTENUATION_LINEAR, 16.0f)
          soundSystem.setVolume(source, lastVolume * volume * Settings.get.soundVolume)
          (source, volume)
        })
        soundSystem.fadeOutIn(source, toUrl(resource), resource.toString, 50, 500)
        soundSystem.play(source)
      }
    }
  }

  def updatePosition(tileEntity: TileEntity) {
    sources.synchronized {
      sources.get(tileEntity) match {
        case Some((source, _)) => soundSystem.setPosition(source, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord)
        case _ =>
      }
    }
  }

  def stopLoop(tileEntity: TileEntity) {
    sources.synchronized {
      sources.get(tileEntity) match {
        case Some((source, _)) => soundSystem.fadeOut(source, null, 500)
        case _ =>
      }
    }
  }

  @SubscribeEvent
  def onSoundLoad(event: SoundLoadEvent) {
    manager = event.manager
  }

  @SubscribeEvent
  def onChunkUnload(event: ChunkEvent.Unload) {
    cleanup(event.getChunk.chunkTileEntityMap.values)
  }

  @SubscribeEvent
  def onWorldUnload(event: WorldEvent.Unload) {
    cleanup(event.world.loadedTileEntityList)
  }

  def cleanup[_](list: Iterable[_]) {
    sources.synchronized {
      list.foreach {
        case robot: tileentity.RobotProxy => sources.remove(robot.robot) match {
          case Some((source, _)) =>
            soundSystem.stop(source)
            soundSystem.removeSource(source)
          case _ =>
        }
        case tileEntity: TileEntity => sources.remove(tileEntity) match {
          case Some((source, _)) =>
            soundSystem.stop(source)
            soundSystem.removeSource(source)
          case _ =>
        }
        case _ =>
      }
    }
  }

  // This is copied from SoundManager.getURLForSoundResource, which is private.
  private def toUrl(resource: ResourceLocation): URL = {
    val name = s"mcsounddomain:${resource.getResourceDomain}:${resource.getResourcePath}"
    try {
      new URL(null, name, new URLStreamHandler {
        protected def openConnection(url: URL): URLConnection = new URLConnection(url) {
          def connect() {
          }

          override def getInputStream = Minecraft.getMinecraft.getResourceManager.getResource(resource).getInputStream
        }
      })
    }
    catch {
      case _: MalformedURLException => null
    }
  }
}
