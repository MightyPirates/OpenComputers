package li.cil.oc.common

import li.cil.oc.Settings
import li.cil.oc.api.driver.EnvironmentHost

import scala.collection.mutable

object Sound {
  val lastPlayed = mutable.WeakHashMap.empty[EnvironmentHost, Long]

  def play(host: EnvironmentHost, name: String) {
    host.world.playSoundEffect(host.xPosition, host.yPosition, host.zPosition, Settings.resourceDomain + ":" + name, Settings.get.soundVolume, 1)
  }

  def playDiskInsert(host: EnvironmentHost) {
    play(host, "floppy_insert")
  }

  def playDiskEject(host: EnvironmentHost) {
    play(host, "floppy_eject")
  }

  def playDiskActivity(host: EnvironmentHost, sound: String) = this.synchronized {
    lastPlayed.get(host) match {
      case Some(time) if time > System.currentTimeMillis() => // Cooldown.
      case _ =>
        play(host, sound)
        lastPlayed += host -> (System.currentTimeMillis() + 500)
    }
  }
}
