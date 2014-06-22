package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.common.component
import net.minecraft.nbt.NBTTagCompound
import stargatetech2.api.StargateTechAPI
import stargatetech2.api.bus._

import scala.collection.convert.WrapAsScala._

class AbstractBusCard(val device: IBusDevice) extends component.ManagedComponent with IBusDriver {
  val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("abstract_bus").
    withConnector().
    create()

  val busInterface: IBusInterface = StargateTechAPI.api.getFactory.getIBusInterface(device, this)

  protected var isEnabled = true

  protected var address = 0

  protected var sendQueue: Option[BusPacket[_]] = None

  protected var owner: Option[Context] = None

  // ----------------------------------------------------------------------- //

  override def getShortName = "Computer"

  override def getDescription = "An OpenComputers computer or server."

  override def canHandlePacket(sender: Short, protocolID: Int, hasLIP: Boolean) = hasLIP

  override def handlePacket(packet: BusPacket[_]) {
    val lip = packet.getPlainText
    val data = Map(lip.getEntryList.map(key => (key, lip.get(key))): _*)
    val metadata = Map("mod" -> lip.getMetadata.modID, "device" -> lip.getMetadata.deviceName, "player" -> lip.getMetadata.playerName)
    owner.foreach(_.signal("bus_message", Int.box(packet.getProtocolID), Int.box(packet.getSender), Int.box(packet.getTarget), data, metadata))
  }

  override def getNextPacketToSend = this.synchronized {
    val packet = sendQueue.orNull
    sendQueue = None
    packet
  }

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

  @Callback(doc = """function(address:number, data:table):table -- Sends data across the abstract bus.""")
  def send(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val target = args.checkInteger(0) & 0xFFFF
    val data = args.checkTable(1)
    if (node.tryChangeBuffer(-Settings.get.abstractBusPacketCost)) {
      val packet = new BusPacketLIP(address.toShort, target.toShort)
      var size = 0
      def checkSize(add: Int) {
        size += add
        if (size > Settings.get.maxNetworkPacketSize) {
          throw new IllegalArgumentException("packet too big (max " + Settings.get.maxNetworkPacketSize + ")")
        }
      }
      for ((key, value) <- data) {
        val keyAsString = key.toString
        checkSize(keyAsString.length)
        val valueAsString = value.toString
        checkSize(valueAsString.length)
        packet.set(keyAsString, valueAsString)
      }
      packet.setMetadata(new BusPacketLIP.LIPMetadata("OpenComputers", node.address, null))
      packet.finish()
      sendQueue = Some(packet)
      busInterface.sendAllPackets()
      result(packet.getResponses.toArray)
    }
    else result(Unit, "not enough energy")
  }

  @Callback(doc = """function(mask:number):table -- Scans the network for other devices.""")
  def scan(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    val mask = (args.checkInteger(0) & 0xFFFF).toShort
    if (node.tryChangeBuffer(-Settings.get.abstractBusPacketCost)) {
      val packet = new BusPacketNetScan(mask)
      sendQueue = Some(packet)
      busInterface.sendAllPackets()
      Array(packet.getDevices.toArray)
    }
    else Array(Unit, "not enough energy")
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
}
