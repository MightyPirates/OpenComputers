package li.cil.oc.server.component

import java.io
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import com.google.common.io.Files
import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.fs.Label
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.CompoundNBT
import net.minecraft.world.storage.FolderName
import net.minecraftforge.fml.server.ServerLifecycleHooks

import scala.collection.convert.ImplicitConversionsToJava._

class Drive(val capacity: Int, val platterCount: Int, val label: Label, host: Option[EnvironmentHost], val sound: Option[String], val speed: Int, val isLocked: Boolean) extends AbstractManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("drive", Visibility.Neighbors).
    withConnector().
    create()

  private def savePath = ServerLifecycleHooks.getCurrentServer.getWorldPath(new FolderName(Settings.savePath + node.address + ".bin")).toFile

  private final val sectorSize = 512

  private val data = new Array[Byte](capacity)

  private val sectorCount = capacity / sectorSize

  private val sectorsPerPlatter = sectorCount / platterCount

  private var headPos = 0

  final val readSectorCosts = Array(1.0 / 10, 1.0 / 20, 1.0 / 30, 1.0 / 40, 1.0 / 50, 1.0 / 60)
  final val writeSectorCosts = Array(1.0 / 5, 1.0 / 10, 1.0 / 15, 1.0 / 20, 1.0 / 25, 1.0 / 30)
  final val readByteCosts = Array(1.0 / 48, 1.0 / 64, 1.0 / 80, 1.0 / 96, 1.0 / 112, 1.0 / 128)
  final val writeByteCosts = Array(1.0 / 24, 1.0 / 32, 1.0 / 40, 1.0 / 48, 1.0 / 56, 1.0 / 64)

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Disk,
    DeviceAttribute.Description -> "Hard disk drive",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> ("MPD" + (capacity / 1024).toString + "L" + platterCount.toString),
    DeviceAttribute.Capacity -> (capacity * 1.024).toInt.toString,
    DeviceAttribute.Size -> capacity.toString,
    DeviceAttribute.Clock -> (((2000 / readSectorCosts(speed)).toInt / 100).toString + "/" + ((2000 / writeSectorCosts(speed)).toInt / 100).toString + "/" + ((2000 / readByteCosts(speed)).toInt / 100).toString + "/" + ((2000 / writeByteCosts(speed)).toInt / 100).toString)
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():string -- Get the current label of the drive.""")
  def getLabel(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    if (label != null) result(label.getLabel) else null
  }

  @Callback(doc = """function(value:string):string -- Sets the label of the drive. Returns the new value, which may be truncated.""")
  def setLabel(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    if (isLocked) throw new Exception("drive is read only")
    if (label == null) throw new Exception("drive does not support labeling")
    if (args.checkAny(0) == null) label.setLabel(null)
    else label.setLabel(args.checkString(0))
    result(label.getLabel)
  }

  @Callback(direct = true, doc = """function():number -- Returns the total capacity of the drive, in bytes.""")
  def getCapacity(context: Context, args: Arguments): Array[AnyRef] = result(capacity)

  @Callback(direct = true, doc = """function():number -- Returns the size of a single sector on the drive, in bytes.""")
  def getSectorSize(context: Context, args: Arguments): Array[AnyRef] = result(sectorSize)

  @Callback(direct = true, doc = """function():number -- Returns the number of platters in the drive.""")
  def getPlatterCount(context: Context, args: Arguments): Array[AnyRef] = result(platterCount)

  @Callback(direct = true, doc = """function(sector:number):string -- Read the current contents of the specified sector.""")
  def readSector(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    context.consumeCallBudget(readSectorCosts(speed))
    val sector = moveToSector(context, checkSector(args, 0))
    diskActivity()
    val sectorData = new Array[Byte](sectorSize)
    Array.copy(data, sectorOffset(sector), sectorData, 0, sectorSize)
    result(sectorData)
  }

  @Callback(direct = true, doc = """function(sector:number, value:string) -- Write the specified contents to the specified sector.""")
  def writeSector(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    if (isLocked) throw new Exception("drive is read only")
    context.consumeCallBudget(writeSectorCosts(speed))
    val sectorData = args.checkByteArray(1)
    val sector = moveToSector(context, checkSector(args, 0))
    diskActivity()
    Array.copy(sectorData, 0, data, sectorOffset(sector), math.min(sectorSize, sectorData.length))
    null
  }

  @Callback(direct = true, doc = """function(offset:number):number -- Read a single byte at the specified offset.""")
  def readByte(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    context.consumeCallBudget(readByteCosts(speed))
    val offset = args.checkInteger(0) - 1
    moveToSector(context, checkSector(offset))
    diskActivity()
    result(data(offset))
  }

  @Callback(direct = true, doc = """function(offset:number, value:number) -- Write a single byte to the specified offset.""")
  def writeByte(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    if (isLocked) throw new Exception("drive is read only")
    context.consumeCallBudget(writeByteCosts(speed))
    val offset = args.checkInteger(0) - 1
    val value = args.checkInteger(1).toByte
    moveToSector(context, checkSector(offset))
    diskActivity()
    data(offset) = value
    null
  }

  // ----------------------------------------------------------------------- //

  private final val HeadPosTag = "headPos"

  override def loadData(nbt: CompoundNBT): Unit = this.synchronized {
    super.loadData(nbt)

    if (node.address != null) try {
      val path = savePath
      if (path.exists()) {
        val bin = new ByteArrayInputStream(Files.toByteArray(path))
        val zin = new GZIPInputStream(bin)
        var offset = 0
        var read = 0
        while (read >= 0 && offset < data.length) {
          read = zin.read(data, offset, data.length - offset)
          offset += read
        }
      }
    }
    catch {
      case t: Throwable => OpenComputers.log.warn(s"Failed loading drive contents for '${node.address}'.", t)
    }

    headPos = nbt.getInt(HeadPosTag) max 0 min sectorToHeadPos(sectorCount)

    if (label != null) {
      label.loadData(nbt)
    }
  }

  override def saveData(nbt: CompoundNBT): Unit = this.synchronized {
    super.saveData(nbt)

    if (node.address != null) try {
      val path = savePath
      path.getParentFile.mkdirs()
      val bos = new ByteArrayOutputStream()
      val zos = new GZIPOutputStream(bos)
      zos.write(data)
      zos.close()
      Files.write(bos.toByteArray, path)
    }
    catch {
      case t: Throwable => OpenComputers.log.warn(s"Failed saving drive contents for '${node.address}'.", t)
    }

    nbt.putInt(HeadPosTag, headPos)

    if (label != null) {
      label.saveData(nbt)
    }
  }

  // ----------------------------------------------------------------------- //

  private def validateSector(sector: Int) = {
    if (sector < 0 || sector >= sectorCount)
      throw new IllegalArgumentException("invalid offset, not in a usable sector")
    sector
  }

  private def checkSector(offset: Int) = validateSector(offsetSector(offset))

  private def checkSector(args: Arguments, n: Int) = validateSector(args.checkInteger(n) - 1)

  private def moveToSector(context: Context, sector: Int) = {
    val newHeadPos = sectorToHeadPos(sector)
    if (headPos != newHeadPos) {
      val delta = math.abs(headPos - newHeadPos)
      if (delta > Settings.get.sectorSeekThreshold) context.pause(Settings.get.sectorSeekTime)
      headPos = newHeadPos
    }
    sector
  }

  private def sectorToHeadPos(sector: Int) = sector % sectorsPerPlatter

  private def sectorOffset(sector: Int) = sector * sectorSize

  private def offsetSector(offset: Int) = offset / sectorSize

  private def diskActivity() {
    (sound, host) match {
      case (Some(s), Some(h)) => ServerPacketSender.sendFileSystemActivity(node, h, s)
      case _ =>
    }
  }
}
