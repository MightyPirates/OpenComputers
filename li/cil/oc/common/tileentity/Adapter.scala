package li.cil.oc.common.tileentity

import dan200.computer.api.{ILuaContext, IComputerAccess, IPeripheral}
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.server
import li.cil.oc.server.driver
import net.minecraft.nbt.{NBTTagString, NBTTagList, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection
import scala.Some
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

// TODO persist managed environments of attached blocks somehow...

class Adapter extends Rotatable with Environment with IPeripheral {
  val node = api.Network.createComponent(api.Network.createNode(this, "adapter", Visibility.None))

  private val blocks = Array.fill[Option[(ManagedEnvironment, api.driver.Block)]](6)(None)

  private val blocksAddresses = Array.fill[String](6)(java.util.UUID.randomUUID.toString)

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    for (block <- blocks) block match {
      case Some((environment, _)) => environment.update()
      case _ => // Empty.
    }
  }

  def neighborChanged() {
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      val (x, y, z) = (xCoord + d.offsetX, yCoord + d.offsetY, zCoord + d.offsetZ)
      driver.Registry.driverFor(worldObj, x, y, z) match {
        case Some(newDriver) => blocks(d.ordinal()) match {
          case Some((environment, driver)) =>
            if (newDriver != driver) {
              // This is... odd. Maybe moved by some other mod?
              node.disconnect(environment.node)
              val newEnvironment = newDriver.createEnvironment(worldObj, x, y, z)
              newEnvironment.node.asInstanceOf[server.network.Node].address = blocksAddresses(d.ordinal())
              node.connect(newEnvironment.node)
              blocks(d.ordinal()) = Some((newEnvironment, newDriver))
            } // else: the more things change, the more they stay the same.
          case _ =>
            // A challenger appears.
            val environment = newDriver.createEnvironment(worldObj, x, y, z)
            environment.node.asInstanceOf[server.network.Node].address = blocksAddresses(d.ordinal())
            node.connect(environment.node)
            blocks(d.ordinal()) = Some((environment, newDriver))
        }
        case _ => blocks(d.ordinal()) match {
          case Some((environment, driver)) =>
            // We had something there, but it's gone now...
            blocks(d.ordinal()) = None
            node.disconnect(environment.node)
          case _ => // Nothing before, nothing now.
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("send")
  def send(context: Context, args: Arguments) = {
    val port = args.checkInteger(0)
    val answerPort = args.checkInteger(1)
    for ((computer, ports) <- openPorts if ports.contains(port)) {
      computer.queueEvent("modem_message", Array(Seq(computer.getAttachmentName, Int.box(port), Int.box(answerPort)) ++ args.drop(2): _*))
    }
    null
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      neighborChanged()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    node.load(nbt)

    val addressesNbt = nbt.getTagList("oc.adapter.addresses")
    (0 until (addressesNbt.tagCount min blocksAddresses.length)).
      map(addressesNbt.tagAt).
      map(_.asInstanceOf[NBTTagString].data).
      zipWithIndex.
      foreach {
      case (a, i) => blocksAddresses(i) = a
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    node.save(nbt)

    val addressesNbt = new NBTTagList()
    for (i <- 0 until blocksAddresses.length) {
      addressesNbt.appendTag(new NBTTagString(null, blocksAddresses(i)))
    }
    nbt.setTag("oc.adapter.addresses", addressesNbt)
  }

  // ----------------------------------------------------------------------- //

  private val computers = mutable.ArrayBuffer.empty[IComputerAccess]

  private val openPorts = mutable.Map.empty[IComputerAccess, mutable.Set[Int]]

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
      val port = checkPort(arguments, 0)
      if (openPorts(computer).size >= 128)
        throw new IllegalArgumentException("too many open channels")
      Array(Boolean.box(openPorts(computer).add(port)))
    case "isOpen" =>
      val port = checkPort(arguments, 0)
      Array(Boolean.box(openPorts(computer).contains(port)))
    case "close" =>
      val port = checkPort(arguments, 0)
      Array(Boolean.box(openPorts(computer).remove(port)))
    case "closeAll" =>
      openPorts(computer).clear()
      null
    case "transmit" =>
      val sendPort = checkPort(arguments, 0)
      val answerPort = checkPort(arguments, 1)
      node.sendToReachable("network.message", Seq(Int.box(sendPort), Int.box(answerPort)) ++ arguments.drop(2): _*)
      null
    case "isWireless" => Array(java.lang.Boolean.FALSE)
    case _ => null
  }

  override def canAttachToSide(side: Int) = true

  private def checkPort(args: Array[AnyRef], index: Int) = {
    if (args.length < index - 1 || !args(index).isInstanceOf[Int])
      throw new IllegalArgumentException("bad argument #%d (number expected)".format(index))
    val port = args(index).asInstanceOf[Int]
    if (port < 1 || port > 0xFFFF)
      throw new IllegalArgumentException("bad argument #%d (number in [1, 65535] expected)".format(index))
    port
  }
}
