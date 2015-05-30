package li.cil.oc.server.component

import java.security.MessageDigest
import java.util.zip.{InflaterOutputStream, DeflaterOutputStream}

import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, OpenComputers, api}
import li.cil.oc.api.machine.{Arguments, Context, Callback}
import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.api.{Network, prefab}
import net.minecraft.nbt.NBTTagCompound
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.output.ByteArrayOutputStream

class DataCard extends prefab.ManagedEnvironment{
  override val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("data", Visibility.Neighbors).
    withConnector().
    create()

  val romData = Option(api.FileSystem.asManagedEnvironment(api.FileSystem.
    fromClass(OpenComputers.getClass, Settings.resourceDomain, "lua/component/data"), "data"))
  
  @Callback(direct = true, doc = """function(data:string):string -- Applies base64 encoding to the data.""")
  def encode64(context: Context, args: Arguments): Array[AnyRef] = {
    result(Base64.encodeBase64(args.checkByteArray(0)))
  }

  @Callback(direct = true, doc = """function(data:string):string -- Applies base64 decoding to the data.""")
  def decode64(context: Context, args: Arguments): Array[AnyRef] = {
    result(Base64.decodeBase64(args.checkByteArray(0)))
  }

  @Callback(direct = true, doc = """function(data:string):string -- Applies deflate compression to the data.""")
  def deflate(context: Context, args: Arguments): Array[AnyRef] = {
    val baos = new ByteArrayOutputStream(512)
    val deos = new DeflaterOutputStream(baos)
    deos.write(args.checkByteArray(0))
    deos.finish()
    result(baos.toByteArray)
  }

  @Callback(direct = true, doc = """function(data:string):string -- Applies inflate decompression to the data.""")
  def inflate(context: Context, args: Arguments): Array[AnyRef] = {
    val baos = new ByteArrayOutputStream(512)
    val inos = new InflaterOutputStream(baos)
    inos.write(args.checkByteArray(0))
    inos.finish()
    result(baos.toByteArray)
  }

  @Callback(direct = true, doc = """function(data:string):string -- Computes SHA2-256 hash of the data.""")
  def sha256(context: Context, args: Arguments): Array[AnyRef] = {
    result(Helper.sha256(args.checkByteArray(0)))
  }

  private object Helper {
    val sha256asher = MessageDigest.getInstance("SHA-256")
    def sha256(data: Array[Byte]) = sha256asher.clone.asInstanceOf[MessageDigest].digest(data)
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
