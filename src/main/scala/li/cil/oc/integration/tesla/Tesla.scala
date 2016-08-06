package li.cil.oc.integration.tesla

import li.cil.oc.OpenComputers
import li.cil.oc.common.tileentity.traits.PowerAcceptor
import li.cil.oc.integration.util.Power
import net.darkhax.tesla.api.ITeslaConsumer
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object Tesla {
  val PROVIDER_TESLA: ResourceLocation = new ResourceLocation(OpenComputers.ID, "tesla")

  def init(): Unit = {
    MinecraftForge.EVENT_BUS.register(TeslaConsumerCapability.INSTANCE)
  }

  @SubscribeEvent
  def onAttachCapabilities(event: AttachCapabilitiesEvent.TileEntity) {
    event.getTileEntity match {
      case tileEntity: PowerAcceptor =>
        event.addCapability(PROVIDER_TESLA, new TeslaConsumerCapabilityProvider(tileEntity))
      case _ => // Ignore.
    }
  }

  class TeslaConsumerCapabilityProvider(tileEntity: PowerAcceptor) extends ICapabilityProvider {
    private val providers = EnumFacing.VALUES.map(side => new TeslaConsumerImpl(tileEntity, side))

    override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = capability == TeslaConsumerCapability.CONSUMER_CAPABILITY

    override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
      if (capability == TeslaConsumerCapability.CONSUMER_CAPABILITY) {
        providers(facing.getIndex).asInstanceOf[T]
      } else throw new IllegalArgumentException("Capability not supported, check via hasCapability first!")
    }
  }

  class TeslaConsumerImpl(val tileEntity: PowerAcceptor, val side: EnumFacing) extends ITeslaConsumer {
    override def givePower(power: Long, simulated: Boolean): Long = {
      Power.toTesla(tileEntity.tryChangeBuffer(side, Power.fromTesla(power), !simulated))
    }
  }

}
