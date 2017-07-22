package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ResultWrapper.result

trait WorldControl extends WorldAware with SideRestricted {
  @Callback(doc = "function(side:number):boolean, string -- Checks the contents of the block on the specified sides and returns the findings.")
  def detect(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    val (something, what) = blockContent(side)
    result(something, what)
  }
}
