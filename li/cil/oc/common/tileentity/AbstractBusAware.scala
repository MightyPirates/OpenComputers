package li.cil.oc.common.tileentity

import cpw.mods.fml.common.{Loader, Optional}
import li.cil.oc.Items
import li.cil.oc.api.network.Node
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import stargatetech2.api.bus.{BusEvent, IBusDevice}

@Optional.Interface(iface = "stargatetech2.api.bus.IBusDevice", modid = "StargateTech2")
trait AbstractBusAware extends TileEntity with ComponentInventory with IBusDevice {
  def getInterfaces(side: Int) =
    if (hasAbstractBusCard) {
      components collect {
        case Some(abstractBus: component.AbstractBus) => abstractBus.busInterface
      }
    }
    else null

  def getXCoord = x

  def getYCoord = y

  def getZCoord = z

  protected def hasAbstractBusCard = components exists {
    case Some(_: component.AbstractBus) => true
    case _ => false
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    if (Items.abstractBus.parent.subItem(stack) == Items.abstractBus) {
      // Trigger network re-map after another interface was added.
      addAbstractBus()
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (Items.abstractBus.parent.subItem(stack) == Items.abstractBus) {
      if (!hasAbstractBusCard) {
        // Last interface was removed, trigger removal. This is the case when
        // the last abstract bus card was removed.
        removeAbstractBus(force = true)
      }
      else {
        // Trigger network re-map if some interface still remains. This is the
        // case when one of multiple abstract bus cards was removed.
        addAbstractBus()
      }
    }
  }

  abstract override def onConnect(node: Node) {
    super.onConnect(node)
    addAbstractBus()
  }

  abstract override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    removeAbstractBus()
  }

  protected def addAbstractBus() {
    // Mod loaded check to avoid class not found errors.
    if (Loader.isModLoaded("StargateTech2") && hasAbstractBusCard) {
      MinecraftForge.EVENT_BUS.post(new BusEvent.AddToNetwork(world, x, y, z))
    }
  }

  protected def removeAbstractBus(force: Boolean = false) {
    // Mod loaded check to avoid class not found errors.
    if (Loader.isModLoaded("StargateTech2") && (hasAbstractBusCard || force)) {
      MinecraftForge.EVENT_BUS.post(new BusEvent.RemoveFromNetwork(world, x, y, z))
    }
  }
}
