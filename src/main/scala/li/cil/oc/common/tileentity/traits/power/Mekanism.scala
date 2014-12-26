//package li.cil.oc.common.tileentity.traits.power
//
//import net.minecraftforge.fml.common.Optional
//import li.cil.oc.Settings
//import li.cil.oc.common.asm.Injectable
//import li.cil.oc.integration.Mods
//import net.minecraft.util.EnumFacing
//
//@Injectable.Interface(value = "mekanism.api.energy.IStrictEnergyAcceptor", modid = Mods.IDs.Mekanism)
//trait Mekanism extends Common {
//  @Optional.Method(modid = Mods.IDs.Mekanism)
//  def canReceiveEnergy(side: EnumFacing) = Mods.Mekanism.isAvailable && canConnectPower(side)
//
//  @Optional.Method(modid = Mods.IDs.Mekanism)
//  def transferEnergyToAcceptor(side: EnumFacing, amount: Double) =
//    if (!Mods.Mekanism.isAvailable) 0
//    else amount - tryChangeBuffer(side, amount * Settings.get.ratioMekanism) / Settings.get.ratioMekanism
//
//  @Optional.Method(modid = Mods.IDs.Mekanism)
//  def getMaxEnergy = EnumFacing.values.map(globalBufferSize).max / Settings.get.ratioMekanism
//
//  @Optional.Method(modid = Mods.IDs.Mekanism)
//  def getEnergy = EnumFacing.values.map(globalBuffer).max / Settings.get.ratioMekanism
//
//  @Optional.Method(modid = Mods.IDs.Mekanism)
//  def setEnergy(energy: Double) {}
//}
