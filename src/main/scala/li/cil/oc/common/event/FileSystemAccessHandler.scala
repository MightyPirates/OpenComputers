package li.cil.oc.common.event

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Settings
import li.cil.oc.api.event.FileSystemAccessEvent
import li.cil.oc.common.tileentity.Case
import li.cil.oc.common.tileentity.DiskDrive
import li.cil.oc.common.tileentity.Raid
import li.cil.oc.common.tileentity.ServerRack

object FileSystemAccessHandler {
  @SubscribeEvent
  def onFileSystemAccess(e: FileSystemAccessEvent.Server) {
    e.getTileEntity match {
      case t: ServerRack =>
        val serverSlot = t.servers.indexWhere {
          case Some(server) => server.componentSlot(e.getNode.address) >= 0
          case _ => false
        }
        if (serverSlot < 0) e.setCanceled(true)
        else e.getData.setInteger("server", serverSlot)
      case _ =>
    }
  }

  @SubscribeEvent
  def onFileSystemAccess(e: FileSystemAccessEvent.Client) {
    val volume = Settings.get.soundVolume
    e.getWorld.playSound(e.getX, e.getY, e.getZ, e.getSound, volume, 1, false)
    e.getTileEntity match {
      case t: DiskDrive => t.lastAccess = System.currentTimeMillis()
      case t: Case => t.lastAccess = System.currentTimeMillis()
      case t: Raid => t.lastAccess = System.currentTimeMillis()
      case t: ServerRack => t.lastAccess(e.getData.getInteger("server")) = System.currentTimeMillis()
      case _ =>
    }
  }
}
