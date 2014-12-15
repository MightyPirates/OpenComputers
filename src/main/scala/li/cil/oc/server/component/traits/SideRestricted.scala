package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import net.minecraftforge.common.util.ForgeDirection

trait SideRestricted {
  protected def checkSideForAction(args: Arguments, n: Int): ForgeDirection
}
