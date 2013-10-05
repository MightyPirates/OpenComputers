package li.cil.oc.api.network

/**
 * Created with IntelliJ IDEA.
 * User: lordjoda
 * Date: 04.10.13
 * Time: 17:29
 * To change this template use File | Settings | File Templates.
 */
trait  PoweredNode extends Node{
    var main:Node = null
    var demand = 2;
  override def receive(message: Message): Option[Array[Any]] = {
    val ret = super.receive(message)
    message.name match {
      case "power.connect" => {
        println("connect")
        if(main != message.source){
          println("setting main")
          main = message.source
          network.foreach(_.sendToAddress(this,message.source.address.get,"power.request",demand))
        }

      }
      case "network.disconnect"=> {if(message.source == main)main = null}
      case _ => // Ignore.
    }
    return ret
  }
}
