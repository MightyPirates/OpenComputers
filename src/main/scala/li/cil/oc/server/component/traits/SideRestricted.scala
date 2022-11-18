package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import net.minecraft.util.Direction

trait SideRestricted {
  protected def checkSideForAction(args: Arguments, n: Int): Direction
}
