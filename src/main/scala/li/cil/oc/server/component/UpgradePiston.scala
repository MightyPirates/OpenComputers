package li.cil.oc.server.component

import cpw.mods.fml.relauncher.ReflectionHelper
import li.cil.oc.Settings
import li.cil.oc.api.driver.Container
import li.cil.oc.api.network.{Arguments, Callback, Context, Visibility}
import li.cil.oc.api.{Network, Rotatable}
import li.cil.oc.common.component
import net.minecraft.block.{Block, BlockPistonBase}
import net.minecraft.world.World

class UpgradePiston(val container: Rotatable with Container) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("piston").
    withConnector().
    create()

  private lazy val tryExtend = ReflectionHelper.findMethod(classOf[BlockPistonBase], null, Array("tryExtend", "func_72115_j", "f"), classOf[World], classOf[Int], classOf[Int], classOf[Int], classOf[Int])

  @Callback(doc = """function(side:number):boolean -- Tries to push the block in front of the container of the upgrade.""")
  def push(context: Context, args: Arguments): Array[AnyRef] = {
    val (x, y, z) = (math.floor(container.xPosition).toInt, math.floor(container.yPosition).toInt, math.floor(container.zPosition).toInt)
    val (bx, by, bz) = (x + container.facing.offsetX, y + container.facing.offsetY, z + container.facing.offsetZ)
    if (!container.world.isAirBlock(bx, by, bz) && node.tryChangeBuffer(-Settings.get.pistonCost) && tryExtend.invoke(Block.pistonBase, container.world, x.underlying(), y.underlying(), z.underlying(), container.facing.ordinal.underlying()).asInstanceOf[Boolean]) {
      container.world.setBlockToAir(bx, by, bz)
      container.world.playSoundEffect(container.xPosition, container.yPosition, container.zPosition, "tile.piston.out", 0.5f, container.world.rand.nextFloat() * 0.25f + 0.6f)
      context.pause(0.5)
      result(true)
    }
    else result(false)
  }
}
