package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import li.cil.oc.server.component.NetworkCard.Packet
import li.cil.oc.util.WirelessNetwork
import li.cil.oc.{api, Settings}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

class WirelessRouter extends Router with WirelessNetwork.Endpoint {
  var strength = Settings.get.maxWirelessRange

  override def owner = this

  override def receivePacket(packet: Packet, distance: Double) {
    if (queue.size < 20) {
      queue += ForgeDirection.UNKNOWN -> packet.hop()
    }
  }

  override protected def relayPacket(sourceSide: ForgeDirection, packet: Packet) {
    super.relayPacket(sourceSide, packet)
    if (sourceSide != ForgeDirection.UNKNOWN && strength > 0) {
      val cost = Settings.get.wirelessCostPerRange
      if (cost > 0 && !Settings.get.ignorePower) {
        val connector = plugs(sourceSide.ordinal).node.asInstanceOf[Connector]
        if (connector.tryChangeBuffer(-strength * cost)) {
          for ((endpoint, distance) <- WirelessNetwork.computeReachableFrom(this)) {
            endpoint.receivePacket(packet, distance)
          }
        }
      }
    }
  }

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    if (nbt.hasKey("strength")) {
      strength = nbt.getDouble("strength") max 0 min Settings.get.maxWirelessRange
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    nbt.setDouble("strength", strength)
  }

  override protected def createNode(plug: Plug) = api.Network.newNode(plug, Visibility.Network).withConnector().create()
}
