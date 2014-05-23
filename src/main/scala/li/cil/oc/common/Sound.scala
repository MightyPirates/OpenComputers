package li.cil.oc.common

import li.cil.oc.Settings
import net.minecraft.tileentity.TileEntity
import scala.collection.mutable

object Sound {
  val lastPlayed = mutable.WeakHashMap.empty[TileEntity, Long]

  def play(t: TileEntity, name: String) {
    t.getWorldObj.playSoundEffect(t.xCoord + 0.5, t.yCoord + 0.5, t.zCoord + 0.5, Settings.resourceDomain + ":" + name, 1, 1)
  }

  def playDiskInsert(t: TileEntity) {
    play(t, "floppy_insert")
  }

  def playDiskEject(t: TileEntity) {
    play(t, "floppy_eject")
  }

  def playDiskActivity(t: TileEntity, isFloppy: Boolean) = this.synchronized {
    lastPlayed.get(t) match {
      case Some(time) if time > System.currentTimeMillis() => // Cooldown.
      case _ =>
        if (isFloppy) play(t, "floppy_access")
        else play(t, "hdd_access")
        lastPlayed += t -> (System.currentTimeMillis() + 500)
    }
  }
}
