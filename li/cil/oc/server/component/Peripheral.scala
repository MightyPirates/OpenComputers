package li.cil.oc.server.component

import dan200.computer.api.{IMount, IWritableMount, IComputerAccess, IPeripheral}
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.server.network.{Node => MutableNode}
import net.minecraft.nbt.{NBTTagString, NBTTagCompound}
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class Peripheral(peripheral: IPeripheral) extends ManagedComponent with IComputerAccess {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("peripheral").
    create()

  private val mounts = mutable.Map.empty[String, ManagedEnvironment]

  // Used to restore mounts to their previous addresses after save/load.
  private val mountAddresses = mutable.Map.empty[String, String]

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "getType", asynchronous = true)
  def getType(context: Context, args: Arguments): Array[Object] =
    result(peripheral.getType)

  @LuaCallback(value = "getMethodNames", asynchronous = true)
  def getMethodNames(context: Context, args: Arguments): Array[Object] =
    peripheral.getMethodNames.map(_.asInstanceOf[AnyRef])

  @LuaCallback(value = "callMethod", asynchronous = true)
  def callMethod(context: Context, args: Arguments): Array[Object] = {
    val method = args.checkInteger(0)
    peripheral.callMethod(this, null, method, args.drop(1).map {
      case x: Array[Byte] => new String(x, "UTF-8")
      case x => x
    }.toArray)
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      peripheral.attach(this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      peripheral.detach(this)
      for ((_, env) <- mounts) {
        env.node.remove()
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    if (nbt.hasKey("oc.peripheral.addresses")) {
      val addressesNbt = nbt.getCompoundTag("oc.peripheral.addresses")
      for (tag <- addressesNbt.getTags) tag match {
        case addressNbt: NBTTagString =>
          mountAddresses += addressNbt.getName -> addressNbt.data
        case _ =>
      }
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    val addressesNbt = new NBTTagCompound()
    for ((location, env) <- mounts) {
      addressesNbt.setString(location, env.node.address)
    }
    nbt.setCompoundTag("oc.peripheral.addresses", addressesNbt)
  }

  // ----------------------------------------------------------------------- //

  def getID = -1

  def getAttachmentName = node.address

  def queueEvent(event: String, arguments: Array[AnyRef]) {
    node.sendToReachable("computer.signal", Seq(event) ++ arguments: _*)
  }

  def mount(desiredLocation: String, mount: IMount) =
    mountFileSystem(desiredLocation, api.FileSystem.fromComputerCraft(mount))

  def mountWritable(desiredLocation: String, mount: IWritableMount) =
    mountFileSystem(desiredLocation, api.FileSystem.fromComputerCraft(mount))

  private def mountFileSystem(desiredLocation: String, fs: api.fs.FileSystem) =
    if (!mounts.contains(desiredLocation)) {
      val env = api.FileSystem.asManagedEnvironment(fs, desiredLocation)
      env.node.asInstanceOf[Component].setVisibility(Visibility.Network)
      if (mountAddresses.contains(desiredLocation)) {
        env.node.asInstanceOf[MutableNode].address = mountAddresses(desiredLocation)
        mountAddresses -= desiredLocation
      }
      node.connect(env.node)
      mounts += desiredLocation -> env
      desiredLocation
    }
    else null

  def unmount(location: String) {
    mounts.remove(location) match {
      case Some(env) =>
        mountAddresses -= location
        env.node.remove()
      case _ =>
    }
  }
}
