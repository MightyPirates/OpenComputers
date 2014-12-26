package li.cil.oc.util

import java.nio.ByteBuffer

import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import li.cil.oc.OpenComputers
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.SoundCategory
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.lwjgl.openal.OpenALException

import scala.collection.mutable

/**
 * This class contains the logic used by computers' internal "speakers".
 * It can generate square waves with a specific frequency and duration
 * and will play them through OpenAL, acquiring sources as necessary.
 * Tones that have finished playing are disposed automatically in the
 * tick handler.
 */
object Audio {
  private def sampleRate = 8000

  private val sources = mutable.Set.empty[Source]

  private def volume = Minecraft.getMinecraft.gameSettings.getSoundLevel(SoundCategory.BLOCKS)

  private var disableAudio = false

  def play(x: Float, y: Float, z: Float, frequencyInHz: Int, durationInMilliseconds: Int): Unit = {
    play(x, y, z, ".", frequencyInHz, durationInMilliseconds)
  }

  def play(x: Float, y: Float, z: Float, pattern: String, frequencyInHz: Int = 1000, durationInMilliseconds: Int = 200): Unit = {
    if (!disableAudio) {
      val distanceBasedGain = math.max(0, 1 - Minecraft.getMinecraft.thePlayer.getDistance(x, y, z) / 12).toFloat
      val gain = distanceBasedGain * volume
      if (gain > 0 && AL.isCreated) {
        val sampleCounts = pattern.toCharArray.
          map(ch => if (ch == '.') durationInMilliseconds else 2 * durationInMilliseconds).
          map(_ * sampleRate / 1000)
        // 50ms pause between pattern parts.
        val pauseSampleCount = 50 * sampleRate / 1000
        val data = BufferUtils.createByteBuffer(sampleCounts.sum + (sampleCounts.length - 1) * pauseSampleCount)
        val step = frequencyInHz / sampleRate.toFloat
        var offset = 0f
        for (sampleCount <- sampleCounts) {
          for (sample <- 0 until sampleCount) {
            val angle = 2 * math.Pi * offset
            // We could sort of fake the square wave with a little less
            // computational effort, but until somebody complains let's
            // go with the fourier series! We leave out the  4 / Pi because
            // it's just an approximation and we avoid clipping like this.
            val value = (0 to 6).map(k => math.sin((1 + k * 2) * angle) / (1 + k * 2)).sum * Byte.MaxValue
            // val tmp = math.sin(angle) * Byte.MaxValue
            // val value = math.signum(tmp) * 0.99 + tmp * 0.01
            offset += step
            if (offset > 1) offset -= 1
            data.put(value.toByte)
          }
          if (data.hasRemaining) {
            for (sample <- 0 until pauseSampleCount) {
              data.put(127: Byte)
            }
          }
        }
        data.rewind()

        // Watch out for sound cards running out of memory... this apparently
        // really does happen. I'm assuming this is due to too many sounds being
        // kept loaded, since from what I can see OC's releasing its audio
        // memory as it should.
        try sources.synchronized(sources += new Source(x, y, z, data, gain)) catch {
          case e: LessUselessOpenALException =>
            if (e.errorCode == AL10.AL_OUT_OF_MEMORY) {
              // Well... let's just stop here.
              OpenComputers.log.info("Couldn't play computer speaker sound because your sound card ran out of memory. Either your sound card is just really low-end, or there are just too many sounds in use already by other mods. Disabling computer speakers to avoid spamming your log file now.")
              disableAudio = true
            }
            else {
              OpenComputers.log.warn("Error playing computer speaker sound.", e)
            }
        }
      }
    }
  }

  def update() {
    if (!disableAudio) {
      sources.synchronized(sources --= sources.filter(_.checkFinished))

      // Clear error stack.
      if (AL.isCreated) {
        try AL10.alGetError() catch {
          case _: UnsatisfiedLinkError =>
            OpenComputers.log.warn("Negotiations with OpenAL broke down, disabling sounds.")
            disableAudio = true
        }
      }
    }
  }

  private class Source(val x: Float, y: Float, z: Float, val data: ByteBuffer, val gain: Float) {
    // Clear error stack.
    AL10.alGetError()

    val (source, buffer) = {
      val buffer = AL10.alGenBuffers()
      checkALError()

      try {
        AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO8, data, sampleRate)
        checkALError()

        val source = AL10.alGenSources()
        checkALError()

        try {
          AL10.alSourceQueueBuffers(source, buffer)
          checkALError()

          AL10.alSource3f(source, AL10.AL_POSITION, x, y, z)
          AL10.alSourcef(source, AL10.AL_GAIN, gain * 0.3f)
          checkALError()

          AL10.alSourcePlay(source)
          checkALError()

          (source, buffer)
        }
        catch {
          case t: Throwable =>
            AL10.alDeleteSources(source)
            throw t
        }
      }
      catch {
        case t: Throwable =>
          AL10.alDeleteBuffers(buffer)
          throw t
      }
    }

    def checkFinished = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING && {
      AL10.alDeleteSources(source)
      AL10.alDeleteBuffers(buffer)
      true
    }
  }

  // Having the error code in an accessible way is really cool, you know.
  class LessUselessOpenALException(val errorCode: Int) extends OpenALException(errorCode)

  // Custom implementation of Util.checkALError() that uses our custom exception.
  def checkALError(): Unit = {
    val errorCode = AL10.alGetError()
    if (errorCode != AL10.AL_NO_ERROR) {
      throw new LessUselessOpenALException(errorCode)
    }
  }

  FMLCommonHandler.instance.bus.register(this)

  @SubscribeEvent
  def onTick(e: ClientTickEvent) {
    update()
  }
}
