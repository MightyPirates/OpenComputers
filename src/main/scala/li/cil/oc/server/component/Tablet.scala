package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network.{Arguments, Callback, Context, Visibility}
import li.cil.oc.common.component
import li.cil.oc.common.item.TabletWrapper

class Tablet(val tablet: TabletWrapper) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("tablet").
    withConnector(Settings.get.bufferRobot).
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():boolean -- Whether the local bus interface is enabled.""")
  def getPitch(context: Context, args: Arguments): Array[AnyRef] = result(tablet.holder.rotationPitch)
}
