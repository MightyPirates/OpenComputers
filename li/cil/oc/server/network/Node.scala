package li.cil.oc.server.network

import java.util.logging.Level
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.network.environment.Environment
import li.cil.oc.{OpenComputers, api}
import net.minecraft.nbt.NBTTagCompound

class Node(val host: Environment, val name: String, val reachability: Visibility) extends api.network.Node {
  final var address: String = null

  final var network: api.network.Network = null

  def update() {}

  def receive(message: api.network.Message) =
    if (message.source == this) message.name match {
      case "system.connect" =>
        try {
          host.onConnect()
        } catch {
          case e: Throwable => OpenComputers.log.log(Level.WARNING, "Error in connect callback:\n", e)
        }
        null
      case "system.disconnect" =>
        try {
          host.onDisconnect()
        } catch {
          case e: Throwable => OpenComputers.log.log(Level.WARNING, "Error in disconnect callback:\n", e)
        }
        null
    } else {
      try {
        host.onMessage(message)
      } catch {
        case e: Throwable => Array(Unit, e.getMessage)
      }
    }

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) = {
    if (nbt.hasKey("oc.node.address"))
      address = nbt.getString("oc.node.address")
  }

  def save(nbt: NBTTagCompound) = {
    if (address != null)
      nbt.setString("oc.node.address", address)
  }
}