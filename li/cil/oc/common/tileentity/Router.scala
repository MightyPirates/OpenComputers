package li.cil.oc.common.tileentity

import cpw.mods.fml.common.{Loader, Optional}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import dan200.computer.api.{ILuaContext, IComputerAccess, IPeripheral}
import li.cil.oc.api.network.{Node, Message, Visibility}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Blocks, Settings, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import scala.collection.mutable

@Optional.Interface(iface = "dan200.computer.api.IPeripheral", modid = "ComputerCraft")
class Router extends TileEntity with api.network.SidedEnvironment with IPeripheral {
  private val plugs = ForgeDirection.VALID_DIRECTIONS.map(side => new Plug(side))

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  def canConnect(side: ForgeDirection) = true

  def sidedNode(side: ForgeDirection) = plugs(side.ordinal()).node

  // ----------------------------------------------------------------------- //

  override def canUpdate = false

  override def validate() {
    super.validate()
    worldObj.scheduleBlockUpdateFromLoad(xCoord, yCoord, zCoord, Blocks.router.parent.blockID, Int.MinValue, 0)
  }

  override def invalidate() {
    super.invalidate()
    for (plug <- plugs if plug.node != null) {
      plug.node.remove()
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    for (plug <- plugs if plug.node != null) {
      plug.node.remove()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    nbt.getTagList(Settings.namespace + "plugs").iterator[NBTTagCompound].zip(plugs).foreach {
      case (plugNbt, plug) => plug.node.load(plugNbt)
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setNewTagList(Settings.namespace + "plugs", plugs.map(plug => {
      val plugNbt = new NBTTagCompound()
      plug.node.save(plugNbt)
      plugNbt
    }))
  }

  // ----------------------------------------------------------------------- //
  // Peripheral

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
      plugs.foreach(_.node.sendToReachable("network.message", Seq(Int.box(sendPort), Int.box(answerPort)) ++ arguments.drop(2): _*))
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

  private def queueMessage(port: Int, answerPort: Int, args: Seq[AnyRef]) {
    for (computer <- computers.map(_.asInstanceOf[IComputerAccess])) {
      if (openPorts(computer).contains(port))
        computer.queueEvent("modem_message", Array(Seq(computer.getAttachmentName, Int.box(port), Int.box(answerPort)) ++ args.map {
          case x: Array[Byte] => new String(x, "UTF-8")
          case x => x
        }: _*))
    }
  }

  private class Plug(val side: ForgeDirection) extends api.network.Environment {
    val node = api.Network.newNode(this, Visibility.Network).create()

    def onMessage(message: Message) {
      if (isPrimary && message.name == "network.message") {
        plugsInOtherNetworks.foreach(_.node.sendToReachable(message.name, message.data: _*))
        if (Loader.isModLoaded("ComputerCraft")) {
          message.data match {
            case Array(port: Integer, answerPort: java.lang.Double, args@_*) =>
              queueMessage(port, answerPort.toInt, args)
            case Array(port: Integer, args@_*) =>
              queueMessage(port, -1, args)
            case _ =>
          }
        }
      }
    }

    def onDisconnect(node: Node) {}

    def onConnect(node: Node) {}

    private def isPrimary = plugs(plugs.indexWhere(_.node.network == node.network)) == this

    private def plugsInOtherNetworks = plugs.filter(_.node.network != node.network)
  }

}
