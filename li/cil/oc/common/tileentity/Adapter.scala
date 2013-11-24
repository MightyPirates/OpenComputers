package li.cil.oc.common.tileentity

import cpw.mods.fml.common.{Loader, Optional}
import dan200.computer.api.{ILuaContext, IComputerAccess, IPeripheral}
import li.cil.oc.api.network._
import li.cil.oc.server.driver
import li.cil.oc.{Config, api}
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.{Array, Some}

@Optional.Interface(iface = "dan200.computer.api.IPeripheral", modid = "ComputerCraft")
class Adapter extends Environment with IPeripheral {
  val node = api.Network.newNode(this, Visibility.Network).create()

  private val blocks = Array.fill[Option[(ManagedEnvironment, api.driver.Block)]](6)(None)

  private val blocksData = Array.fill[Option[BlockData]](6)(None)

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    for (block <- blocks) block match {
      case Some((environment, _)) => environment.update()
      case _ => // Empty.
    }
  }

  def neighborChanged() = if (node != null && node.network != null) {
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      val (x, y, z) = (this.x + d.offsetX, this.y + d.offsetY, this.z + d.offsetZ)
      driver.Registry.driverFor(world, x, y, z) match {
        case Some(newDriver) => blocks(d.ordinal()) match {
          case Some((oldEnvironment, driver)) =>
            if (newDriver != driver) {
              // This is... odd. Maybe moved by some other mod?
              node.disconnect(oldEnvironment.node)
              val environment = newDriver.createEnvironment(world, x, y, z)
              blocks(d.ordinal()) = Some((environment, newDriver))
              blocksData(d.ordinal()) = Some(new BlockData(environment.getClass.getName, new NBTTagCompound()))
              node.connect(environment.node)
            } // else: the more things change, the more they stay the same.
          case _ =>
            // A challenger appears.
            val environment = newDriver.createEnvironment(world, x, y, z)
            blocks(d.ordinal()) = Some((environment, newDriver))
            blocksData(d.ordinal()) match {
              case Some(data) if data.name == environment.getClass.getName =>
                environment.load(data.data)
              case _ =>
            }
            blocksData(d.ordinal()) = Some(new BlockData(environment.getClass.getName, new NBTTagCompound()))
            node.connect(environment.node)
        }
        case _ => blocks(d.ordinal()) match {
          case Some((environment, driver)) =>
            // We had something there, but it's gone now...
            node.disconnect(environment.node)
            environment.save(blocksData(d.ordinal()).get.data)
            blocks(d.ordinal()) = None
          case _ => // Nothing before, nothing now.
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("send")
  @Optional.Method(modid = "ComputerCraft")
  def send(context: Context, args: Arguments) = {
    val port = args.checkInteger(0)
    val answerPort = args.checkInteger(1)
    for ((computer: IComputerAccess, ports) <- openPorts if ports.contains(port)) {
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

  override def onMessage(message: Message) {
    super.onMessage(message)
    if (Loader.isModLoaded("ComputerCraft")) {
      if (message.name == "network.message") message.data match {
        case Array(port: Integer, answerPort: java.lang.Double, args@_*) =>
          for (computer <- computers.map(_.asInstanceOf[IComputerAccess])) {
            if (openPorts(computer).contains(port))
              computer.queueEvent("modem_message", Array(Seq(computer.getAttachmentName, Int.box(port), Int.box(answerPort.toInt)) ++ args.map {
                case x: Array[Byte] => new String(x, "UTF-8")
                case x => x
              }: _*))
          }
        case _ =>
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)

    val blocksNbt = nbt.getTagList(Config.namespace + "adapter.blocks")
    (0 until (blocksNbt.tagCount min blocksData.length)).
      map(blocksNbt.tagAt).
      map(_.asInstanceOf[NBTTagCompound]).
      zipWithIndex.
      foreach {
      case (blockNbt, i) =>
        if (blockNbt.hasKey("name") && blockNbt.hasKey("data")) {
          blocksData(i) = Some(new BlockData(blockNbt.getString("name"), blockNbt.getCompoundTag("data")))
        }
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)

    val blocksNbt = new NBTTagList()
    for (i <- 0 until blocks.length) {
      val blockNbt = new NBTTagCompound()
      blocksData(i) match {
        case Some(data) =>
          blocks(i) match {
            case Some((environment, _)) => environment.save(data.data)
            case _ =>
          }
          blockNbt.setString("name", data.name)
          blockNbt.setCompoundTag("data", data.data)
        case _ =>
      }
      blocksNbt.appendTag(blockNbt)
    }
    nbt.setTag(Config.namespace + "adapter.blocks", blocksNbt)
  }

  // ----------------------------------------------------------------------- //

  private val computers = mutable.ArrayBuffer.empty[AnyRef]

  private val openPorts = mutable.Map.empty[AnyRef, mutable.Set[Int]]

  @Optional.Method(modid = "ComputerCraft")
  override def getType = "oc_adapter"

  @Optional.Method(modid = "ComputerCraft")
  override def attach(computer: IComputerAccess) {
    computers += computer
    openPorts += computer -> mutable.Set.empty
  }

  @Optional.Method(modid = "ComputerCraft")
  override def detach(computer: IComputerAccess) {
    computers -= computer
    openPorts -= computer
  }

  @Optional.Method(modid = "ComputerCraft")
  override def getMethodNames = Array("open", "isOpen", "close", "closeAll", "transmit", "isWireless")

  @Optional.Method(modid = "ComputerCraft")
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

  @Optional.Method(modid = "ComputerCraft")
  override def canAttachToSide(side: Int) = true

  private def checkPort(args: Array[AnyRef], index: Int) = {
    if (args.length < index - 1 || !args(index).isInstanceOf[Double])
      throw new IllegalArgumentException("bad argument #%d (number expected)".format(index + 1))
    val port = args(index).asInstanceOf[Double].toInt
    if (port < 1 || port > 0xFFFF)
      throw new IllegalArgumentException("bad argument #%d (number in [1, 65535] expected)".format(index + 1))
    port
  }

  // ----------------------------------------------------------------------- //

  private class BlockData(val name: String, val data: NBTTagCompound)

}
