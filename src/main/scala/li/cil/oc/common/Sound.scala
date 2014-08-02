package li.cil.oc.common

import li.cil.oc.Settings
import li.cil.oc.api.driver.Container

import scala.collection.mutable

object Sound {
  val lastPlayed = mutable.WeakHashMap.empty[Container, Long]

  def play(container: Container, name: String) {
    container.world.playSoundEffect(container.xPosition, container.yPosition, container.zPosition, Settings.resourceDomain + ":" + name, Settings.get.soundVolume, 1)
  }

  def playDiskInsert(container: Container) {
    play(container, "floppy_insert")
  }

  def playDiskEject(container: Container) {
    play(container, "floppy_eject")
  }

  def playDiskActivity(container: Container, isFloppy: Boolean) = this.synchronized {
    lastPlayed.get(container) match {
      case Some(time) if time > System.currentTimeMillis() => // Cooldown.
      case _ =>
        if (isFloppy) play(container, "floppy_access")
        else play(container, "hdd_access")
        lastPlayed += container -> (System.currentTimeMillis() + 500)
    }
  }
}
