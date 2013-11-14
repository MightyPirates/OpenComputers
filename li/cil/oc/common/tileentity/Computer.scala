package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import li.cil.oc.server.component
import li.cil.oc.{Config, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import scala.Some

abstract class Computer extends Environment with Context with Analyzable {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("computer", Visibility.Neighbors).
    withConnector().
    create()

  val instance: component.Computer

  def installedMemory: Int

  // ----------------------------------------------------------------------- //

  private var hasChanged = false

  def markAsChanged() = hasChanged = true

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    // If we're not yet in a network we were just loaded from disk. We skip
    // the update this round to allow other tile entities to join the network,
    // too, avoiding issues of missing nodes (e.g. in the GPU which would
    // otherwise loose track of its screen).
    if (!worldObj.isRemote && node != null && node.network != null) {
      if (instance.isRunning && !node.changeBuffer(-Config.computerCost)) {
        instance.lastError = "not enough energy"
        instance.stop()
      }
      instance.update()

      if (hasChanged) {
        hasChanged = false
        worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this)
      }
    }

    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (instance != null) instance.load(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (instance != null) instance.save(nbt)
  }

  // ----------------------------------------------------------------------- //

  def address = node.address

  def isUser(player: String) = instance.isUser(player)

  def signal(name: String, args: AnyRef*) = instance.signal(name, args: _*)

  // ----------------------------------------------------------------------- //

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    if (instance != null) {
      instance.lastError match {
        case Some(value) => stats.setString(Config.namespace + "text.Analyzer.LastError", value)
        case _ =>
      }
    }
    this
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("start")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    Array(Boolean.box(instance.start()))

  @LuaCallback("stop")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    Array(Boolean.box(instance.stop()))

  @LuaCallback("isRunning")
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    Array(Boolean.box(instance.isRunning))

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) = {
    super.onMessage(message)
    message.data match {
      case Array(name: String, args@_*) if message.name == "computer.signal" =>
        instance.signal(name, Seq(message.source.address) ++ args: _*)
      case Array(player: EntityPlayer, name: String, args@_*) if message.name == "computer.checked_signal" =>
        if (isUser(player.getCommandSenderName))
          instance.signal(name, Seq(message.source.address) ++ args: _*)
      case _ =>
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      instance.rom.foreach(rom => node.connect(rom.node))
      instance.tmp.foreach(tmp => node.connect(tmp.node))
    }
    else {
      node match {
        case component: Component => instance.addComponent(component)
        case _ =>
      }
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      instance.rom.foreach(_.node.remove())
      instance.tmp.foreach(_.node.remove())
      instance.stop()
    }
    else {
      node match {
        case component: Component => instance.removeComponent(component)
        case _ =>
      }
    }
  }
}
