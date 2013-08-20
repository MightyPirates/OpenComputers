package li.cil.oc

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.network.NetworkMod
import li.cil.oc.common.CommonProxy

@Mod(modid = "OpenComputers", name = "OpenComputers", version = "0.0.0", dependencies = "required-after:Forge@[9.10.0.804,)", modLanguage = "scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
object OpenComputers {
  @SidedProxy(
    clientSide = "li.cil.oc.client.ClientProxy",
    serverSide = "li.cil.oc.common.CommonProxy")
  var proxy: CommonProxy = null

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) = proxy.preInit(e)

  @EventHandler
  def init(e: FMLInitializationEvent) = proxy.init(e)

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) = proxy.postInit(e)
}