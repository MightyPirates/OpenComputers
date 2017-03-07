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
import li.cil.oc.api.network._
import li.cil.oc.api.util.Lifecycle
import li.cil.oc.api.util.StateAware
import li.cil.oc.api.util.StateAware.State
import li.cil.oc.common.Tier
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

class TerminalServer(val rack: api.internal.Rack, val slot: Int) extends Environment with EnvironmentHost with Analyzable with RackMountable with Lifecycle with DeviceInfo {
  val getNode = api.Network.newNode(this, Visibility.NONE).create()

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
        val stack = player.getHeldItemMainhand
        Delegator.subItem(stack) match {
          case Some(t: item.Terminal) if stack.hasTagCompound => sidedKeys.contains(stack.getTagCompound.getString(Settings.namespace + "key"))
          case _ => false
        }
      }
    })
    keyboard
  }

  var range = Settings.get.maxWirelessRange
  val keys = mutable.ListBuffer.empty[String]

  def address = rack.getMountableData(slot).getString("terminalAddress")

  def sidedKeys = {
    if (!rack.getWorld.isRemote) keys
    else rack.getMountableData(slot).getTagList("keys", NBT.TAG_STRING).map((tag: NBTTagString) => tag.getString)
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
    if (node == this.getNode) {
      node.connect(buffer.getNode)
      node.connect(keyboard.getNode)
      buffer.getNode.connect(keyboard.getNode)
    }
  }

  override def onDisconnect(node: Node) {
    if (node == this.getNode) {
      buffer.getNode.remove()
      keyboard.getNode.remove()
    }
  }

  override def onMessage(message: Message) {
  }

  // ----------------------------------------------------------------------- //
  // EnvironmentHost

  override def getWorld = rack.getWorld

  override def xPosition = rack.xPosition

  override def yPosition = rack.yPosition

  override def zPosition = rack.zPosition

  override def markChanged() = rack.markChanged()

  // ----------------------------------------------------------------------- //
  // RackMountable

  override def getData: NBTTagCompound = {
    if (getNode.getAddress == null) api.Network.joinNewNetwork(getNode)

    val nbt = new NBTTagCompound()
    nbt.setNewTagList("keys", keys)
    nbt.setString("terminalAddress", getNode.getAddress)
    nbt
  }

  override def getConnectableCount: Int = 0

  override def getConnectableAt(index: Int): RackBusConnectable = null

  override def onActivate(player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, hitX: Float, hitY: Float): Boolean = {
    if (api.Items.get(heldItem) == api.Items.get(Constants.ItemName.Terminal)) {
      if (!getWorld.isRemote) {
        val key = UUID.randomUUID().toString
        if (!heldItem.hasTagCompound) {
          heldItem.setTagCompound(new NBTTagCompound())
        }
        else {
          keys -= heldItem.getTagCompound.getString(Settings.namespace + "key")
        }
        val maxSize = Settings.get.terminalsPerServer
        while (keys.length >= maxSize) {
          keys.remove(0)
        }
        keys += key
        heldItem.getTagCompound.setString(Settings.namespace + "key", key)
        heldItem.getTagCompound.setString(Settings.namespace + "server", getNode.getAddress)
        rack.markChanged(slot)
        player.inventory.markDirty()
      }
      true
    }
    else false
  }

  // ----------------------------------------------------------------------- //
  // Persistable

  private final val BufferTag = Settings.namespace + "buffer"
  private final val KeyboardTag = Settings.namespace + "keyboard"
  private final val KeysTag = Settings.namespace + "keys"

  override def load(nbt: NBTTagCompound): Unit = {
    if (!rack.getWorld.isRemote) {
      getNode.load(nbt)
    }
    buffer.load(nbt.getCompoundTag(BufferTag))
    keyboard.load(nbt.getCompoundTag(KeyboardTag))
    keys.clear()
    nbt.getTagList(KeysTag, NBT.TAG_STRING).foreach((tag: NBTTagString) => keys += tag.getString)
  }

  override def save(nbt: NBTTagCompound): Unit = {
    getNode.save(nbt)
    nbt.setNewCompoundTag(BufferTag, buffer.save)
    nbt.setNewCompoundTag(KeyboardTag, keyboard.save)
    nbt.setNewTagList(KeysTag, keys)
  }

  // ----------------------------------------------------------------------- //
  // ManagedEnvironment

  override def canUpdate: Boolean = true

  override def update(): Unit = {
    if (getWorld.isRemote || (getNode.getAddress != null && getNode.getNetwork != null)) {
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

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = Array(buffer.getNode, keyboard.getNode)

  // ----------------------------------------------------------------------- //
  // LifeCycle

  override def onLifecycleStateChange(state: Lifecycle.LifecycleState): Unit = if (rack.getWorld.isRemote) state match {
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
