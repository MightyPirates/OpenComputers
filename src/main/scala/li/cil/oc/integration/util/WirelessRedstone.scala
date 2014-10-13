package li.cil.oc.integration.util

import li.cil.oc.server.component.RedstoneWireless

import scala.collection.mutable

object WirelessRedstone {
  val systems = mutable.Set.empty[WirelessRedstoneSystem]

  def isAvailable = systems.size > 0

  def addReceiver(rs: RedstoneWireless) {
    systems.foreach(_.addReceiver(rs))
  }

  def removeReceiver(rs: RedstoneWireless) {
    systems.foreach(_.removeReceiver(rs))
  }

  def updateOutput(rs: RedstoneWireless) {
    systems.foreach(_.updateOutput(rs))
  }

  def removeTransmitter(rs: RedstoneWireless) {
    systems.foreach(_.removeTransmitter(rs))
  }

  def getInput(rs: RedstoneWireless) = systems.exists(_.getInput(rs))

  trait WirelessRedstoneSystem {
    def addReceiver(rs: RedstoneWireless)

    def removeReceiver(rs: RedstoneWireless)

    def updateOutput(rs: RedstoneWireless)

    def removeTransmitter(rs: RedstoneWireless)

    def getInput(rs: RedstoneWireless): Boolean
  }

}
