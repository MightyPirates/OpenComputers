package li.cil.oc.common

import java.io
import java.io._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.util.BlockPosition
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.World
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.event.world.ChunkDataEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.commons.lang3.JavaVersion
import org.apache.commons.lang3.SystemUtils

import scala.collection.mutable

// Used by the native lua state to store kernel and stack data in auxiliary
// files instead of directly in the tile entity data, avoiding potential
// problems with the tile entity data becoming too large.
object SaveHandler {
  private val uuidRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"

  private val TimeToHoldOntoOldSaves = 60 * 1000

  // THIS IS A MASSIVE HACK OF THE UGLIEST KINDS.
  // But it works, and the alternative would be to change the Persistable
  // interface to pass along this state to *everything that gets saved ever*,
  // which in 99% of the cases it doesn't need to know. So yes, this is fugly,
  // but the "clean" solution would be no less fugly.
  // Why is this even required? To avoid flushing file systems to disk and
  // avoid persisting machine states when sending description packets to clients,
  // which takes a lot of time and is completely unnecessary in those cases.
  var savingForClients = false

  val saveData = mutable.Map.empty[Int, mutable.Map[ChunkCoordIntPair, mutable.Map[String, Array[Byte]]]]

  def savePath = new io.File(DimensionManager.getCurrentSaveRootDirectory, Settings.savePath)

  def statePath = new io.File(savePath, "state")

  def scheduleSave(host: MachineHost, nbt: NBTTagCompound, name: String, data: Array[Byte]) {
    scheduleSave(BlockPosition(host), nbt, name, data)
  }

  def scheduleSave(host: MachineHost, nbt: NBTTagCompound, name: String, save: NBTTagCompound => Unit) {
    scheduleSave(host, nbt, name, writeNBT(save))
  }

  def scheduleSave(host: EnvironmentHost, nbt: NBTTagCompound, name: String, save: NBTTagCompound => Unit) {
    scheduleSave(BlockPosition(host), nbt, name, writeNBT(save))
  }

  def scheduleSave(world: World, x: Double, z: Double, nbt: NBTTagCompound, name: String, data: Array[Byte]) {
    scheduleSave(BlockPosition(x, 0, z, world), nbt, name, data)
  }

  def scheduleSave(world: World, x: Double, z: Double, nbt: NBTTagCompound, name: String, save: NBTTagCompound => Unit) {
    scheduleSave(world, x, z, nbt, name, writeNBT(save))
  }

  def scheduleSave(position: BlockPosition, nbt: NBTTagCompound, name: String, data: Array[Byte]) {
    val world = position.world.get
    val dimension = world.provider.getDimension
    val chunk = new ChunkCoordIntPair(position.x >> 4, position.z >> 4)

    // We have to save the dimension and chunk coordinates, because they are
    // not available on load / may have changed if the computer was moved.
    nbt.setInteger("dimension", dimension)
    nbt.setInteger("chunkX", chunk.chunkXPos)
    nbt.setInteger("chunkZ", chunk.chunkZPos)

    scheduleSave(dimension, chunk, name, data)
  }

  private def writeNBT(save: NBTTagCompound => Unit) = {
    val tmpNbt = new NBTTagCompound()
    save(tmpNbt)
    val baos = new ByteArrayOutputStream()
    val dos = new DataOutputStream(baos)
    CompressedStreamTools.write(tmpNbt, dos)
    baos.toByteArray
  }

  def loadNBT(nbt: NBTTagCompound, name: String): NBTTagCompound = {
    val data = load(nbt, name)
    if (data.length > 0) try {
      val bais = new ByteArrayInputStream(data)
      val dis = new DataInputStream(bais)
      CompressedStreamTools.read(dis)
    }
    catch {
      case t: Throwable =>
        OpenComputers.log.warn("There was an error trying to restore a block's state from external data. This indicates that data was somehow corrupted.", t)
        new NBTTagCompound()
    }
    else new NBTTagCompound()
  }

  def load(nbt: NBTTagCompound, name: String): Array[Byte] = {
    // Since we have no world yet, we rely on the dimension we were saved in.
    // Same goes for the chunk. This also works around issues with computers
    // being moved (e.g. Redstone in Motion).
    val dimension = nbt.getInteger("dimension")
    val chunk = new ChunkCoordIntPair(nbt.getInteger("chunkX"), nbt.getInteger("chunkZ"))

    load(dimension, chunk, name)
  }

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
    if (!file.exists()) return Array.empty[Byte]
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
    val dimension = e.getWorld.provider.getDimension
    val chunk = e.getChunk.getChunkCoordIntPair
    val dimPath = new io.File(path, dimension.toString)
    val chunkPath = new io.File(dimPath, s"${chunk.chunkXPos}.${chunk.chunkZPos}")
    if (chunkPath.exists && chunkPath.isDirectory && chunkPath.list() != null) {
      for (file <- chunkPath.listFiles() if System.currentTimeMillis() - file.lastModified() > TimeToHoldOntoOldSaves) file.delete()
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

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  def onWorldLoad(e: WorldEvent.Load) {
    if (!e.getWorld.isRemote) {
      // Touch all externally saved data when loading, to avoid it getting
      // deleted in the next save (because the now - save time will usually
      // be larger than the time out after loading a world again).
      if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_7)) SaveHandlerJava17Functionality.visitJava17(statePath)
      else visitJava16()
    }
  }

  private def visitJava16() {
    // This may run into infinite loops if there are evil symlinks.
    // But that's really not something I'm bothered by, it's a fallback.
    def recurse(file: File) {
      file.setLastModified(System.currentTimeMillis())
      if (file.exists() && file.isDirectory && file.list() != null) file.listFiles().foreach(recurse)
    }
    recurse(statePath)
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  def onWorldSave(e: WorldEvent.Save) {
    saveData.synchronized {
      saveData.get(e.getWorld.provider.getDimension) match {
        case Some(chunks) => chunks.clear()
        case _ =>
      }
    }

    // Delete empty folders to keep the state folder clean.
    val emptyDirs = savePath.listFiles(new FileFilter {
      override def accept(file: File) = file.isDirectory &&
        // Make sure we only consider file system folders (UUID).
        file.getName.matches(uuidRegex) &&
        // We set the modified time in the save() method of unbuffered file
        // systems, to avoid deleting in-use folders here.
        System.currentTimeMillis() - file.lastModified() > TimeToHoldOntoOldSaves && {
        val list = file.list()
        list == null || list.isEmpty
      }
    })
    if (emptyDirs != null) {
      emptyDirs.filter(_ != null).foreach(_.delete())
    }
  }
}

object SaveHandlerJava17Functionality {
  def visitJava17(statePath: File) {
    Files.walkFileTree(statePath.toPath, new FileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes) = {
        file.toFile.setLastModified(System.currentTimeMillis())
        FileVisitResult.CONTINUE
      }

      override def visitFileFailed(file: Path, exc: IOException) = FileVisitResult.CONTINUE

      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = FileVisitResult.CONTINUE

      override def postVisitDirectory(dir: Path, exc: IOException) = FileVisitResult.CONTINUE
    })
  }
}
