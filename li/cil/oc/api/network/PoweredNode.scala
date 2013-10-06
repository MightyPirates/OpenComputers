package li.cil.oc.api.network


trait PoweredNode extends Node {
  var main: Node = null
  var demand = 2

  override def receive(message: Message): Option[Array[Any]] = {
    val ret = super.receive(message)
    message.name match {
      case "power.connect" => {
        if (main != message.source) {
          if (main != null)
            network.foreach(_.sendToAddress(this, main.address.get, "power.disconnect"))
          main = message.source
          network.foreach(_.sendToAddress(this, message.source.address.get, "power.request", demand, 1))
        }

      }
      case "network.disconnect" => {
        if (message.source == main) main = null
      }
      case _ => // Ignore.
    }
    ret
  }

  override protected def onDisconnect() {
    println("sending disc")
    network.foreach(_.sendToAddress(this, main.address.get, "power.disconnect"))

    super.onDisconnect()
  }
}
