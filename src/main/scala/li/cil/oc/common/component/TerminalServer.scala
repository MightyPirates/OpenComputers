package li.cil.oc.common.component

import java.util
import java.util.UUID

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.component.RackBusConnectable
import li.cil.oc.api.component.RackMountable
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal.Keyboard.UsabilityChecker
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.util.Lifecycle
import li.cil.oc.api.util.StateAware
import li.cil.oc.api.util.StateAware.State
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

class TerminalServer(val rack: api.internal.Rack, val slot: Int) extends Environment with EnvironmentHost with Analyzable with RackMountable with Lifecycle with DeviceInfo {
  val node = api.Network.newNode(this, Visibility.None).create()

  lazy val buffer = {
    val screenItem = api.Items.get(Constants.BlockName.ScreenTier1).createItemStack(1)
    val buffer = api.Driver.driverFor(screenItem, getClass).createEnvironment(screenItem, this).asInstanceOf[api.internal.TextBuffer]
    val (maxWidth, maxHeight) = Settings.screenResolutionsByTier(Tier.Three)
    buffer.setMaximumResolution(maxWidth, maxHeight)
    buffer.setMaximumColorDepth(Settings.screenDepthsByTier(Tier.Three))
    buffer
  }

  lazy val keyboard = {
    val keyboardItem = api.Items.get(Constants.BlockName.Keyboard).createItemStack(1)
    val keyboard = api.Driver.driverFor(keyboardItem, getClass).createEnvironment(keyboardItem, this).asInstanceOf[api.internal.Keyboard]
    keyboard.setUsableOverride(new UsabilityChecker {
      override def isUsableByPlayer(keyboard: api.internal.Keyboard, player: EntityPlayer) = {
        val stack = player.getHeldItem
        Delegator.subItem(stack) match {
          case Some(t: item.Terminal) if stack.hasTagCompound => sidedKeys.contains(stack.getTagCompound.getString(Settings.namespace + "key"))
          case _ => false
        }
      }
    })
    keyboard
  }

  var range = Settings.get.maxWirelessRange(Tier.Two)
  val keys = mutable.ListBuffer.empty[String]

  def address = rack.getMountableData(slot).getString("terminalAddress")

  def sidedKeys = {
    if (!rack.world.isRemote) keys
    else rack.getMountableData(slot).getTagList("keys", NBT.TAG_STRING).map((tag: NBTTagString) => tag.func_150285_a_())
  }

  // ----------------------------------------------------------------------- //
  // DeviceInfo

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Terminal server",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "RemoteViewing EX"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //
  // Environment

  override def onConnect(node: Node) {
    if (node == this.node) {
      node.connect(buffer.node)
      node.connect(keyboard.node)
      buffer.node.connect(keyboard.node)
    }
  }

  override def onDisconnect(node: Node) {
    if (node == this.node) {
      buffer.node.remove()
      keyboard.node.remove()
    }
  }

  override def onMessage(message: Message) {
  }

  // ----------------------------------------------------------------------- //
  // EnvironmentHost

  override def world = rack.world

  override def xPosition = rack.xPosition

  override def yPosition = rack.yPosition

  override def zPosition = rack.zPosition

  override def markChanged() = rack.markChanged()

  // ----------------------------------------------------------------------- //
  // RackMountable

  override def getData: NBTTagCompound = {
    if (node.address == null) api.Network.joinNewNetwork(node)

    val nbt = new NBTTagCompound()
    nbt.setNewTagList("keys", keys)
    nbt.setString("terminalAddress", node.address)
    nbt
  }

  override def getConnectableCount: Int = 0

  override def getConnectableAt(index: Int): RackBusConnectable = null

  override def onActivate(player: EntityPlayer, hitX: Float, hitY: Float): Boolean = {
    val stack = player.getHeldItem
    if (api.Items.get(stack) == api.Items.get(Constants.ItemName.Terminal)) {
      if (!world.isRemote) {
        val key = UUID.randomUUID().toString
        if (!stack.hasTagCompound) {
          stack.setTagCompound(new NBTTagCompound())
        }
        else {
          keys -= stack.getTagCompound.getString(Settings.namespace + "key")
        }
        val maxSize = Settings.get.terminalsPerServer
        while (keys.length >= maxSize) {
          keys.remove(0)
        }
        keys += key
        stack.getTagCompound.setString(Settings.namespace + "key", key)
        stack.getTagCompound.setString(Settings.namespace + "server", node.address)
        rack.markChanged(slot)
        player.inventory.markDirty()
      }
      true
    }
    else false
  }

  // ----------------------------------------------------------------------- //
  // Persistable

  override def load(nbt: NBTTagCompound): Unit = {
    if (!rack.world.isRemote) {
      node.load(nbt)
    }
    buffer.load(nbt.getCompoundTag(Settings.namespace + "buffer"))
    keyboard.load(nbt.getCompoundTag(Settings.namespace + "keyboard"))
    keys.clear()
    nbt.getTagList(Settings.namespace + "keys", NBT.TAG_STRING).foreach((tag: NBTTagString) => keys += tag.func_150285_a_())
  }

  override def save(nbt: NBTTagCompound): Unit = {
    node.save(nbt)
    nbt.setNewCompoundTag(Settings.namespace + "buffer", buffer.save)
    nbt.setNewCompoundTag(Settings.namespace + "keyboard", keyboard.save)
    nbt.setNewTagList(Settings.namespace + "keys", keys)
  }

  // ----------------------------------------------------------------------- //
  // ManagedEnvironment

  override def canUpdate: Boolean = true

  override def update(): Unit = {
    if (world.isRemote || (node.address != null && node.network != null)) {
      buffer.update()
    }
  }

  // ----------------------------------------------------------------------- //
  // StateAware

  override def getCurrentState: util.EnumSet[State] = {
    util.EnumSet.noneOf(classOf[StateAware.State])
  }

  // ----------------------------------------------------------------------- //
  // Analyzable

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = Array(buffer.node, keyboard.node)

  // ----------------------------------------------------------------------- //
  // LifeCycle

  override def onLifecycleStateChange(state: Lifecycle.LifecycleState): Unit = if (rack.world.isRemote) state match {
    case Lifecycle.LifecycleState.Initialized =>
      TerminalServer.loaded += this
    case Lifecycle.LifecycleState.Disposed =>
      TerminalServer.loaded -= this
    case _ => // Ignore.
  }
}

object TerminalServer {
  val loaded = mutable.Buffer.empty[TerminalServer]
}
