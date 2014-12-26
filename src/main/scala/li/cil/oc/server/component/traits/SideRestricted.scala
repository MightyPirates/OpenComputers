package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import net.minecraft.util.EnumFacing

trait SideRestricted {
  protected def checkSideForAction(args: Arguments, n: Int): EnumFacing
}
