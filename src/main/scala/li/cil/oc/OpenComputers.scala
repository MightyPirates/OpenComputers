package li.cil.oc

import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.FMLEventChannel
import cpw.mods.fml.common.{Mod, SidedProxy}
import li.cil.oc.common.Proxy
import li.cil.oc.server.CommandHandler
import org.apache.logging.log4j.LogManager

@Mod(modid = OpenComputers.ID, name = OpenComputers.Name,
  version = OpenComputers.Version, /* certificateFingerprint = OpenComputers.Fingerprint, */
  modLanguage = "scala", useMetadata = true)
object OpenComputers {
  final val ID = "OpenComputers"

  final val Name = "OpenComputers"

  final val Version = "@VERSION@"

  final val Fingerprint = "@FINGERPRINT@"

  var log = LogManager.getLogger("OpenComputers")

  @SidedProxy(clientSide = "li.cil.oc.client.Proxy", serverSide = "li.cil.oc.server.Proxy")
  var proxy: Proxy = null

  var channel: FMLEventChannel = _

  var tampered: Option[FMLFingerprintViolationEvent] = None

//  @EventHandler
//  def invalidFingerprint(e: FMLFingerprintViolationEvent) = tampered = Some(e)

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) {
    proxy.preInit(e)
    log = e.getModLog
  }

  @EventHandler
  def init(e: FMLInitializationEvent) = proxy.init(e)

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) = proxy.postInit(e)

  @EventHandler
  def missingMappings(e: FMLMissingMappingsEvent) = proxy.missingMappings(e)

  @EventHandler
  def serverStart(e: FMLServerStartingEvent) = CommandHandler.register(e)
}