package li.cil.oc.common.tileentity

import dan200.computer.api.{ILuaContext, IComputerAccess, IPeripheral}
import li.cil.oc.api
import li.cil.oc.api.network.{Message, Visibility, Node}
import li.cil.oc.server.driver
import net.minecraftforge.common.ForgeDirection
import scala.collection.mutable
import net.minecraft.nbt.NBTTagCompound

class Adapter extends Rotatable with Node with IPeripheral {
  val name = "adapter"

  val visibility = Visibility.None

  private val blocks = Array.fill[Option[(Node, api.driver.Block)]](6)(None)

  private val computers = mutable.ArrayBuffer.empty[IComputerAccess]

  private val openPorts = mutable.Map.empty[IComputerAccess, mutable.Set[Int]]

  override def updateEntity() {
    for (block <- blocks) block match {
      case Some((node, driver)) => node.update()
      case _ => // Empty.
    }
  }

  override def receive(message: Message) = super.receive(message) orElse {
    message.data match {
      case Array(port: Int, answerPort: Double, data: AnyRef) if message.name == "network.message" =>
        for ((computer, ports) <- openPorts) if (ports.contains(port)) {
          computer.queueEvent("modem_message", Array(computer.getAttachmentName, Int.box(port), Int.box(answerPort.toInt), data))
        }
      case _ => // Ignore.
    }
    None
  }

  override protected def onConnect() {
    super.onConnect()
    neighborChanged()
  }

  def neighborChanged() {
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      val (x, y, z) = (xCoord + d.offsetX, yCoord + d.offsetY, zCoord + d.offsetZ)
      driver.Registry.driverFor(worldObj, x, y, z) match {
        case Some(newDriver) => blocks(d.ordinal()) match {
          case Some((node, driver)) =>
            if (newDriver != driver) {
              // This is... odd.
              network.foreach(_.disconnect(this, node))
              val newNode = newDriver.node(worldObj, x, y, z)
              network.foreach(_.connect(this, newNode))
              blocks(d.ordinal()) = Some((newNode, newDriver))
            } // else: the more things change, the more they stay the same.
          case _ =>
            // A challenger appears.
            val node = newDriver.node(worldObj, x, y, z)
            network.foreach(_.connect(this, node))
            blocks(d.ordinal()) = Some((node, newDriver))
        }
        case _ => blocks(d.ordinal()) match {
          case Some((node, driver)) =>
            // We had something there, but it's gone now...
            blocks(d.ordinal()) = None
            network.foreach(_.disconnect(this, node))
          case _ => // Nothing before, nothing now.
        }
      }
    }
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    load(nbt.getCompoundTag("node"))
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)

    val nodeNbt = new NBTTagCompound
    save(nodeNbt)
    nbt.setCompoundTag("node", nodeNbt)
  }

  // ----------------------------------------------------------------------- //

  override def getType = "oc_adapter"

  override def attach(computer: IComputerAccess) {
    computers += computer
    openPorts += computer -> mutable.Set.empty
  }

  override def detach(computer: IComputerAccess) {
    computers -= computer
    openPorts -= computer
  }

  override def getMethodNames = Array("open", "isOpen", "close", "closeAll", "transmit", "isWireless")

  override def callMethod(computer: IComputerAccess, context: ILuaContext, method: Int, arguments: Array[AnyRef]) = getMethodNames()(method) match {
    case "open" =>
      val port = parsePort(arguments, 0)
      if (openPorts(computer).size >= 128)
        throw new IllegalArgumentException("too many open channels")
      Array(Boolean.box(openPorts(computer).add(port)))
    case "isOpen" =>
      val port = parsePort(arguments, 0)
      Array(Boolean.box(openPorts(computer).contains(port)))
    case "close" =>
      val port = parsePort(arguments, 0)
      Array(Boolean.box(openPorts(computer).remove(port)))
    case "closeAll" =>
      openPorts(computer).clear()
      null
    case "transmit" =>
      val sendPort = parsePort(arguments, 0)
      val answerPort = parsePort(arguments, 1)
      network.foreach(_.sendToVisible(this, "network.message", sendPort, answerPort, arguments(2)))
      null
    case "isWireless" => Array(Boolean.box(false))
    case _ => null
  }

  override def canAttachToSide(side: Int) = true

  private def parsePort(args: Array[AnyRef], index: Int) = {
    if (args.length < index - 1 || !args(index).isInstanceOf[Int])
      throw new IllegalArgumentException("bad argument #%d (number expected)".format(index))
    val port = args(index).asInstanceOf[Int]
    if (port < 1 || port > 0xFFFF)
      throw new IllegalArgumentException("bad argument #%d (number in [1, 65535] expected)".format(index))
    port
  }
}
