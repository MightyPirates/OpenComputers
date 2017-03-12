package li.cil.oc.common

import li.cil.oc.Settings
import li.cil.oc.api.util.Location
import li.cil.oc.server.PacketSender
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundCategory

import scala.collection.mutable

object Sound {
  val globalTimeouts = mutable.WeakHashMap.empty[Location, mutable.Map[String, Long]]

  def play(host: Location, name: String) = this.synchronized {
    globalTimeouts.get(host) match {
      case Some(hostTimeouts) if hostTimeouts.getOrElse(name, 0L) > System.currentTimeMillis() => // Cooldown.
      case _ =>
        PacketSender.sendSound(host.getWorld, host.xPosition, host.yPosition, host.zPosition, new ResourceLocation(Settings.resourceDomain + ":" + name), SoundCategory.BLOCKS, 15 * Settings.get.soundVolume)
        globalTimeouts.getOrElseUpdate(host, mutable.Map.empty) += name -> (System.currentTimeMillis() + 500)
    }
  }

  def playDiskInsert(host: Location) {
    play(host, "floppy_insert")
  }

  def playDiskEject(host: Location) {
    play(host, "floppy_eject")
  }
}
