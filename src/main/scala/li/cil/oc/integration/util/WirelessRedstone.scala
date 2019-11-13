package li.cil.oc.integration.util

import li.cil.oc.server.component.RedstoneWireless

import scala.collection.mutable

object WirelessRedstone {
  val systems = mutable.Set.empty[WirelessRedstoneSystem]

  def isAvailable: Boolean = systems.nonEmpty

  def addReceiver(rs: RedstoneWireless) {
    systems.foreach(system => try system.addReceiver(rs) catch {
      case _: Throwable => // Ignore
    })
  }

  def removeReceiver(rs: RedstoneWireless) {
    systems.foreach(system => try system.removeReceiver(rs) catch {
      case _: Throwable => // Ignore
    })
  }

  def updateOutput(rs: RedstoneWireless) {
    systems.foreach(system => try system.updateOutput(rs) catch {
      case _: Throwable => // Ignore
    })
  }

  def removeTransmitter(rs: RedstoneWireless) {
    systems.foreach(system => try system.removeTransmitter(rs) catch {
      case _: Throwable => // Ignore
    })
  }

  def getInput(rs: RedstoneWireless): Boolean = systems.exists(_.getInput(rs))

  trait WirelessRedstoneSystem {
    def addReceiver(rs: RedstoneWireless)

    def removeReceiver(rs: RedstoneWireless)

    def updateOutput(rs: RedstoneWireless)

    def removeTransmitter(rs: RedstoneWireless)

    def getInput(rs: RedstoneWireless): Boolean
  }

}
