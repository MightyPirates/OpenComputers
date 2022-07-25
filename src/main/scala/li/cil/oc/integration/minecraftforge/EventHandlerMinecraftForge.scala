package li.cil.oc.integration.minecraftforge

import li.cil.oc.OpenComputers
import li.cil.oc.common.tileentity.traits.PowerAcceptor
import li.cil.oc.integration.util.Power
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.common.util.NonNullSupplier
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.energy.IEnergyStorage
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object EventHandlerMinecraftForge {

  @SubscribeEvent
  def onAttachCapabilities(event: AttachCapabilitiesEvent[TileEntity]): Unit = {
    event.getObject match {
      case tileEntity: PowerAcceptor =>
        val provider = new Provider(tileEntity)
        event.addCapability(ProviderEnergy, provider)
        event.addListener(new Runnable {
          override def run = provider.invalidate
        })
      case _ =>
    }
  }

  def canCharge(stack: ItemStack): Boolean =
    stack.getCapability(CapabilityEnergy.ENERGY, null).orElse(null) match {
      case storage: IEnergyStorage => storage.canReceive
      case _ => false
    }

  def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double =
    stack.getCapability(CapabilityEnergy.ENERGY, null).orElse(null) match {
      case storage: IEnergyStorage => amount - Power.fromRF(storage.receiveEnergy(Power.toRF(amount), simulate))
      case _ => amount
    }

  val ProviderEnergy: ResourceLocation = new ResourceLocation(OpenComputers.ID, "forgeenergy")

  class Provider(tile: PowerAcceptor) extends ICapabilityProvider {

    private val providers = Direction.values.map(side => LazyOptional.of(new NonNullSupplier[EnergyStorageImpl] {
      override def get = new EnergyStorageImpl(tile, side)
    }))
    private val nullProvider = LazyOptional.of(new NonNullSupplier[EnergyStorageImpl] {
      override def get = new EnergyStorageImpl(tile, null)
    })

    def invalidate(): Unit = {
      for (provider <- providers) provider.invalidate
      nullProvider.invalidate
    }

    override def getCapability[T](capability: Capability[T], facing: Direction): LazyOptional[T] = {
      if (capability == CapabilityEnergy.ENERGY) {
        (if (facing == null) nullProvider.cast[T] else providers(facing.get3DDataValue)).cast[T]
      } else LazyOptional.empty[T]
    }

    class EnergyStorageImpl(val tile: PowerAcceptor, val side: Direction) extends IEnergyStorage {

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
