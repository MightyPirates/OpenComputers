package li.cil.oc.common

import li.cil.oc.Settings
import net.minecraft.tileentity.TileEntity
import scala.collection.mutable

object Sound {
  val lastPlayed = mutable.WeakHashMap.empty[TileEntity, Long]

  def play(t: tileentity.TileEntity, name: String) {
    t.world.playSoundEffect(t.x + 0.5, t.y + 0.5, t.z + 0.5, Settings.resourceDomain + ":" + name, 1, 1)
  }

  def playDiskInsert(t: tileentity.DiskDrive) {
    play(t, "floppy_insert")
  }

  def playDiskEject(t: tileentity.DiskDrive) {
    play(t, "floppy_eject")
  }

  def playDiskActivity(t: TileEntity) = this.synchronized {
    lastPlayed.get(t) match {
      case Some(time) if time > System.currentTimeMillis() => // Cooldown.
      case _ =>
        t match {
          case computer: tileentity.Computer => play(computer, "hdd_access")
          case rack: tileentity.Rack => play(rack, "hdd_access")
          case drive: tileentity.DiskDrive => play(drive, "floppy_access")
          case _ => // Huh?
        }
        lastPlayed += t -> (System.currentTimeMillis() + 500)
    }
  }

}
