package li.cil.oc.common.tileentity.traits

import li.cil.oc.server.PacketSender

import scala.collection.mutable

trait SwitchLike extends Hub {
  def relayDelay: Int

  def isWirelessEnabled: Boolean

  def isLinkedEnabled: Boolean

  val computers = mutable.Buffer.empty[AnyRef]

  val openPorts = mutable.Map.empty[AnyRef, mutable.Set[Int]]

  var lastMessage = 0L

  def onSwitchActivity(): Unit = {
    val now = System.currentTimeMillis()
    if (now - lastMessage >= (relayDelay - 1) * 50) {
      lastMessage = now
      PacketSender.sendSwitchActivity(this)
    }
  }
}
