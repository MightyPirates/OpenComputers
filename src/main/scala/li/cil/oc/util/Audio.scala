package li.cil.oc.util

import java.nio.ByteBuffer

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.SimpleSound
import net.minecraft.util.SoundEvents
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.vector.Vector3d
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.event.TickEvent.ClientTickEvent
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10

import scala.collection.mutable

/**
  * This class contains the logic used by computers' internal "speakers".
  * It can generate square waves with a specific frequency and duration
  * and will play them through OpenAL, acquiring sources as necessary.
  * Tones that have finished playing are disposed automatically in the
  * tick handler.
  */
object Audio {
  private def sampleRate = Settings.get.beepSampleRate

  private def amplitude = Settings.get.beepAmplitude

  private def maxDistance = Settings.get.beepRadius

  private val sources = mutable.Set.empty[Source]

  private def volume = Minecraft.getInstance.options.getSoundSourceVolume(SoundCategory.BLOCKS)

  private var disableAudio = false

  def play(x: Float, y: Float, z: Float, frequencyInHz: Int, durationInMilliseconds: Int): Unit = {
    play(x, y, z, ".", frequencyInHz, durationInMilliseconds)
  }

  def play(x: Float, y: Float, z: Float, pattern: String, frequencyInHz: Int = 1000, durationInMilliseconds: Int = 200): Unit = {
    val mc = Minecraft.getInstance
    val distanceBasedGain = math.max(0, 1 - mc.player.position.distanceTo(new Vector3d(x, y, z)) / maxDistance).toFloat
    val gain = distanceBasedGain * volume
    if (gain <= 0 || amplitude <= 0) return

    if (disableAudio) {
      // Fallback audio generation, using built-in Minecraft sound. This can be
      // necessary on certain systems with audio cards that do not have enough
      // memory. May still fail, but at least we can say we tried!
      // Valid range is 20-2000Hz, clamp it to that and get a relative value.
      // MC's pitch system supports a minimum pitch of 0.5, however, so up it
      // by that.
      val clampedFrequency = ((frequencyInHz - 20) max 0 min 1980) / 1980f + 0.5f
      var delay = 0
      for (ch <- pattern) {
        val record = new SimpleSound(SoundEvents.NOTE_BLOCK_HARP, SoundCategory.BLOCKS, gain, clampedFrequency, new BlockPos(x, y, z))
        if (delay == 0) mc.getSoundManager.play(record)
        else mc.getSoundManager.playDelayed(record, delay)
        delay += ((if (ch == '.') durationInMilliseconds else 2 * durationInMilliseconds) * 20 / 1000) max 1
      }
    }
    else {
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
          val value = (math.signum(math.sin(angle)) * amplitude).toByte ^ 0x80
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
        case e: OpenALException =>
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

  def update() {
    if (!disableAudio) {
      sources.synchronized(sources --= sources.filter(_.checkFinished))

      // Clear error stack.
      try AL10.alGetError() catch {
        case _: UnsatisfiedLinkError =>
          OpenComputers.log.warn("Negotiations with OpenAL broke down, disabling sounds.")
          disableAudio = true
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
          AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, maxDistance)
          AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, maxDistance)
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
  class OpenALException(val errorCode: Int) extends RuntimeException

  // Custom implementation of Util.checkALError() that uses our custom exception.
  def checkALError(): Unit = {
    val errorCode = AL10.alGetError()
    if (errorCode != AL10.AL_NO_ERROR) {
      throw new OpenALException(errorCode)
    }
  }

  @SubscribeEvent
  def onTick(e: ClientTickEvent) {
    update()
  }
}
