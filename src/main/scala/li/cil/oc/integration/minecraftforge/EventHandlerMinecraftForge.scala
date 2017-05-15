package li.cil.oc.integration.minecraftforge

import li.cil.oc.OpenComputers
import li.cil.oc.common.tileentity.traits.PowerAcceptor
import li.cil.oc.integration.util.Power
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.energy.IEnergyStorage
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EventHandlerMinecraftForge {

  @SubscribeEvent
  def onAttachCapabilities(event: AttachCapabilitiesEvent[TileEntity]): Unit = {
    event.getObject match {
      case tileEntity: PowerAcceptor =>
        event.addCapability(ProviderEnergy, new Provider(tileEntity))
      case _ =>
    }
  }

  def canCharge(stack: ItemStack): Boolean =
    if (stack.hasCapability(CapabilityEnergy.ENERGY, null)) stack.getCapability(CapabilityEnergy.ENERGY, null) match {
      case storage: IEnergyStorage => storage.canReceive
      case _ => false
    } else false

  def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double =
    if (stack.hasCapability(CapabilityEnergy.ENERGY, null)) stack.getCapability(CapabilityEnergy.ENERGY, null) match {
      case storage: IEnergyStorage => Power.fromRF(storage.receiveEnergy(Power.toRF(amount), simulate))
      case _ => 0.0
    } else 0.0

  val ProviderEnergy: ResourceLocation = new ResourceLocation(OpenComputers.ID, "forgeenergy")

  class Provider(tile: PowerAcceptor) extends ICapabilityProvider {

    private val providers = EnumFacing.VALUES.map(side => new EnergyStorageImpl(tile, side))
    private val nullProvider = new EnergyStorageImpl(tile, null)

    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = capability == CapabilityEnergy.ENERGY

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
      if (capability == CapabilityEnergy.ENERGY) {
        (if (facing == null) nullProvider else providers(facing.getIndex)).asInstanceOf[T]
      } else null.asInstanceOf[T]
    }

    class EnergyStorageImpl(val tile: PowerAcceptor, val side: EnumFacing) extends IEnergyStorage {

      override def getEnergyStored: Int = Power.toRF(tile.globalBuffer(side))

      override def getMaxEnergyStored: Int = Power.toRF(tile.globalBufferSize(side))

      override def canReceive: Boolean = tile.canConnectPower(side)

      override def receiveEnergy(maxReceive: Int, simulate: Boolean): Int = {
        Power.toRF(tile.tryChangeBuffer(side, Power.fromRF(maxReceive), !simulate))
      }

      override def canExtract: Boolean = false

      override def extractEnergy(maxExtract: Int, simulate: Boolean): Int = 0
    }

  }

}
