package li.cil.oc

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent
import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.FMLEventChannel
import li.cil.oc.common.IMC
import li.cil.oc.common.Proxy
import li.cil.oc.server.command.CommandHandler
import li.cil.oc.util.ThreadPoolFactory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(modid = OpenComputers.ID, name = OpenComputers.Name,
  version = OpenComputers.Version,
  modLanguage = "scala", useMetadata = true /*@MCVERSIONDEP@*/)
object OpenComputers {
  final val ID = "OpenComputers"

  final val Name = "OpenComputers"

  final val Version = "@VERSION@"

  def log = logger.getOrElse(LogManager.getLogger(Name))

  var logger: Option[Logger] = None

  @SidedProxy(clientSide = "li.cil.oc.client.Proxy", serverSide = "li.cil.oc.server.Proxy")
  var proxy: Proxy = null

  var channel: FMLEventChannel = _

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) {
    logger = Option(e.getModLog)
    proxy.preInit(e)
    OpenComputers.log.info("Done with pre init phase.")
  }

  @EventHandler
  def init(e: FMLInitializationEvent) = {
    proxy.init(e)
    OpenComputers.log.info("Done with init phase.")
  }

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) = {
    proxy.postInit(e)
    OpenComputers.log.info("Done with post init phase.")
  }

  @EventHandler
  def missingMappings(e: FMLMissingMappingsEvent) = proxy.missingMappings(e)

  @EventHandler
  def serverStart(e: FMLServerStartingEvent): Unit = {
    CommandHandler.register(e)
    ThreadPoolFactory.safePools.foreach(_.newThreadPool())
  }

  @EventHandler
  def serverStop(e: FMLServerStoppedEvent): Unit = {
    ThreadPoolFactory.safePools.foreach(_.waitForCompletion())
  }

  @EventHandler
  def imc(e: IMCEvent) = IMC.handleEvent(e)
}
