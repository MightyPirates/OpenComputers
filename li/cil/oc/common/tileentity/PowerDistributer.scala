package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{PoweredNode, Message, Visibility, Node}

/**
 * Created with IntelliJ IDEA.
 * User: lordjoda
 * Date: 03.10.13
 * Time: 19:51
 * To change this template use File | Settings | File Templates.
 */
class PowerDistributer extends Rotatable with PoweredNode {

  var powerDemand:Int = 0
  override def name = "powerdistributer"

  override def visibility = Visibility.Network


  override def receive(message: Message): Option[Array[Any]] = {

    message.name match {
      case "network.disconnect"=> {
        println("recieved disc")
        if(message.source == main){
          main = this
          network.foreach(_.sendToAddress(this,address.get,"power.request",demand))
          network.foreach(_.sendToVisible(this, "power.connect"))
        }
      }
      case _ => // Ignore.
    }
    val ret = super.receive(message)
    message.name match {
      case "network.connect"=>{
        if(main==this){
          network.foreach(_.sendToAddress(this,message.source.address.get,"power.connect"))
        }
      }
      case  "power.find"=>{
        if(main==this){
          network.foreach(_.sendToAddress(this,message.source.address.get,"power.connect"))
          message.cancel()
        }
      }
      case "power.request"=>{
        println("recieved power request")
        if(main == this){
          println("this is main")
          message.data match     {
            case Array(value:Int)=> {
              powerDemand+=value
              println("now demanding "+powerDemand)
            }
            case _ => // Ignore.
          }
        }
      }
      case _ => // Ignore.
    }
    return ret
  }

  override protected def onConnect() {
    network.foreach(_.sendToVisible(this, "power.find"))
    if(main==null)
      { main = this
      network.foreach(_.sendToAddress(this,address.get,"power.request",demand))
      }
    super.onConnect()
  }
}
