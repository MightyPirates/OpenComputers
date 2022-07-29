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
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.StringNBT
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.convert.ImplicitConversionsToJava._
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
      override def isUsableByPlayer(keyboard: api.internal.Keyboard, player: PlayerEntity) = {
        val stack = player.getItemInHand(Hand.MAIN_HAND)
        stack.getItem match {
          case t: item.Terminal if stack.hasTag => sidedKeys.contains(stack.getTag.getString(Settings.namespace + "key"))
          case _ => false
        }
      }
    })
    keyboard
  }

  var range = Settings.get.maxWirelessRange(Tier.Two)
  val keys = mutable.ListBuffer.empty[String]

  def hasAddress: Boolean = {
    if (rack != null) {
      val data = rack.getMountableData(slot)
      if (data != null) {
        return data.contains("terminalAddress")
      }
    }
    false
  }

  def address: String = rack.getMountableData(slot).getString("terminalAddress")

  def sidedKeys = {
    if (!rack.world.isClientSide) keys
    else rack.getMountableData(slot).getList("keys", NBT.TAG_STRING).map((tag: StringNBT) => tag.getAsString)
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

  override def getData: CompoundNBT = {
    if (node.address == null) api.Network.joinNewNetwork(node)

    val nbt = new CompoundNBT()
    nbt.setNewTagList("keys", keys)
    nbt.putString("terminalAddress", node.address)
    nbt
  }

  override def getConnectableCount: Int = 0

  override def getConnectableAt(index: Int): RackBusConnectable = null

  override def onActivate(player: PlayerEntity, hand: Hand, heldItem: ItemStack, hitX: Float, hitY: Float): Boolean = {
    if (api.Items.get(heldItem) == api.Items.get(Constants.ItemName.Terminal)) {
      if (!world.isClientSide) {
        val key = UUID.randomUUID().toString
        keys -= heldItem.getOrCreateTag.getString(Settings.namespace + "key")
        val maxSize = Settings.get.terminalsPerServer
        while (keys.length >= maxSize) {
          keys.remove(0)
        }
        keys += key
        heldItem.getTag.putString(Settings.namespace + "key", key)
        heldItem.getTag.putString(Settings.namespace + "server", node.address)
        rack.markChanged(slot)
        player.inventory.setChanged()
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

  override def loadData(nbt: CompoundNBT): Unit = {
    if (!rack.world.isClientSide) {
      node.loadData(nbt)
    }
    buffer.loadData(nbt.getCompound(BufferTag))
    keyboard.loadData(nbt.getCompound(KeyboardTag))
    keys.clear()
    nbt.getList(KeysTag, NBT.TAG_STRING).foreach((tag: StringNBT) => keys += tag.getAsString)
  }

  override def saveData(nbt: CompoundNBT): Unit = {
    node.saveData(nbt)
    nbt.setNewCompoundTag(BufferTag, buffer.saveData)
    nbt.setNewCompoundTag(KeyboardTag, keyboard.saveData)
    nbt.setNewTagList(KeysTag, keys)
  }

  // ----------------------------------------------------------------------- //
  // ManagedEnvironment

  override def canUpdate: Boolean = true

  override def update(): Unit = {
    if (world.isClientSide || (node.address != null && node.network != null)) {
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

  override def onAnalyze(player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = Array(buffer.node, keyboard.node)

  // ----------------------------------------------------------------------- //
  // LifeCycle

  override def onLifecycleStateChange(state: Lifecycle.LifecycleState): Unit = if (rack.world.isClientSide) state match {
    case Lifecycle.LifecycleState.Initialized =>
      TerminalServer.loaded.add(this)
    case Lifecycle.LifecycleState.Disposed =>
      TerminalServer.loaded.remove(this)
    case _ => // Ignore.
  }
}

object TerminalServer {
  val loaded = new TerminalServerCache()

  // we need a smart cache because nodes are loaded in before they have addresses
  // and we need a unique set of terminal servers based on address
  // This cache acts as a Map[address: String, term: TerminalServer]
  // But it can store terminals before they have an address
  // Null-address terminals are not available for binding
  // As an address loads, repeated addresses are dropped from the list
  class TerminalServerCache {

    private val ready: mutable.Map[String, TerminalServer] = new mutable.HashMap[String, TerminalServer]()
    private val pending: mutable.Buffer[TerminalServer] = mutable.Buffer.empty[TerminalServer]

    private def completePending(): Unit = {
      val promoted: mutable.Buffer[TerminalServer] = mutable.Buffer.empty[TerminalServer]
      pending.foreach { term => if (term.hasAddress)
        promoted += term
      }
      promoted.foreach { term =>
        pending -= term
        val address = term.address
        if (!ready.contains(address)) {
          ready.put(address, term)
        }
      }
    }

    def add(terminal: TerminalServer): Boolean = {
      completePending()
      if (terminal.hasAddress) {
        val newAddress: String = terminal.address
        if (ready.contains(newAddress)) {
          false
        } else {
          ready.put(newAddress, terminal)
          true
        }
      }
      else {
        pending += terminal
        true
      }
    }

    def remove(terminal: TerminalServer): Boolean = {
      completePending()
      if (terminal.hasAddress)
        ready.remove(terminal.address).isDefined
      else {
        val before = pending.size
        pending -= terminal
        pending.size > before
      }
    }

    def clear(): Unit = {
      ready.clear()
      pending.clear()
    }

    def find(address: String): Option[TerminalServer] = {
      completePending()
      ready.getOrDefault(address, null) match {
        case term: TerminalServer => Option(term)
        case _ => None
      }
    }
  }
}
