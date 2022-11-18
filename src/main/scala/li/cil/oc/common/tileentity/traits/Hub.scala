package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity.traits
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.MovingAverage
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.mutable

trait Hub extends traits.Environment with SidedEnvironment with Tickable {
  override def node: Node = null

  override protected def isConnected = plugs.exists(plug =>
    plug != null &&
    plug.node != null &&
    plug.node.address != null &&
    plug.node.network != null)

  protected val plugs = Direction.values.map(side => createPlug(side))

  val queue = mutable.Queue.empty[(Option[Direction], Packet)]

  var maxQueueSize = queueBaseSize

  var relayDelay = relayBaseDelay

  var relayAmount = relayBaseAmount

  var relayCooldown = -1

  // 20 cycles
  val packetsPerCycleAvg = new MovingAverage(20)

  // ----------------------------------------------------------------------- //

  protected def queueBaseSize = Settings.get.switchDefaultMaxQueueSize

  protected def queueSizePerUpgrade = Settings.get.switchQueueSizeUpgrade

  protected def relayBaseDelay = Settings.get.switchDefaultRelayDelay

  protected def relayDelayPerUpgrade = Settings.get.switchRelayDelayUpgrade

  protected def relayBaseAmount = Settings.get.switchDefaultRelayAmount

  protected def relayAmountPerUpgrade = Settings.get.switchRelayAmountUpgrade

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  override def canConnect(side: Direction) = side != null

  override def sidedNode(side: Direction) = if (side != null) plugs(side.ordinal).node else null

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (relayCooldown > 0) {
      relayCooldown -= 1
    }
    else {
      relayCooldown = -1
      if (queue.nonEmpty) queue.synchronized {
        val packetsToRely = math.min(queue.size, relayAmount)
        packetsPerCycleAvg += packetsToRely
        for (i <- 0 until packetsToRely) {
          val (sourceSide, packet) = queue.dequeue()
          relayPacket(sourceSide, packet)
        }
        if (queue.nonEmpty) {
          relayCooldown = relayDelay - 1
        }
      }
      else if (getLevel.getGameTime % relayDelay == 0) {
        packetsPerCycleAvg += 0
      }
    }
  }

  def tryEnqueuePacket(sourceSide: Option[Direction], packet: Packet) = queue.synchronized {
    if (packet.ttl > 0 && queue.size < maxQueueSize) {
      queue += sourceSide -> packet.hop()
      if (relayCooldown < 0) {
        relayCooldown = relayDelay - 1
      }
      true
    }
    else false
  }

  protected def relayPacket(sourceSide: Option[Direction], packet: Packet) {
    for (side <- Direction.values) {
      if (sourceSide.isEmpty || sourceSide.get != side) {
        val node = sidedNode(side)
        if (node != null) {
          node.sendToReachable("network.message", packet)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val PlugsTag = Settings.namespace + "plugs"
  private final val QueueTag = Settings.namespace + "queue"
  private final val SideTag = "side"
  private final val RelayCooldownTag = Settings.namespace + "relayCooldown"

  override def loadForServer(nbt: CompoundNBT) {
    super.loadForServer(nbt)
    nbt.getList(PlugsTag, NBT.TAG_COMPOUND).toTagArray[CompoundNBT].
      zipWithIndex.foreach {
      case (tag, index) => plugs(index).node.loadData(tag)
    }
    nbt.getList(QueueTag, NBT.TAG_COMPOUND).foreach(
      (tag: CompoundNBT) => {
        val side = tag.getDirection(SideTag)
        val packet = api.Network.newPacket(tag)
        queue += side -> packet
      })
    if (nbt.contains(RelayCooldownTag)) {
      relayCooldown = nbt.getInt(RelayCooldownTag)
    }
  }

  override def saveForServer(nbt: CompoundNBT) = queue.synchronized {
    super.saveForServer(nbt)
    // Side check for Waila (and other mods that may call this client side).
    if (isServer) {
      nbt.setNewTagList(PlugsTag, plugs.map(plug => {
        val plugNbt = new CompoundNBT()
        if (plug.node != null)
          plug.node.saveData(plugNbt)
        plugNbt
      }))
      nbt.setNewTagList(QueueTag, queue.map {
        case (sourceSide, packet) =>
          val tag = new CompoundNBT()
          tag.setDirection(SideTag, sourceSide)
          packet.saveData(tag)
          tag
      })
      if (relayCooldown > 0) {
        nbt.putInt(RelayCooldownTag, relayCooldown)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  protected def createPlug(side: Direction) = new Plug(side)

  protected class Plug(val side: Direction) extends api.network.Environment {
    val node = createNode(this)

    override def onMessage(message: Message) {
      if (isPrimary) {
        onPlugMessage(this, message)
      }
    }

    override def onConnect(node: Node) = onPlugConnect(this, node)

    override def onDisconnect(node: Node) = onPlugDisconnect(this, node)

    def isPrimary = plugs(plugs.indexWhere(_.node.network == node.network)) == this

    def plugsInOtherNetworks = plugs.filter(_.node.network != node.network)
  }

  protected def onPlugConnect(plug: Plug, node: Node) {}

  protected def onPlugDisconnect(plug: Plug, node: Node) {}

  protected def onPlugMessage(plug: Plug, message: Message) {
    if (message.name == "network.message" && !plugs.exists(_.node == message.source)) message.data match {
      case Array(packet: Packet) => tryEnqueuePacket(Option(plug.side), packet)
      case _ =>
    }
  }

  protected def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.Network).create()
}
