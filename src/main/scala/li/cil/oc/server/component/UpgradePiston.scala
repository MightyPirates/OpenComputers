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
import net.minecraft.block.BlockPistonBase
import net.minecraft.init.Blocks
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.ReflectionHelper

class UpgradePiston(val host: Rotatable with EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("piston").
    withConnector().
    create()

  private lazy val tryExtend = ReflectionHelper.findMethod(classOf[BlockPistonBase], null, Array("tryExtend", "func_150079_i", "i"), classOf[World], classOf[Int], classOf[Int], classOf[Int], classOf[Int])

  @Callback(doc = """function():boolean -- Tries to push the block in front of the container of the upgrade.""")
  def push(context: Context, args: Arguments): Array[AnyRef] = {
    val hostPos = BlockPosition(host)
    val blockPos = hostPos.offset(host.facing)
    if (!host.world.isAirBlock(blockPos) && node.tryChangeBuffer(-Settings.get.pistonCost) && tryExtend.invoke(Blocks.piston, host.world, int2Integer(hostPos.x), int2Integer(hostPos.y), int2Integer(hostPos.z), int2Integer(host.facing.ordinal)).asInstanceOf[Boolean]) {
      host.world.setBlockToAir(blockPos)
      host.world.playSoundEffect(host.xPosition, host.yPosition, host.zPosition, "tile.piston.out", 0.5f, host.world.rand.nextFloat() * 0.25f + 0.6f)
      context.pause(0.5)
      result(true)
    }
    else result(false)
  }
}
