package li.cil.oc

import java.util.logging.Logger

import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent
import cpw.mods.fml.common.{Mod, SidedProxy}
import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkMod
import cpw.mods.fml.common.network.NetworkMod._
import li.cil.oc.client.{PacketHandler => ClientPacketHandler}
import li.cil.oc.common.Proxy
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.server.{CommandHandler, PacketHandler => ServerPacketHandler}

import scala.collection.convert.WrapAsScala._

@Mod(modid = OpenComputers.ID, name = OpenComputers.Name,
  version = OpenComputers.Version,
  modLanguage = "scala", useMetadata = true)
@NetworkMod(clientSideRequired = true, serverSideRequired = false,
  clientPacketHandlerSpec = new SidedPacketHandler(
    channels = Array("OpenComp"), packetHandler = classOf[ClientPacketHandler]),
  serverPacketHandlerSpec = new SidedPacketHandler(
    channels = Array("OpenComp"), packetHandler = classOf[ServerPacketHandler]))
object OpenComputers {
  final val ID = "OpenComputers"

  final val Name = "OpenComputers"

  final val Version = "@VERSION@"

  var log = Logger.getLogger("OpenComputers")

  @SidedProxy(clientSide = "li.cil.oc.client.Proxy", serverSide = "li.cil.oc.server.Proxy")
  var proxy: Proxy = null

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
  def serverStart(e: FMLServerStartingEvent) = CommandHandler.register(e)

  @EventHandler
  def imc(e: IMCEvent) = {
    for (message <- e.getMessages) {
      if (message.key == "registerAssemblerTemplate" && message.isNBTMessage) {
        log.fine(s"Registering new assembler template from mod ${message.getSender}.")
        AssemblerTemplates.add(message.getNBTValue)
      }
    }
  }
}
