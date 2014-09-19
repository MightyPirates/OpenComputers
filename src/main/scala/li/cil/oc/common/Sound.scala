package li.cil.oc.common

import li.cil.oc.Settings
import li.cil.oc.api.driver.Host

import scala.collection.mutable

object Sound {
  val lastPlayed = mutable.WeakHashMap.empty[Host, Long]

  def play(host: Host, name: String) {
    host.world.playSoundEffect(host.xPosition, host.yPosition, host.zPosition, Settings.resourceDomain + ":" + name, Settings.get.soundVolume, 1)
  }

  def playDiskInsert(host: Host) {
    play(host, "floppy_insert")
  }

  def playDiskEject(host: Host) {
    play(host, "floppy_eject")
  }

  def playDiskActivity(host: Host, isFloppy: Boolean) = this.synchronized {
    lastPlayed.get(host) match {
      case Some(time) if time > System.currentTimeMillis() => // Cooldown.
      case _ =>
        if (isFloppy) play(host, "floppy_access")
        else play(host, "hdd_access")
        lastPlayed += host -> (System.currentTimeMillis() + 500)
    }
  }
}
