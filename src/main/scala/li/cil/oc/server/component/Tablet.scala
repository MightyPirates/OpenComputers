package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.common.item.TabletWrapper

class Tablet(val tablet: TabletWrapper) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("tablet").
    withConnector(Settings.get.bufferTablet).
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number -- Gets the pitch of the player holding the tablet.""")
  def getPitch(context: Context, args: Arguments): Array[AnyRef] = result(tablet.player.rotationPitch)
}
