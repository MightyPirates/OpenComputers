package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Settings
import li.cil.oc.api.event.FileSystemAccessEvent

object FileAccessHandler {
  @SubscribeEvent
  def onFileSystemAccess(e: FileSystemAccessEvent) {
    val volume = Settings.get.soundVolume
    e.world.playSound(e.x, e.y, e.z, e.sound, volume, 1, false)
  }
}
