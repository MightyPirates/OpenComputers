package li.cil.oc.common.container

import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.client.Textures
import li.cil.oc.common
import net.minecraft.inventory.IInventory
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class StaticComponentSlot(val agentContainer: Player, inventory: IInventory, index: Int, x: Int, y: Int, host: Class[_ <: EnvironmentHost], val slot: String, val tier: Int)
  extends ComponentSlot(inventory, index, x, y, host) {

  @OnlyIn(Dist.CLIENT)
  def tierIcon = Textures.Icons.get(tier)

  @OnlyIn(Dist.CLIENT)
  override def getBackgroundLocation: ResourceLocation = Textures.Icons.get(slot)

  override def getMaxStackSize =
    slot match {
      case common.Slot.Tool | common.Slot.Any | common.Slot.Filtered => super.getMaxStackSize
      case common.Slot.None => 0
      case _ => 1
    }
}
