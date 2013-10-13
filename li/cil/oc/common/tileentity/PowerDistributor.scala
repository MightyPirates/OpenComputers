package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import scala.collection.mutable

class PowerDistributor  extends Rotatable with Provider {


  MAXENERGY = 2000.0

  override val name = "powerdistributor"

  override val visibility = Visibility.Network




}
