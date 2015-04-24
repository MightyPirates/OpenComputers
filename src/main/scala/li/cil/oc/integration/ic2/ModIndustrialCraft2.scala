package li.cil.oc.integration.ic2

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.MinecraftForge

object ModIndustrialCraft2 extends ModProxy {
  override def getMod = Mods.IndustrialCraft2

  override def initialize() {
    FMLInterModComms.sendMessage(Mods.IDs.OpenComputers, "registerToolDurabilityProvider", "li.cil.oc.integration.ic2.EventHandlerIndustrialCraft2.getDurability")
    FMLInterModComms.sendMessage(Mods.IDs.OpenComputers, "registerWrenchTool", "li.cil.oc.integration.ic2.EventHandlerIndustrialCraft2.useWrench")
    val chargerNbt = new NBTTagCompound()
    chargerNbt.setString("name", "IndustrialCraft2")
    chargerNbt.setString("canCharge", "li.cil.oc.integration.ic2.EventHandlerIndustrialCraft2.canCharge")
    chargerNbt.setString("charge", "li.cil.oc.integration.ic2.EventHandlerIndustrialCraft2.charge")
    FMLInterModComms.sendMessage(Mods.IDs.OpenComputers, "registerItemCharge", chargerNbt)

    MinecraftForge.EVENT_BUS.register(EventHandlerIndustrialCraft2)

    Driver.add(new DriverEnergyConductor)
    Driver.add(new DriverEnergySink)
    Driver.add(new DriverEnergySource)
    Driver.add(new DriverEnergyStorage)
    Driver.add(new DriverMassFab)
    Driver.add(new DriverReactor)
    Driver.add(new DriverReactorChamber)

    Driver.add(new ConverterElectricItem)
  }
}
