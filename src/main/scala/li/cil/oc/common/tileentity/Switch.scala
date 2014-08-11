package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import dan200.computer.api.{IComputerAccess, ILuaContext, IPeripheral}
import li.cil.oc.api.Driver
import li.cil.oc.api.network.{Message, Packet}
import li.cil.oc.common.{InventorySlots, Slot, item}
import li.cil.oc.server.PacketSender
import li.cil.oc.util.mods.Mods
import li.cil.oc.{Items, Settings, api}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

import scala.collection.mutable

// Note on the CC1.5+1.6 compatibility
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// We simply implement both APIs. Since 1.6 moved all logic out of the actual
// tile entities (this is essentially exactly like OC's block drivers, except
// that in CC computers are adapters, too) we can keep the CC 1.6 stuff in the
// peripheral provider.
// The @Optional annotations are probably superfluous, they're just there
// because I'm paranoid. If either one of the two APIs is missing, our class
// transformer will take care of stripping out methods and interfaces that are
// not present.
// Aside from that, at least for now CC 1.6 is shipping both the new and the
// old API, so there should be no ClassNotFoundExceptions anyway.

@Optional.Interface(iface = "dan200.computer.api.IPeripheral", modid = Mods.IDs.ComputerCraft)
class Switch extends traits.Hub with traits.NotAnalyzable with IPeripheral with traits.ComponentInventory {
  var lastMessage = 0L

  val computers = mutable.Map.empty[AnyRef, ComputerWrapper]

  val openPorts = mutable.Map.empty[AnyRef, mutable.Set[Int]]

