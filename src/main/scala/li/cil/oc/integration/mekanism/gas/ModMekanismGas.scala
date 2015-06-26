package li.cil.oc.integration.mekanism.gas

import li.cil.oc.api.Driver
import li.cil.oc.integration.Mod
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModMekanismGas extends ModProxy {
  override def getMod: Mod = Mods.MekanismGas

  override def initialize(): Unit = {
    Driver.add(ConverterGasStack)
  }
}
