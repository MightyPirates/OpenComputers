package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import net.minecraft.item.ItemStack
import stargatetech2.api.bus.IBusDevice

@Optional.Interface(iface = "stargatetech2.api.bus.IBusDevice", modid = "StargateTech2")
trait AbstractBusAware extends TileEntity with Inventory with IBusDevice {
  def getInterfaces(side: Int) = if (hasAbstractBusCard) Array(null) else null

  def getXCoord = x

  def getYCoord = y

  def getZCoord = z

  protected def hasAbstractBusCard = false

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    // TODO if card wasn't present, send device added event
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    // TODO if no card is present anymore, send device removed event
  }
}
