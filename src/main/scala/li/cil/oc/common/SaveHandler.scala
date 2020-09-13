package li.cil.oc.common

import java.io
import java.io._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import cpw.mods.fml.common.eventhandler.EventPriority
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.SafeThreadPool
import li.cil.oc.util.ThreadPoolFactory
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.World
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.event.world.WorldEvent
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

  class SaveDataEntry(val data: Array[Byte], val pos: ChunkCoordIntPair, val name: String, val dimension: Int) extends Runnable {
    override def run(): Unit = {
      val path = statePath
      val dimPath = new io.File(path, dimension.toString)
      val chunkPath = new io.File(dimPath, s"${this.pos.chunkXPos}.${this.pos.chunkZPos}")
      chunkDirs.add(chunkPath)
      if (!chunkPath.exists()) {
        chunkPath.mkdirs()
      }
      val file = new io.File(chunkPath, this.name)
      try {
        // val fos = new GZIPOutputStream(new io.FileOutputStream(file))
        val fos = new io.BufferedOutputStream(new io.FileOutputStream(file))
        fos.write(this.data)
        fos.close()
      }
      catch {
        case e: io.IOException => OpenComputers.log.warn(s"Error saving auxiliary tile entity data to '${file.getAbsolutePath}.", e)
      }
    }
  }

  val stateSaveHandler: SafeThreadPool = ThreadPoolFactory.createSafePool("SaveHandler", 1)

  val chunkDirs = new ConcurrentLinkedDeque[io.File]()
  val saving = mutable.HashMap.empty[String, Future[_]]

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
    val dimension = world.provider.dimensionId
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

    // Wait for the latest save task for the requested file to complete.
    // This prevents the chance of loading an outdated version
    // of this file.
    saving.get(name).foreach(f => try {
      f.get(120L, TimeUnit.SECONDS)
    } catch {
      case e: TimeoutException => OpenComputers.log.warn("Waiting for state data to save took two minutes! Aborting.")
      case e: CancellationException => // NO-OP
    })
    saving.remove(name)

    load(dimension, chunk, name)
  }

  def scheduleSave(dimension: Int, chunk: ChunkCoordIntPair, name: String, data: Array[Byte]): Unit = {
    if (chunk == null) throw new IllegalArgumentException("chunk is null")
    else {
      // Disregarding whether or not there already was a
      // save submitted for the requested file
      // allows for better concurrency at the cost of
      // doing more writing operations.
      stateSaveHandler.withPool(_.submit(new SaveDataEntry(data, chunk, name, dimension))).foreach(saving.put(name, _))
    }
  }

  def load(dimension: Int, chunk: ChunkCoordIntPair, name: String): Array[Byte] = {
    if (chunk == null) throw new IllegalArgumentException("chunk is null")

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

  def cleanSaveData(): Unit = {
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

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  def onWorldLoad(e: WorldEvent.Load) {
    // Touch all externally saved data when loading, to avoid it getting
    // deleted in the next save (because the now - save time will usually
    // be larger than the time out after loading a world again).
    if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_7)) SaveHandlerJava17Functionality.visitJava17(statePath)
    else visitJava16()
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
    stateSaveHandler.withPool(_.submit(new Runnable {
      override def run(): Unit = cleanSaveData()
    }))
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
