package li.cil.oc.server.component

import li.cil.oc.api.driver.Host
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.component
import li.cil.oc.{Settings, api}
import net.minecraft.block.Block
import net.minecraftforge.fluids.FluidRegistry

class Geolyzer(val host: Host) extends component.ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("geolyzer").
    withConnector().
    create()

  @Callback(doc = """function(x:number, z:number[, ignoreReplaceable:boolean]):table -- Analyzes the density of the column at the specified relative coordinates.""")
  def scan(computer: Context, args: Arguments): Array[AnyRef] = {
    val rx = args.checkInteger(0)
    val rz = args.checkInteger(1)
    val includeReplaceable = !(args.count > 2 && args.checkBoolean(2))
    if (math.abs(rx) > Settings.get.geolyzerRange || math.abs(rz) > Settings.get.geolyzerRange) {
      throw new IllegalArgumentException("location out of bounds")
    }
    val (x, y, z) = ((host.xPosition - 0.5).toInt, (host.yPosition - 0.5).toInt, (host.zPosition - 0.5).toInt)
    val bx = x + rx
    val bz = z + rz

    if (!node.tryChangeBuffer(-Settings.get.geolyzerScanCost))
      return result(Unit, "not enough energy")

    val count = 64
    val noise = new Array[Byte](count)
    host.world.rand.nextBytes(noise)
    // Map to [-1, 1). The additional /33f is for normalization below.
    val values = noise.map(_ / 128f / 33f)
    for (ry <- 0 until count) {
      val by = y + ry - 32
      if (!host.world.isAirBlock(bx, by, bz)) {
        val block = host.world.getBlock(bx, by, bz)
        if (block != null && (includeReplaceable || isFluid(block) || !block.isReplaceable(host.world, x, y, z))) {
          values(ry) = values(ry) * (math.abs(ry - 32) + 1) * Settings.get.geolyzerNoise + block.getBlockHardness(host.world, bx, by, bz)
        }
      }
      else values(ry) = 0
    }

    result(values)
  }

  private def isFluid(block: Block) = FluidRegistry.lookupFluidForBlock(block) != null
}
