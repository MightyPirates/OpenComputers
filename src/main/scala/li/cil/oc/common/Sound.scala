package li.cil.oc.common

import li.cil.oc.Settings
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.client.event.sound.SoundLoadEvent
import net.minecraftforge.event.ForgeSubscribe
import cpw.mods.fml.relauncher.{Side, SideOnly}

object Sound {
  @SideOnly(Side.CLIENT)
  @ForgeSubscribe
  def onSoundLoad(event: SoundLoadEvent) {
    for (i <- 1 to 6) {
      event.manager.soundPoolSounds.addSound(Settings.resourceDomain + s":floppy_access$i.ogg")
    }
    event.manager.soundPoolSounds.addSound(Settings.resourceDomain + ":floppy_insert.ogg")
    event.manager.soundPoolSounds.addSound(Settings.resourceDomain + ":floppy_eject.ogg")
  }

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
    t match {
      case computer: tileentity.Computer => play(computer, "hdd_access")
      case rack: tileentity.Rack => play(rack, "hdd_access")
      case drive: tileentity.DiskDrive => play(drive, "floppy_access")
      case _ => // Huh?
    }
  }

}
