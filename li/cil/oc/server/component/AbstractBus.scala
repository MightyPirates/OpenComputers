package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network.{Arguments, Context, LuaCallback, Visibility}
import li.cil.oc.common.tileentity
import net.minecraft.nbt.NBTTagCompound
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

  protected var sendQueue = mutable.Queue.empty[BusPacketLIP]

  // ----------------------------------------------------------------------- //

  def canHandlePacket(sender: Short, protocolID: Int, hasLIP: Boolean) = hasLIP

  def handlePacket(packet: BusPacket) {
    val lip = packet.getPlainText
    val data = Map(lip.getEntryList.map(key => (key, lip.get(key))): _*)
    // TODO do we want to push metadata, too?
    owner.signal("bus_message", Int.box(packet.getProtocolID), Int.box(packet.getSender), Int.box(packet.getTarget), data)
  }

  def getNextPacketToSend = if (sendQueue.nonEmpty) sendQueue.dequeue() else null

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
    val packet = new BusPacketLIP(address.toShort, target.toShort)
    for ((key, value) <- data) {
      packet.set(key.toString, value.toString)
    }
    // TODO generate metadata
    sendQueue += packet
    result(true)
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    busInterface.readFromNBT(nbt, "bus")
    isEnabled = nbt.getBoolean("enabled")
    address = nbt.getInteger("address") & 0xFFFF
    // TODO load queue
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    busInterface.writeToNBT(nbt, "bus")
    nbt.setBoolean("enabled", isEnabled)
    nbt.setInteger("address", address)
    // TODO save queue
  }
}
