package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.internal.Rotatable
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.init.Blocks

class UpgradePiston(val host: Rotatable with EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("piston").
    withConnector().
    create()

  @Callback(doc = """function():boolean -- Tries to push the block in front of the container of the upgrade.""")
  def push(context: Context, args: Arguments): Array[AnyRef] = {
    val hostPos = BlockPosition(host)
    val blockPos = hostPos.offset(host.facing)
    if (!host.world.isAirBlock(blockPos) && node.tryChangeBuffer(-Settings.get.pistonCost) && Blocks.piston.doMove(host.world, hostPos.toBlockPos, host.facing, true)) {
      host.world.setBlockToAir(blockPos)
      host.world.playSoundEffect(host.xPosition, host.yPosition, host.zPosition, "tile.piston.out", 0.5f, host.world.rand.nextFloat() * 0.25f + 0.6f)
      context.pause(0.5)
      result(true)
    }
    else result(false)
  }
}
