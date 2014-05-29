package li.cil.oc.common.tileentity

import li.cil.oc.api
import li.cil.oc.Settings
import li.cil.oc.api.network.{Arguments, Context, Callback, Visibility}

class Geolyzer extends traits.Environment {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("geolyzer").
    withConnector().
    create()

  override def canUpdate = false

  @Callback(doc = """function() -- Analyzes the density of the column at the specified relative coordinates.""")
  def scan(computer: Context, args: Arguments): Array[AnyRef] = {
    val rx = args.checkInteger(0)
    val rz = args.checkInteger(1)
    if (math.abs(rx) > Settings.get.geolyzerRange || math.abs(rz) > Settings.get.geolyzerRange) {
      throw new IllegalArgumentException("location out of bounds")
    }
    val bx = x + rx
    val bz = z + rz

    if (!node.tryChangeBuffer(-Settings.get.geolyzerScanCost))
      return result(Unit, "not enough energy")

    val values = new Array[Float](64)
    for (ry <- 0 until values.length) {
      val by = y + ry - 32
      if (!world.isAirBlock(bx, by, bz)) {
        val block = world.getBlock(bx, by, bz)
        if (block != null)
          values(ry) = block.getBlockHardness(world, bx, by, bz)
      }
    }

    result(values)
  }
}
