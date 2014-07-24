package li.cil.oc.common

import java.io
import java.io.{File, FileFilter}

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.{OpenComputers, Settings}
import net.minecraft.world.ChunkCoordIntPair
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.event.world.{ChunkDataEvent, WorldEvent}

import scala.collection.mutable

// Used by the native lua state to store kernel and stack data in auxiliary
// files instead of directly in the tile entity data, avoiding potential
// problems with the tile entity data becoming too large.
object SaveHandler {
  private val uuidRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"

  val saveData = mutable.Map.empty[Int, mutable.Map[ChunkCoordIntPair, mutable.Map[String, Array[Byte]]]]

  def savePath = new io.File(DimensionManager.getCurrentSaveRootDirectory, Settings.savePath)

  def statePath = new io.File(savePath, "state")

  def scheduleSave(dimension: Int, chunk: ChunkCoordIntPair, name: String, data: Array[Byte]) = saveData.synchronized {
    if (chunk == null) throw new IllegalArgumentException("chunk is null")
    else {
      // Make sure we get rid of old versions (e.g. left over by other mods
      // triggering a save - this is mostly used for RiM compatibility). We
      // need to do this for *each* dimension, in case computers are teleported
      // across dimensions.
      for (chunks <- saveData.values) chunks.values.foreach(_ -= name)
      val chunks = saveData.getOrElseUpdate(dimension, mutable.Map.empty)
      chunks.getOrElseUpdate(chunk, mutable.Map.empty) += name -> data
    }
  }

  def load(dimension: Int, chunk: ChunkCoordIntPair, name: String): Array[Byte] = {
    if (chunk == null) throw new IllegalArgumentException("chunk is null")
    // Use data from 'cache' if possible. This avoids weird things happening
    // when writeToNBT+readFromNBT is called by other mods (i.e. this is not
    // used to actually save the data to disk).
    saveData.get(dimension) match {
      case Some(chunks) => chunks.get(chunk) match {
        case Some(map) => map.get(name) match {
          case Some(data) => return data
          case _ =>
        }
        case _ =>
      }
      case _ =>
    }
    val path = statePath
    val dimPath = new io.File(path, dimension.toString)
    val chunkPath = new io.File(dimPath, s"${chunk.chunkXPos}.${chunk.chunkZPos}")
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
        OpenComputers.log.warn("Error loading auxiliary tile entity data.", e)
        Array.empty[Byte]
    }
  }

  @SubscribeEvent
  def onChunkSave(e: ChunkDataEvent.Save) = saveData.synchronized {
    val path = statePath
    val dimension = e.world.provider.dimensionId
    val chunk = e.getChunk.getChunkCoordIntPair
    val dimPath = new io.File(path, dimension.toString)
    val chunkPath = new io.File(dimPath, s"${chunk.chunkXPos}.${chunk.chunkZPos}")
    if (chunkPath.exists && chunkPath.isDirectory) {
      for (file <- chunkPath.listFiles()) file.delete()
    }
    saveData.get(dimension) match {
      case Some(chunks) => chunks.get(chunk) match {
        case Some(entries) =>
          chunkPath.mkdirs()
          for ((name, data) <- entries) {
            val file = new io.File(chunkPath, name)
            try {
              // val fos = new GZIPOutputStream(new io.FileOutputStream(file))
              val fos = new io.BufferedOutputStream(new io.FileOutputStream(file))
              fos.write(data)
              fos.close()
            }
            catch {
              case e: io.IOException => OpenComputers.log.warn(s"Error saving auxiliary tile entity data to '${file.getAbsolutePath}.", e)
            }
          }
        case _ => chunkPath.delete()
      }
      case _ =>
    }
  }

  @SubscribeEvent
  def onWorldSave(e: WorldEvent.Save) {
    saveData.synchronized {
      saveData.get(e.world.provider.dimensionId) match {
        case Some(chunks) => chunks.clear()
        case _ =>
      }
    }

    // Delete empty folders that match a drive UUID to keep the state folder clean.
    val emptyDirs = savePath.listFiles(new FileFilter {
      override def accept(file: File) = file.getName.matches(uuidRegex) && file.isDirectory && {
        val list = file.list()
        list == null || list.length == 0
      }
    })
    if (emptyDirs != null) {
      emptyDirs.filter(_ != null).foreach(_.delete())
    }
  }
}