  override def canUpdate = isServer

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.ComputerCraft)
  override def getType = "oc_adapter"

  @Optional.Method(modid = Mods.IDs.ComputerCraft)
  override def canAttachToSide(side: Int) = true

  @Optional.Method(modid = Mods.IDs.ComputerCraft)
  override def attach(computer: IComputerAccess) {
    computers += computer -> new ComputerWrapper {
      override def id = computer.getID

      override def attachmentName = computer.getAttachmentName

      override def queueEvent(name: String, args: Array[AnyRef]) = computer.queueEvent(name, args)
    }
    openPorts += computer -> mutable.Set.empty
  }

  @Optional.Method(modid = Mods.IDs.ComputerCraft)
  override def detach(computer: IComputerAccess) {
    computers -= computer
    openPorts -= computer
  }

  @Optional.Method(modid = Mods.IDs.ComputerCraft)
  override def getMethodNames = Array("open", "isOpen", "close", "closeAll", "maxPacketSize", "transmit", "isWireless")

  @Optional.Method(modid = Mods.IDs.ComputerCraft)
  override def callMethod(computer: IComputerAccess, context: ILuaContext, method: Int, arguments: Array[AnyRef]) =
    callMethod(computer, computer.getID, computer.getAttachmentName, method, arguments)

  @Optional.Method(modid = Mods.IDs.ComputerCraft)
  def callMethod(computer: AnyRef, computerId: Int, attachmentName: String, method: Int, arguments: Array[AnyRef]): Array[AnyRef] = getMethodNames()(method) match {
    case "open" =>
      val port = checkPort(arguments, 0)
      if (openPorts(computer).size >= 128)
        throw new IllegalArgumentException("too many open channels")
      result(openPorts(computer).add(port))
    case "isOpen" =>
      val port = checkPort(arguments, 0)
      result(openPorts(computer).contains(port))
    case "close" =>
      val port = checkPort(arguments, 0)
      result(openPorts(computer).remove(port))
    case "closeAll" =>
      openPorts(computer).clear()
      null
    case "maxPacketSize" =>
      result(Settings.get.maxNetworkPacketSize)
    case "transmit" =>
      val sendPort = checkPort(arguments, 0)
      val answerPort = checkPort(arguments, 1)
      val data = Seq(Int.box(answerPort)) ++ arguments.drop(2)
      val packet = api.Network.newPacket(s"cc${computerId}_$attachmentName", null, sendPort, data.toArray)
      result(tryEnqueuePacket(ForgeDirection.UNKNOWN, packet))
    case "isWireless" => result(this.isInstanceOf[AccessPoint])
    case _ => null
  }

  // ----------------------------------------------------------------------- //

  protected def checkPort(args: Array[AnyRef], index: Int) = {
    if (args.length < index - 1 || !args(index).isInstanceOf[Double])
      throw new IllegalArgumentException("bad argument #%d (number expected)".format(index + 1))
    val port = args(index).asInstanceOf[Double].toInt
    if (port < 1 || port > 0xFFFF)
      throw new IllegalArgumentException("bad argument #%d (number in [1, 65535] expected)".format(index + 1))
    port
  }

  protected def queueMessage(source: String, destination: String, port: Int, answerPort: Int, args: Array[AnyRef]) {
    for ((computer, wrapper) <- computers) {
      val address = s"cc${wrapper.id}_${wrapper.attachmentName}"
      if (source != address && Option(destination).forall(_ == address) && openPorts(computer).contains(port))
        wrapper.queueEvent("modem_message", Array(Seq(wrapper.attachmentName, Int.box(port), Int.box(answerPort)) ++ args.map {
          case x: Array[Byte] => new String(x, "UTF-8")
          case x => x
        }: _*))
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def relayPacket(sourceSide: ForgeDirection, packet: Packet) {
    super.relayPacket(sourceSide, packet)
    val now = System.currentTimeMillis()
    if (now - lastMessage >= (relayDelay - 1) * 50) {
      lastMessage = now
      PacketSender.sendSwitchActivity(this)
    }
  }

  override protected def onPlugMessage(plug: Plug, message: Message) {
    super.onPlugMessage(plug, message)
    if (message.name == "network.message" && Mods.ComputerCraft.isAvailable) {
      message.data match {
        case Array(packet: Packet) =>
          packet.data.headOption match {
            case Some(answerPort: java.lang.Double) =>
              queueMessage(packet.source, packet.destination, packet.port, answerPort.toInt, packet.data.drop(1))
            case _ =>
              queueMessage(packet.source, packet.destination, packet.port, -1, packet.data)
          }
        case _ =>
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    updateLimits(slot, stack)
  }

  private def updateLimits(slot: Int, stack: ItemStack) {
    Driver.driverFor(stack) match {
      case driver if Slot.fromApi(driver.slot(stack)) == Slot.CPU =>
        relayDelay = math.max(1, relayBaseDelay - ((driver.tier(stack) + 1) * relayDelayPerUpgrade))
      case driver if Slot.fromApi(driver.slot(stack)) == Slot.Memory =>
        relayAmount = math.max(1, relayBaseAmount + (Items.multi.subItem(stack) match {
          case Some(ram: item.Memory) => (ram.tier + 1) * relayAmountPerUpgrade
          case _ => (driver.tier(stack) + 1) * (relayAmountPerUpgrade * 2)
        }))
      case driver if Slot.fromApi(driver.slot(stack)) == Slot.HDD =>
        maxQueueSize = math.max(1, queueBaseSize + (driver.tier(stack) + 1) * queueSizePerUpgrade)
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    Driver.driverFor(stack) match {
      case driver if Slot.fromApi(driver.slot(stack)) == Slot.CPU => relayDelay = relayBaseDelay
      case driver if Slot.fromApi(driver.slot(stack)) == Slot.Memory => relayAmount = relayBaseAmount
      case driver if Slot.fromApi(driver.slot(stack)) == Slot.HDD => maxQueueSize = queueBaseSize
    }
  }

  override def getSizeInventory = InventorySlots.switch.length

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    Option(Driver.driverFor(stack)).fold(false)(driver => {
      val provided = InventorySlots.switch(slot)
      Slot.fromApi(driver.slot(stack)) == provided.slot && driver.tier(stack) <= provided.tier
    })

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    for (slot <- 0 until items.length) items(slot) collect {
      case stack => updateLimits(slot, stack)
    }
  }
}

// Abstraction layer for CC computers to support 1.5 and 1.6 API.
trait ComputerWrapper {
  def id: Int

  def attachmentName: String

  def queueEvent(name: String, args: Array[AnyRef])
}