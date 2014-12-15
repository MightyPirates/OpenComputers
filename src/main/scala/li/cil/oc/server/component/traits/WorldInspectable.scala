package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ResultWrapper.result

trait WorldInspectable extends WorldAware with SideRestricted {
  @Callback
  def detect(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    val (something, what) = blockContent(side)
    result(something, what)
  }
}
