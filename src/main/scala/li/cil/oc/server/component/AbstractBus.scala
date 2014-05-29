package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.common.component
import net.minecraft.nbt.NBTTagCompound
import scala.collection.convert.WrapAsScala._
import stargatetech2.api.StargateTechAPI
import stargatetech2.api.bus._

class AbstractBus(val device: IBusDevice) extends component.ManagedComponent with IBusDriver {
  val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("abstract_bus").
    withConnector().
    create()

  val busInterface: IBusInterface = StargateTechAPI.api.getFactory.getIBusInterface(device, this)

  protected var isEnabled = true

  protected var address = 0

  protected var sendQueue: Option[QueuedPacket] = None

  protected var owner: Option[Context] = None

  // ----------------------------------------------------------------------- //

  override def canHandlePacket(sender: Short, protocolID: Int, hasLIP: Boolean) = hasLIP

  override def handlePacket(packet: BusPacket) {
    val lip = packet.getPlainText
    val data = Map(lip.getEntryList.map(key => (key, lip.get(key))): _*)
    val metadata = Map("mod" -> lip.getMetadata.modID, "device" -> lip.getMetadata.deviceName, "player" -> lip.getMetadata.playerName)
    owner.foreach(_.signal("bus_message", Int.box(packet.getProtocolID), Int.box(packet.getSender), Int.box(packet.getTarget), data, metadata))
  }

  override def getNextPacketToSend = if (sendQueue.isDefined) {
    val info = sendQueue.get
    sendQueue = None
    val packet = new BusPacketLIP(info.sender, info.target)
    for ((key, value) <- info.data) {
      packet.set(key, value)
    }
    packet.setMetadata(new BusPacketLIP.LIPMetadata("OpenComputers", node.address, null))
    packet.finish()
    packet
  }
  else null

  override def isInterfaceEnabled = isEnabled

  override def getInterfaceAddress = address.toShort

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():boolean -- Whether the local bus interface is enabled.""")
  def getEnabled(context: Context, args: Arguments): Array[AnyRef] = result(isEnabled)

  @Callback(doc = """function(enabled:boolean):boolean -- Sets whether the local bus interface should be enabled.""")
  def setEnabled(context: Context, args: Arguments): Array[AnyRef] = {
    isEnabled = args.checkBoolean(0)
    result(isEnabled)
  }

  @Callback(doc = """function():number -- Get the local interface address.""")
  def getAddress(context: Context, args: Arguments): Array[AnyRef] = result(address)

  @Callback(doc = """function(address:number):number -- Sets the local interface address.""")
  def setAddress(context: Context, args: Arguments): Array[AnyRef] = {
    address = args.checkInteger(0) & 0xFFFF
    result(address)
  }

  @Callback(doc = """function(address:number, data:table):boolean -- Sends data across the abstract bus.""")
  def send(context: Context, args: Arguments): Array[AnyRef] = {
    val target = args.checkInteger(0) & 0xFFFF
    val data = args.checkTable(1)
    if (node.tryChangeBuffer(-Settings.get.abstractBusPacketCost)) {
      sendQueue = Some(new QueuedPacket(address.toShort, target.toShort, Map(data.toSeq.map(entry => (entry._1.toString, entry._2.toString)): _*)))
      busInterface.sendAllPackets()
      result(true)
    }
    else result(Unit, "not enough energy")
  }

  @Callback(direct = true, doc = """function():number -- The maximum packet size that can be sent over the bus.""")
  def maxPacketSize(context: Context, args: Arguments): Array[AnyRef] = result(Settings.get.maxNetworkPacketSize)

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (owner.isEmpty && node.host.isInstanceOf[Context]) {
      owner = Some(node.host.asInstanceOf[Context])
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (owner.isDefined && node.host.isInstanceOf[Context] && (node.host.asInstanceOf[Context] == owner.get)) {
      owner = None
    }
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    busInterface.readFromNBT(nbt, "bus")
    // Don't default to false.
    if (nbt.hasKey("enabled")) {
      isEnabled = nbt.getBoolean("enabled")
    }
    address = nbt.getInteger("address") & 0xFFFF
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    busInterface.writeToNBT(nbt, "bus")
    nbt.setBoolean("enabled", isEnabled)
    nbt.setInteger("address", address)
  }

  protected class QueuedPacket(val sender: Short, val target: Short, val data: Map[String, String]) {
    // Extra braces because we don't want/have to keep size as a field.
    {
      val size = data.foldLeft(0)((acc, arg) => {
        acc + arg._1.length + arg._2.length
      })
      if (size > Settings.get.maxNetworkPacketSize) {
        throw new IllegalArgumentException("packet too big (max " + Settings.get.maxNetworkPacketSize + ")")
      }
    }
  }

}
