package li.cil.oc.server.component

import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.{Callback, Arguments, Context}
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.{Network, prefab, internal}
import li.cil.oc.integration.util.Crop
import li.cil.oc.integration.util.Crop.CropProvider
import li.cil.oc.util.BlockPosition
import net.minecraft.block._
import net.minecraft.init.{Items, Blocks}
import net.minecraft.item.Item
import net.minecraftforge.common.IPlantable
import net.minecraftforge.common.util.ForgeDirection

class UpgradeFarming(val host: EnvironmentHost with internal.Robot) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("farming").
    create()

  @Callback(doc = """function([count:number]):boolean -- checks the ripeness of the seed.""")
  def check(context: Context, args: Arguments): Array[AnyRef] = {
    val hostPos = BlockPosition(host)
    val targetPos = hostPos.offset(ForgeDirection.DOWN)
    val target = host.world.getBlock(targetPos.x, targetPos.y, targetPos.z)
    Crop.getProviderForBlock(target) match {
      case Some(provider) => provider.getInformation(BlockPosition(targetPos.x, targetPos.y, targetPos.z, host.world))
      case _ => result(Unit, "Not a crop")
    }

  }

}
