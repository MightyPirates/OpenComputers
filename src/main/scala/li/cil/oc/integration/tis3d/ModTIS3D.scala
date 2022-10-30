package li.cil.oc.integration.tis3d

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.tis3d.api.serial.SerialInterfaceProvider
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.scorge.lang.ScorgeModLoadingContext

object ModTIS3D extends ModProxy {
  override def getMod = Mods.TIS3D

  @SubscribeEvent
  def registerSerialInterfaceProviders(e: RegistryEvent.Register[SerialInterfaceProvider]) {
    e.getRegistry.register(SerialInterfaceProviderAdapter)
  }

  override def preInitialize(): Unit = {
    ScorgeModLoadingContext.get.getModEventBus.register(this)
  }
}
