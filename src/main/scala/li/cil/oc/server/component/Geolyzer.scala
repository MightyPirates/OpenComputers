package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.internal.Rotatable
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.FluidRegistry

class Geolyzer(val host: EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("geolyzer").
    withConnector().
    create()

  @Callback(doc = """function(x:number, z:number[, ignoreReplaceable:boolean]):table -- Analyzes the density of the column at the specified relative coordinates.""")
  def scan(computer: Context, args: Arguments): Array[AnyRef] = {
    val rx = args.checkInteger(0)
    val rz = args.checkInteger(1)
    val includeReplaceable = !args.optBoolean(2, false)
    if (math.abs(rx) > Settings.get.geolyzerRange || math.abs(rz) > Settings.get.geolyzerRange) {
      throw new IllegalArgumentException("location out of bounds")
    }
    val blockPos = BlockPosition(host)
    val bx = blockPos.x + rx
    val bz = blockPos.z + rz

    if (!node.tryChangeBuffer(-Settings.get.geolyzerScanCost))
      return result(Unit, "not enough energy")

    val count = 64
    val noise = new Array[Byte](count)
    host.world.rand.nextBytes(noise)
    // Map to [-1, 1). The additional /33f is for normalization below.
    val values = noise.map(_ / 128f / 33f)
    for (ry <- 0 until count) {
      val by = blockPos.y + ry - 32
      if (!host.world.isAirBlock(bx, by, bz)) {
        val block = host.world.getBlock(bx, by, bz)
        if (block != null && (includeReplaceable || isFluid(block) || !block.isReplaceable(host.world, blockPos.x, blockPos.y, blockPos.z))) {
          values(ry) = values(ry) * (math.abs(ry - 32) + 1) * Settings.get.geolyzerNoise + block.getBlockHardness(host.world, bx, by, bz)
        }
      }
      else values(ry) = 0
    }

    result(values)
  }

  @Callback(doc = """function(side:number):table -- Get some information on a directly adjacent block.""")
  def analyze(computer: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
    val localSide = host match {
      case rotatable: Rotatable => rotatable.toLocal(side)
      case _ => side
    }
    val blockPos = BlockPosition(host).offset(localSide)
    val block = host.world.getBlock(blockPos)
    val info = Map(
      "name" -> Block.blockRegistry.getNameForObject(block),
      "metadata" -> host.world.getBlockMetadata(blockPos),
      "hardness" -> host.world.getBlockHardness(blockPos),
      "harvestLevel" -> host.world.getBlockHarvestLevel(blockPos),
      "harvestTool" -> host.world.getBlockHarvestTool(blockPos)
    )
    if (Settings.get.insertIdsInConverters)
      result(info ++ Map("id" -> Block.getIdFromBlock(block)))
    else
      result(info)
  }
  else result(Unit, "not enabled in config")

  private def isFluid(block: Block) = FluidRegistry.lookupFluidForBlock(block) != null
}
