package li.cil.oc.common

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import java.io
import java.util.logging.Level
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.world.ChunkCoordIntPair
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.event.world.WorldEvent
import scala.collection.mutable

object SaveHandler {
  val saveData = mutable.Map.empty[ChunkCoordIntPair, mutable.Map[String, Array[Byte]]]

  def savePath = new io.File(DimensionManager.getCurrentSaveRootDirectory, Settings.savePath + "state")

  def scheduleSave(chunk: ChunkCoordIntPair, name: String, data: Array[Byte]) = saveData.synchronized {
    if (chunk == null) throw new IllegalArgumentException("chunk is null")
    else saveData.getOrElseUpdate(chunk, mutable.Map.empty[String, Array[Byte]]) += name -> data
  }

  def load(chunk: ChunkCoordIntPair, name: String): Array[Byte] = {
    if (chunk == null) throw new IllegalArgumentException("chunk is null")
    val path = savePath
    val chunkPath = new io.File(path, s"${chunk.chunkXPos}.${chunk.chunkZPos}")
    val file = new io.File(chunkPath, name)
    try {
      // val bis = new io.BufferedInputStream(new GZIPInputStream(new io.FileInputStream(file)))
      val bis = new io.BufferedInputStream(new io.FileInputStream(file))
      val bos = new io.ByteArrayOutputStream
      val buffer = new Array[Byte](8 * 1024)
      var read = 0
      do {
        read = bis.read(buffer)
        if (read > 0) {
          bos.write(buffer, 0, read)
        }
      } while (read >= 0)
      bis.close()
      bos.toByteArray
    }
    catch {
      case e: io.IOException =>
        OpenComputers.log.log(Level.WARNING, "Error loading auxiliary tile entity data.", e)
        Array.empty[Byte]
    }
  }

  // Used by the native lua state to store kernel and stack data in auxiliary
  // files instead of directly in the tile entity data, avoiding potential
  // problems with the tile entity data becoming too large.
  @SubscribeEvent
  def onWorldSave(e: WorldEvent.Save) = saveData.synchronized {
    val path = savePath
    path.mkdirs()
    for ((chunk, entries) <- saveData) {
      val chunkPath = new io.File(path, s"${chunk.chunkXPos}.${chunk.chunkZPos}")
      chunkPath.mkdirs()
      if (chunkPath.exists && chunkPath.isDirectory) {
        for (file <- chunkPath.listFiles()) file.delete()
      }
      for ((name, data) <- entries) {
        val file = new io.File(chunkPath, name)
        try {
          // val fos = new GZIPOutputStream(new io.FileOutputStream(file))
          val fos = new io.BufferedOutputStream(new io.FileOutputStream(file))
          fos.write(data)
          fos.close()
        }
        catch {
          case e: io.IOException => OpenComputers.log.log(Level.WARNING, s"Error saving auxiliary tile entity data to '${file.getAbsolutePath}.", e)
        }
      }
    }
    saveData.clear()
  }
}
