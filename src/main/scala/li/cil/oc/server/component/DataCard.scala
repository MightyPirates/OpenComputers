package li.cil.oc.server.component

import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterOutputStream

import com.google.common.hash.Hashing
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.output.ByteArrayOutputStream

class DataCard extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("data", Visibility.Neighbors).
    withConnector().
    create()

  val romData = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/data"), "data"))

  @Callback(direct = true, doc = """function():number -- The maximum size of data that can be passed to other functions of the card.""")
  def getLimit(context: Context, args: Arguments): Array[AnyRef] = {
    result(Settings.get.dataCardHardLimit)
  }

  @Callback(direct = true, limit = 32, doc = """function(data:string):string -- Applies base64 encoding to the data.""")
  def encode64(context: Context, args: Arguments): Array[AnyRef] = {
    result(Base64.encodeBase64(checkLimits(context, args, Settings.get.dataCardComplex)))
  }

  @Callback(direct = true, limit = 32, doc = """function(data:string):string -- Applies base64 decoding to the data.""")
  def decode64(context: Context, args: Arguments): Array[AnyRef] = {
    result(Base64.decodeBase64(checkLimits(context, args, Settings.get.dataCardComplex)))
  }

  @Callback(direct = true, limit = 6, doc = """function(data:string):string -- Applies deflate compression to the data.""")
  def deflate(context: Context, args: Arguments): Array[AnyRef] = {
    val data = checkLimits(context, args, Settings.get.dataCardComplex)
    val baos = new ByteArrayOutputStream(512)
    val deos = new DeflaterOutputStream(baos)
    deos.write(data)
    deos.finish()
    result(baos.toByteArray)
  }

  @Callback(direct = true, limit = 6, doc = """function(data:string):string -- Applies inflate decompression to the data.""")
  def inflate(context: Context, args: Arguments): Array[AnyRef] = {
    val data = checkLimits(context, args, Settings.get.dataCardComplex)
    val baos = new ByteArrayOutputStream(512)
    val inos = new InflaterOutputStream(baos)
    inos.write(data)
    inos.finish()
    result(baos.toByteArray)
  }

  @Callback(direct = true, limit = 32, doc = """function(data:string):string -- Computes SHA2-256 hash of the data. Result is in binary format.""")
  def sha256(context: Context, args: Arguments): Array[AnyRef] = {
    val data = checkLimits(context, args, Settings.get.dataCardSimple)
    result(Hashing.sha256().hashBytes(data).asBytes())
  }

  @Callback(direct = true, limit = 32, doc = """function(data:string):string -- Computes MD5 hash of the data. Result is in binary format""")
  def md5(context: Context, args: Arguments): Array[AnyRef] = {
    val data = checkLimits(context, args, Settings.get.dataCardSimple)
    result(Hashing.md5().hashBytes(data).asBytes())
  }

  @Callback(direct = true, limit = 32, doc = """function(data:string):string -- Computes CRC-32 hash of the data. Result is in binary format""")
  def crc32(context: Context, args: Arguments): Array[AnyRef] = {
    val data = checkLimits(context, args, Settings.get.dataCardSimple)
    result(Hashing.crc32().hashBytes(data).asBytes())
  }

  private def checkLimits(context: Context, args: Arguments, cost: Double): Array[Byte] = {
    val data = args.checkByteArray(0)
    if (data.length > Settings.get.dataCardHardLimit) throw new IllegalArgumentException("data size limit exceeded")
    if (!node.tryChangeBuffer(-cost)) throw new Exception("not enough energy")
    if (data.length > Settings.get.dataCardSoftLimit) context.pause(Settings.get.dataCardTimeout)
    data
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node.isNeighborOf(this.node)) {
      romData.foreach(fs => node.connect(fs.node))
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      romData.foreach(_.node.remove())
    }
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    romData.foreach(_.load(nbt.getCompoundTag("romData")))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    romData.foreach(fs => nbt.setNewCompoundTag("romData", fs.save))
  }
}
