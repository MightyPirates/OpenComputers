package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{Message, environment}

trait Environment extends environment.Environment {
  def onMessage(message: Message): Array[Object] = null

  def onConnect() {}

  def onDisconnect() {}
}
