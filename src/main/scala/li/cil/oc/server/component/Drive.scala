package li.cil.oc.server.component

import java.io
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import com.google.common.io.Files
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.fs.Label
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.DimensionManager

class Drive(val capacity: Int, val platterCount: Int, val label: Label, host: Option[EnvironmentHost], val sound: Option[String]) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("drive", Visibility.Neighbors).
    withConnector().
    create()

  private def savePath = new io.File(DimensionManager.getCurrentSaveRootDirectory, Settings.savePath + node.address + ".bin")

  private final val sectorSize = 512

  private val data = new Array[Byte](capacity)

  private val sectorCount = capacity / sectorSize

  private val sectorsPerPlatter = sectorCount / platterCount

  private var headPos = 0

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():string -- Get the current label of the drive.""")
  def getLabel(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    if (label != null) result(label.getLabel) else null
  }

  @Callback(doc = """function(value:string):string -- Sets the label of the drive. Returns the new value, which may be truncated.""")
  def setLabel(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
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

  def readSector(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val sector = moveToSector(context, checkSector(args, 0))
    diskActivity()
    val sectorData = new Array[Byte](sectorSize)
    Array.copy(data, sectorOffset(sector), sectorData, 0, sectorSize)
    result(sectorData)
  }

  def writeSector(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val sectorData = args.checkByteArray(1)
    val sector = moveToSector(context, checkSector(args, 0))
    diskActivity()
    Array.copy(sectorData, 0, data, sectorOffset(sector), math.min(sectorSize, sectorData.length))
    null
  }

  def readByte(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val offset = args.checkInteger(0) - 1
    moveToSector(context, checkSector(offset))
    diskActivity()
    result(data(offset))
  }

  def writeByte(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val offset = args.checkInteger(0) - 1
    val value = args.checkInteger(1).toByte
    moveToSector(context, checkSector(offset))
    diskActivity()
    data(offset) = value
    null
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = this.synchronized {
    super.load(nbt)

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

    headPos = nbt.getInteger("headPos") max 0 min sectorToHeadPos(sectorCount)

    if (label != null) {
      label.load(nbt)
    }
  }

  override def save(nbt: NBTTagCompound) = this.synchronized {
    super.save(nbt)

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

    nbt.setInteger("headPos", headPos)

    if (label != null) {
      label.save(nbt)
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

object Drive {
  // I really need to come up with a way to make the call limit dynamic...
  def apply(capacity: Int, platterCount: Int, label: Label, host: Option[EnvironmentHost], sound: Option[String], speed: Int = 1): Drive = speed match {
    case 6 => new Drive(capacity, platterCount, label, host, sound) {
      @Callback(direct = true, limit = 60, doc = """function(sector:number):string -- Read the current contents of the specified sector.""")
      override def readSector(context: Context, args: Arguments): Array[AnyRef] = super.readSector(context, args)

      @Callback(direct = true, limit = 30, doc = """function(sector:number, value:string) -- Write the specified contents to the specified sector.""")
      override def writeSector(context: Context, args: Arguments): Array[AnyRef] = super.writeSector(context, args)

      @Callback(direct = true, limit = 128, doc = """function(offset:number):number -- Read a single byte at the specified offset.""")
      override def readByte(context: Context, args: Arguments): Array[AnyRef] = super.readByte(context, args)

      @Callback(direct = true, limit = 64, doc = """function(offset:number, value:number) -- Write a single byte to the specified offset.""")
      override def writeByte(context: Context, args: Arguments): Array[AnyRef] = super.writeByte(context, args)
    }
    case 5 => new Drive(capacity, platterCount, label, host, sound) {
      @Callback(direct = true, limit = 50, doc = """function(sector:number):string -- Read the current contents of the specified sector.""")
      override def readSector(context: Context, args: Arguments): Array[AnyRef] = super.readSector(context, args)

      @Callback(direct = true, limit = 25, doc = """function(sector:number, value:string) -- Write the specified contents to the specified sector.""")
      override def writeSector(context: Context, args: Arguments): Array[AnyRef] = super.writeSector(context, args)

      @Callback(direct = true, limit = 112, doc = """function(offset:number):number -- Read a single byte at the specified offset.""")
      override def readByte(context: Context, args: Arguments): Array[AnyRef] = super.readByte(context, args)

      @Callback(direct = true, limit = 56, doc = """function(offset:number, value:number) -- Write a single byte to the specified offset.""")
      override def writeByte(context: Context, args: Arguments): Array[AnyRef] = super.writeByte(context, args)
    }
    case 4 => new Drive(capacity, platterCount, label, host, sound) {
      @Callback(direct = true, limit = 40, doc = """function(sector:number):string -- Read the current contents of the specified sector.""")
      override def readSector(context: Context, args: Arguments): Array[AnyRef] = super.readSector(context, args)

      @Callback(direct = true, limit = 20, doc = """function(sector:number, value:string) -- Write the specified contents to the specified sector.""")
      override def writeSector(context: Context, args: Arguments): Array[AnyRef] = super.writeSector(context, args)

      @Callback(direct = true, limit = 96, doc = """function(offset:number):number -- Read a single byte at the specified offset.""")
      override def readByte(context: Context, args: Arguments): Array[AnyRef] = super.readByte(context, args)

      @Callback(direct = true, limit = 48, doc = """function(offset:number, value:number) -- Write a single byte to the specified offset.""")
      override def writeByte(context: Context, args: Arguments): Array[AnyRef] = super.writeByte(context, args)
    }
    case 3 => new Drive(capacity, platterCount, label, host, sound) {
      @Callback(direct = true, limit = 30, doc = """function(sector:number):string -- Read the current contents of the specified sector.""")
      override def readSector(context: Context, args: Arguments): Array[AnyRef] = super.readSector(context, args)

      @Callback(direct = true, limit = 15, doc = """function(sector:number, value:string) -- Write the specified contents to the specified sector.""")
      override def writeSector(context: Context, args: Arguments): Array[AnyRef] = super.writeSector(context, args)

      @Callback(direct = true, limit = 80, doc = """function(offset:number):number -- Read a single byte at the specified offset.""")
      override def readByte(context: Context, args: Arguments): Array[AnyRef] = super.readByte(context, args)

      @Callback(direct = true, limit = 40, doc = """function(offset:number, value:number) -- Write a single byte to the specified offset.""")
      override def writeByte(context: Context, args: Arguments): Array[AnyRef] = super.writeByte(context, args)
    }
    case 2 => new Drive(capacity, platterCount, label, host, sound) {
      @Callback(direct = true, limit = 20, doc = """function(sector:number):string -- Read the current contents of the specified sector.""")
      override def readSector(context: Context, args: Arguments): Array[AnyRef] = super.readSector(context, args)

      @Callback(direct = true, limit = 10, doc = """function(sector:number, value:string) -- Write the specified contents to the specified sector.""")
      override def writeSector(context: Context, args: Arguments): Array[AnyRef] = super.writeSector(context, args)

      @Callback(direct = true, limit = 64, doc = """function(offset:number):number -- Read a single byte at the specified offset.""")
      override def readByte(context: Context, args: Arguments): Array[AnyRef] = super.readByte(context, args)

      @Callback(direct = true, limit = 32, doc = """function(offset:number, value:number) -- Write a single byte to the specified offset.""")
      override def writeByte(context: Context, args: Arguments): Array[AnyRef] = super.writeByte(context, args)
    }
    case _ => new Drive(capacity, platterCount, label, host, sound) {
      @Callback(direct = true, limit = 10, doc = """function(sector:number):string -- Read the current contents of the specified sector.""")
      override def readSector(context: Context, args: Arguments): Array[AnyRef] = super.readSector(context, args)

      @Callback(direct = true, limit = 5, doc = """function(sector:number, value:string) -- Write the specified contents to the specified sector.""")
      override def writeSector(context: Context, args: Arguments): Array[AnyRef] = super.writeSector(context, args)

      @Callback(direct = true, limit = 48, doc = """function(offset:number):number -- Read a single byte at the specified offset.""")
      override def readByte(context: Context, args: Arguments): Array[AnyRef] = super.readByte(context, args)

      @Callback(direct = true, limit = 24, doc = """function(offset:number, value:number) -- Write a single byte to the specified offset.""")
      override def writeByte(context: Context, args: Arguments): Array[AnyRef] = super.writeByte(context, args)
    }
  }
}
