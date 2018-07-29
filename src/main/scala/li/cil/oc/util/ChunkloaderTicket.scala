package li.cil.oc.util

import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.util.ChunkCoordinates
import net.minecraft.world.{ChunkCoordIntPair, World}
import net.minecraftforge.common.ForgeChunkManager

class ChunkloaderTicket(private val fcmTicket: ForgeChunkManager.Ticket) {
  val data = fcmTicket.getModData
  val address = data.getString("address")
  val dim = fcmTicket.world.provider.dimensionId
  var unchecked: Boolean = false
  def ownerName = Option(fcmTicket.getPlayerName)
  def chunkCoord = new ChunkCoordIntPair(blockCoord.posX >> 4, blockCoord.posZ >> 4)
  def blockCoord = new ChunkCoordinates(data.getInteger("x"), data.getInteger("y"), data.getInteger("z"))
  def blockCoord_=(coord: ChunkCoordinates) {
    data.setInteger("x", coord.posX)
    data.setInteger("y", coord.posY)
    data.setInteger("z", coord.posZ)
  }
  def chunkList = fcmTicket.getChunkList

  def forceChunk(chunkCoord: ChunkCoordIntPair) {
    if (Settings.get.chunkloaderLogLevel > 1)
      OpenComputers.log.info(s"[chunkloader] Force chunk $chunkCoord: $this")
    ForgeChunkManager.forceChunk(fcmTicket, chunkCoord)
  }

  def unforceChunk(chunkCoord: ChunkCoordIntPair) {
    if (Settings.get.chunkloaderLogLevel > 1)
      OpenComputers.log.info(s"[chunkloader] Unforce chunk $chunkCoord: $this")
    ForgeChunkManager.unforceChunk(fcmTicket, chunkCoord)
  }

  def release(): Unit = {
    try ForgeChunkManager.releaseTicket(fcmTicket) catch {
      case _: Throwable => // Ignored.
    }
  }

  override def toString = {
    val sAddress = s"$address"
    val sCoord = s", $blockCoord$chunkCoord/$dim"
    val sOwner = if (ownerName.isDefined) s", owned by ${ownerName.get}" else ""
    s"ticket{$sAddress$sCoord$sOwner}"
  }
}

object  ChunkloaderTicket {

  def requestPlayerTicket(world: World, address: String, ownerName: String) = {
    Option(ForgeChunkManager.requestPlayerTicket(OpenComputers, ownerName, world, ForgeChunkManager.Type.NORMAL))
    .map(fcmTicket => {
      fcmTicket.getModData.setString("address", address)
      fcmTicket.getModData.setString("ownerName", ownerName)
      new ChunkloaderTicket(fcmTicket)
    })
  }

  def requestTicket(world: World, address: String) = {
    Option(ForgeChunkManager.requestTicket(OpenComputers, world, ForgeChunkManager.Type.NORMAL))
      .map(fcmTicket =>{
        fcmTicket.getModData.setString("address", address)
        new ChunkloaderTicket(fcmTicket)
      })
  }
}