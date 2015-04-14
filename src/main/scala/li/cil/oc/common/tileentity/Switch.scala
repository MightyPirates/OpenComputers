package li.cil.oc.common.tileentity

import com.google.common.base.Charsets
import dan200.computercraft.api.peripheral.IComputerAccess
import li.cil.oc.api.Driver
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Packet
import li.cil.oc.common.InventorySlots
import li.cil.oc.common.Slot
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.integration.Mods
import li.cil.oc.server.PacketSender
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable

class Switch extends traits.Hub with traits.NotAnalyzable with traits.ComponentInventory {
  var lastMessage = 0L

  val computers = mutable.Buffer.empty[AnyRef]

  val openPorts = mutable.Map.empty[AnyRef, mutable.Set[Int]]

  override def canUpdate = isServer

  // ----------------------------------------------------------------------- //

  protected def queueMessage(source: String, destination: String, port: Int, answerPort: Int, args: Array[AnyRef]) {
    for (computer <- computers.map(_.asInstanceOf[IComputerAccess])) {
      val address = s"cc${computer.getID}_${computer.getAttachmentName}"
      if (source != address && Option(destination).forall(_ == address) && openPorts(computer).contains(port))
        computer.queueEvent("modem_message", Array(Seq(computer.getAttachmentName, Int.box(port), Int.box(answerPort)) ++ args.map {
          case x: Array[Byte] => new String(x, Charsets.UTF_8)
          case x => x
        }: _*))
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def relayPacket(sourceSide: Option[ForgeDirection], packet: Packet) {
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
    Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver) if driver.slot(stack) == Slot.CPU =>
        relayDelay = math.max(1, relayBaseDelay - ((driver.tier(stack) + 1) * relayDelayPerUpgrade))
      case Some(driver) if driver.slot(stack) == Slot.Memory =>
        relayAmount = math.max(1, relayBaseAmount + (Delegator.subItem(stack) match {
          case Some(ram: item.Memory) => (ram.tier + 1) * relayAmountPerUpgrade
          case _ => (driver.tier(stack) + 1) * (relayAmountPerUpgrade * 2)
        }))
      case Some(driver) if driver.slot(stack) == Slot.HDD =>
        maxQueueSize = math.max(1, queueBaseSize + (driver.tier(stack) + 1) * queueSizePerUpgrade)
      case _ => // Dafuq u doin.
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    Driver.driverFor(stack, getClass) match {
      case driver if driver.slot(stack) == Slot.CPU => relayDelay = relayBaseDelay
      case driver if driver.slot(stack) == Slot.Memory => relayAmount = relayBaseAmount
      case driver if driver.slot(stack) == Slot.HDD => maxQueueSize = queueBaseSize
    }
  }

  override def getSizeInventory = InventorySlots.switch.length

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    Option(Driver.driverFor(stack, getClass)).fold(false)(driver => {
      val provided = InventorySlots.switch(slot)
      driver.slot(stack) == provided.slot && driver.tier(stack) <= provided.tier
    })

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    for (slot <- 0 until items.length) items(slot) collect {
      case stack => updateLimits(slot, stack)
    }
  }
}
