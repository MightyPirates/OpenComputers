package li.cil.oc.integration.appeng

import appeng.api.parts.LayerBase
import li.cil.oc.api.network._
import net.minecraftforge.common.util.ForgeDirection

class LayerSidedEnvironment extends LayerBase with SidedEnvironment {
  override def sidedNode(side: ForgeDirection) = getPart(side) match {
    case env: SidedEnvironment => env.sidedNode(side)
    case _ => null
  }

  override def canConnect(side: ForgeDirection) = getPart(side) match {
    case env: SidedEnvironment => env.canConnect(side)
    case _ => false
  }
}
