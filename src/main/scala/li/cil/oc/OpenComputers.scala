package li.cil.oc

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent
import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.FMLEventChannel
import li.cil.oc.common.IMC
import li.cil.oc.common.Proxy
import li.cil.oc.server.CommandHandler
import org.apache.logging.log4j.LogManager

@Mod(modid = OpenComputers.ID, name = OpenComputers.Name,
  version = OpenComputers.Version,
  modLanguage = "scala", useMetadata = true)
object OpenComputers {
  final val ID = "OpenComputers"

  final val Name = "OpenComputers"

  final val Version = "@VERSION@"

  var log = LogManager.getLogger("OpenComputers")

  @SidedProxy(clientSide = "li.cil.oc.client.Proxy", serverSide = "li.cil.oc.server.Proxy")
  var proxy: Proxy = null

  var channel: FMLEventChannel = _

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) {
    log = e.getModLog
    proxy.preInit(e)
  }

  @EventHandler
  def init(e: FMLInitializationEvent) = proxy.init(e)

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) = proxy.postInit(e)

  @EventHandler
  def missingMappings(e: FMLMissingMappingsEvent) = proxy.missingMappings(e)

  @EventHandler
  def serverStart(e: FMLServerStartingEvent) = CommandHandler.register(e)

  @EventHandler
  def imc(e: IMCEvent) = IMC.handleEvent(e)
}
