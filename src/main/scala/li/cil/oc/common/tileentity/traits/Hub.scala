package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity.traits
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.MovingAverage
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable

trait Hub extends traits.Environment with SidedEnvironment {
  override def node: Node = null

  override protected def isConnected = plugs.exists(plug =>
    plug != null &&
    plug.node != null &&
    plug.node.address != null &&
    plug.node.network != null)

  protected val plugs = ForgeDirection.VALID_DIRECTIONS.map(side => createPlug(side))

  val queue = mutable.Queue.empty[(Option[ForgeDirection], Packet)]

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

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = side != ForgeDirection.UNKNOWN

  override def sidedNode(side: ForgeDirection) = if (side != ForgeDirection.UNKNOWN) plugs(side.ordinal()).node else null

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
      else if (world.getTotalWorldTime % relayDelay == 0) {
        packetsPerCycleAvg += 0
      }
    }
  }

  def tryEnqueuePacket(sourceSide: Option[ForgeDirection], packet: Packet) = queue.synchronized {
    if (packet.ttl > 0 && queue.size < maxQueueSize) {
      queue += sourceSide -> packet.hop()
      if (relayCooldown < 0) {
        relayCooldown = relayDelay - 1
      }
      true
    }
    else false
  }

  protected def relayPacket(sourceSide: Option[ForgeDirection], packet: Packet) {
    for (side <- ForgeDirection.VALID_DIRECTIONS) {
      if (sourceSide.isEmpty || sourceSide.get != side) {
        val node = sidedNode(side)
        if (node != null) {
          node.sendToReachable("network.message", packet)
        }
      }
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    nbt.getTagList(Settings.namespace + "plugs", NBT.TAG_COMPOUND).toArray[NBTTagCompound].
      zipWithIndex.foreach {
      case (tag, index) => plugs(index).node.load(tag)
    }
    nbt.getTagList(Settings.namespace + "queue", NBT.TAG_COMPOUND).foreach(
      (tag: NBTTagCompound) => {
        val side = tag.getDirection("side")
        val packet = api.Network.newPacket(tag)
        queue += side -> packet
      })
    if (nbt.hasKey(Settings.namespace + "relayCooldown")) {
      relayCooldown = nbt.getInteger(Settings.namespace + "relayCooldown")
    }
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) = queue.synchronized {
    super.writeToNBTForServer(nbt)
    // Side check for Waila (and other mods that may call this client side).
    if (isServer) {
      nbt.setNewTagList(Settings.namespace + "plugs", plugs.map(plug => {
        val plugNbt = new NBTTagCompound()
        if (plug.node != null)
          plug.node.save(plugNbt)
        plugNbt
      }))
      nbt.setNewTagList(Settings.namespace + "queue", queue.map {
        case (sourceSide, packet) =>
          val tag = new NBTTagCompound()
          tag.setDirection("side", sourceSide)
          packet.save(tag)
          tag
      })
      if (relayCooldown > 0) {
        nbt.setInteger(Settings.namespace + "relayCooldown", relayCooldown)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  protected def createPlug(side: ForgeDirection) = new Plug(side)

  protected class Plug(val side: ForgeDirection) extends api.network.Environment {
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
