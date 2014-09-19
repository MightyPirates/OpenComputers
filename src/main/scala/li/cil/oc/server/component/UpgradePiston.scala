package li.cil.oc.server.component

import cpw.mods.fml.relauncher.ReflectionHelper
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.tileentity.Rotatable
import li.cil.oc.common.component
import net.minecraft.block.BlockPistonBase
import net.minecraft.init.Blocks
import net.minecraft.world.World

class UpgradePiston(val host: Rotatable with EnvironmentHost) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("piston").
    withConnector().
    create()

  private lazy val tryExtend = ReflectionHelper.findMethod(classOf[BlockPistonBase], null, Array("tryExtend", "func_150079_i", "i"), classOf[World], classOf[Int], classOf[Int], classOf[Int], classOf[Int])

  @Callback(doc = """function(side:number):boolean -- Tries to push the block in front of the container of the upgrade.""")
  def push(context: Context, args: Arguments): Array[AnyRef] = {
    val (x, y, z) = (math.floor(host.xPosition).toInt, math.floor(host.yPosition).toInt, math.floor(host.zPosition).toInt)
    val (bx, by, bz) = (x + host.facing.offsetX, y + host.facing.offsetY, z + host.facing.offsetZ)
    if (!host.world.isAirBlock(bx, by, bz) && node.tryChangeBuffer(-Settings.get.pistonCost) && tryExtend.invoke(Blocks.piston, host.world, x.underlying(), y.underlying(), z.underlying(), host.facing.ordinal.underlying()).asInstanceOf[Boolean]) {
      host.world.setBlockToAir(bx, by, bz)
      host.world.playSoundEffect(host.xPosition, host.yPosition, host.zPosition, "tile.piston.out", 0.5f, host.world.rand.nextFloat() * 0.25f + 0.6f)
      context.pause(0.5)
      result(true)
    }
    else result(false)
  }
}
