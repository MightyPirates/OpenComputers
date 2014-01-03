package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network.{Arguments, Context, LuaCallback, Visibility}
import li.cil.oc.common.tileentity
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.{NBTTagString, NBTTagList, NBTTagCompound}
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import stargatetech2.api.StargateTechAPI
import stargatetech2.api.bus.{BusPacketLIP, BusPacket, IBusDriver, IBusInterface}

class AbstractBus(val owner: tileentity.Computer) extends ManagedComponent with IBusDriver {
  val node = api.Network.newNode(this, Visibility.Neighbors).
    withComponent("abstract_bus").
    create()

  protected val busInterface: IBusInterface = StargateTechAPI.api.getFactory.getIBusInterface(owner, this)

  protected var isEnabled = true

  protected var address = 0

  protected val sendQueue = mutable.Queue.empty[QueuedPacket]

  // ----------------------------------------------------------------------- //

  def canHandlePacket(sender: Short, protocolID: Int, hasLIP: Boolean) = hasLIP

  def handlePacket(packet: BusPacket) {
    val lip = packet.getPlainText
    val data = Map(lip.getEntryList.map(key => (key, lip.get(key))): _*)
    // TODO do we want to push metadata, too?
    owner.signal("bus_message", Int.box(packet.getProtocolID), Int.box(packet.getSender), Int.box(packet.getTarget), data)
  }

  def getNextPacketToSend = if (sendQueue.nonEmpty) {
    val info = sendQueue.dequeue()
    val packet = new BusPacketLIP(info.sender, info.target)
    for ((key, value) <- info.data) {
      packet.set(key, value)
    }
    // TODO generate metadata?
    packet.finish()
    packet
  } else null

  def isInterfaceEnabled = isEnabled

  def getInterfaceAddress = address.toShort

  // ----------------------------------------------------------------------- //

  @LuaCallback("getEnabled")
  def getEnabled(context: Context, args: Arguments): Array[AnyRef] = result(isEnabled)

  @LuaCallback("setEnabled")
  def setEnabled(context: Context, args: Arguments): Array[AnyRef] = {
    isEnabled = args.checkBoolean(0)
    result(isEnabled)
  }

  @LuaCallback("getAddress")
  def getAddress(context: Context, args: Arguments): Array[AnyRef] = result(address)

  @LuaCallback("setAddress")
  def setAddress(context: Context, args: Arguments): Array[AnyRef] = {
    address = args.checkInteger(0) & 0xFFFF
    result(address)
  }

  @LuaCallback("send")
  def send(context: Context, args: Arguments): Array[AnyRef] = {
    val target = args.checkInteger(0) & 0xFFFF
    val data = args.checkTable(1)
    sendQueue += new QueuedPacket(address.toShort, target.toShort, Map(data.toSeq.map(entry => (entry._1.toString, entry._2.toString)): _*))
    result(true)
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    busInterface.readFromNBT(nbt, "bus")
    isEnabled = nbt.getBoolean("enabled")
    address = nbt.getInteger("address") & 0xFFFF
    if (nbt.hasKey("queue")) {
      sendQueue ++= nbt.getTagList("queue").map(QueuedPacket.readFromNBT)
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    busInterface.writeToNBT(nbt, "bus")
    nbt.setBoolean("enabled", isEnabled)
    nbt.setInteger("address", address)
    if (sendQueue.nonEmpty) {
      nbt.setNewTagList("queue", sendQueue.toIterable.map(packet => packet.writeToNBT(new NBTTagCompound())))
    }
  }

  protected class QueuedPacket(val sender: Short, val target: Short, val data: Map[String, String]) {
    def writeToNBT(nbt: NBTTagCompound) = {
      nbt.setShort("sender", sender)
      nbt.setShort("target", target)
      val list = new NBTTagList()
      for ((key, value) <- data) {
        list.append(key)
        list.append(value)
      }
      nbt.setTag("data", list)
      nbt
    }
  }

  protected object QueuedPacket {
    def readFromNBT(nbt: NBTTagCompound) = {
      val sender = nbt.getShort("sender")
      val target = nbt.getShort("target")
      val list = nbt.getTagList("data")
      val data = mutable.Map.empty[String, String]
      for (i <- 0 until list.tagCount by 2) {
        (list.tagAt(i), list.tagAt(i + 1)) match {
          case (key: NBTTagString, value: NBTTagString) => data += key.data -> value.data
          case _ =>
        }
      }
      new QueuedPacket(sender, target, data.toMap)
    }
  }

}
