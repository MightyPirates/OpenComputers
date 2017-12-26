package li.cil.oc.common.tileentity

import li.cil.oc.Constants
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

// Removed in MC 1.11
class AccessPoint extends Switch with WirelessEndpoint with traits.PowerAcceptor {
  var strength = Settings.get.maxWirelessRange(Tier.Two)

  var isRepeater = true

  val componentNodes = Array.fill(6)(api.Network.newNode(this, Visibility.Network).
    withComponent("access_point").
    create())

  override def isWirelessEnabled = true

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: EnumFacing) = true

  override protected def connector(side: EnumFacing) = sidedNode(side) match {
    case connector: Connector => Option(connector)
    case _ => None
  }

  override def energyThroughput = Settings.get.accessPointRate

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = {
    player.addChatMessage(Localization.Analyzer.WirelessStrength(strength))
    Array(componentNodes(side.getIndex))
  }

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():number -- Get the signal strength (range) used when relaying messages.""")
  def getStrength(context: Context, args: Arguments): Array[AnyRef] = synchronized(result(strength))

  @Callback(doc = """function(strength:number):number -- Set the signal strength (range) used when relaying messages.""")
  def setStrength(context: Context, args: Arguments): Array[AnyRef] = synchronized {
    strength = math.max(args.checkDouble(0), math.min(0, Settings.get.maxWirelessRange(Tier.Two)))
    result(strength)
  }

  @Callback(direct = true, doc = """function():boolean -- Get whether the access point currently acts as a repeater (resend received wireless packets wirelessly).""")
  def isRepeater(context: Context, args: Arguments): Array[AnyRef] = synchronized(result(isRepeater))

  @Callback(doc = """function(enabled:boolean):boolean -- Set whether the access point should act as a repeater.""")
  def setRepeater(context: Context, args: Arguments): Array[AnyRef] = synchronized {
    isRepeater = args.checkBoolean(0)
    result(isRepeater)
  }

  // ----------------------------------------------------------------------- //

  override def receivePacket(packet: Packet, source: WirelessEndpoint) {
    tryEnqueuePacket(None, packet)
  }

  override protected def relayPacket(sourceSide: Option[EnumFacing], packet: Packet) {
    super.relayPacket(sourceSide, packet)
    if (strength > 0 && (sourceSide.isDefined || isRepeater)) {
      val cost = Settings.get.wirelessCostPerRange(Tier.Two)
      val tryChangeBuffer = sourceSide match {
        case Some(side) =>
          (amount: Double) => plugs(side.ordinal).node.asInstanceOf[Connector].tryChangeBuffer(amount)
        case _ =>
          (amount: Double) => plugs.exists(_.node.asInstanceOf[Connector].tryChangeBuffer(amount))
      }
      if (tryChangeBuffer(-strength * cost)) {
        api.Network.sendWirelessPacket(this, strength, packet)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def createNode(plug: Plug) = api.Network.newNode(plug, Visibility.Network).
    withConnector(math.round(Settings.get.bufferAccessPoint)).
    create()

  override protected def onPlugConnect(plug: Plug, node: Node) {
    super.onPlugConnect(plug, node)
    if (node == plug.node) {
      api.Network.joinWirelessNetwork(this)
    }
    if (plug.isPrimary)
      plug.node.connect(componentNodes(plug.side.ordinal()))
    else
      componentNodes(plug.side.ordinal).remove()
  }

  override protected def onPlugDisconnect(plug: Plug, node: Node) {
    super.onPlugDisconnect(plug, node)
    if (node == plug.node) {
      api.Network.leaveWirelessNetwork(this)
    }
    if (plug.isPrimary && node != plug.node)
      plug.node.connect(componentNodes(plug.side.ordinal()))
    else
      componentNodes(plug.side.ordinal).remove()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) = {
    super.readFromNBTForServer(nbt)
    if (nbt.hasKey(Settings.namespace + "strength")) {
      strength = nbt.getDouble(Settings.namespace + "strength") max 0 min Settings.get.maxWirelessRange(Tier.Two)
    }
    if (nbt.hasKey(Settings.namespace + "isRepeater")) {
      isRepeater = nbt.getBoolean(Settings.namespace + "isRepeater")
    }
    nbt.getTagList(Settings.namespace + "componentNodes", NBT.TAG_COMPOUND).toArray[NBTTagCompound].
      zipWithIndex.foreach {
      case (tag, index) => componentNodes(index).load(tag)
    }
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) = {
    super.writeToNBTForServer(nbt)
    nbt.setDouble(Settings.namespace + "strength", strength)
    nbt.setBoolean(Settings.namespace + "isRepeater", isRepeater)
    nbt.setNewTagList(Settings.namespace + "componentNodes", componentNodes.map {
      case node: Node =>
        val tag = new NBTTagCompound()
        node.save(tag)
        tag
      case _ => new NBTTagCompound()
    })
  }
}
