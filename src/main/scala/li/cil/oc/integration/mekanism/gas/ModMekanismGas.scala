package li.cil.oc.integration.mekanism.gas

import li.cil.oc.api.Driver
import li.cil.oc.integration.{Mod, ModProxy, Mods}


object ModMekanismGas extends ModProxy{
   override def getMod: Mod = Mods.MekanismGas

   override def initialize(): Unit = {
     Driver.add(ConverterGasStack)
   }
 }
