package li.cil.oc.common

import li.cil.oc.Settings
import li.cil.oc.api.network.EnvironmentHost

import scala.collection.mutable

object Sound {
  val globalTimeouts = mutable.WeakHashMap.empty[EnvironmentHost, mutable.Map[String, Long]]

  def play(host: EnvironmentHost, name: String) = this.synchronized {
    globalTimeouts.get(host) match {
      case Some(hostTimeouts) if hostTimeouts.getOrElse(name, 0L) > System.currentTimeMillis() => // Cooldown.
      case _ =>
        host.world.playSoundEffect(host.xPosition, host.yPosition, host.zPosition, Settings.resourceDomain + ":" + name, Settings.get.soundVolume, 1)
        globalTimeouts.getOrElseUpdate(host, mutable.Map.empty) += name -> (System.currentTimeMillis() + 500)
    }
  }

  def playDiskInsert(host: EnvironmentHost) {
    play(host, "floppy_insert")
  }

  def playDiskEject(host: EnvironmentHost) {
    play(host, "floppy_eject")
  }
}
