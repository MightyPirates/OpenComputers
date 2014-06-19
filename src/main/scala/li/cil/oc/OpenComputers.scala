package li.cil.oc

import java.util.logging.Logger

import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.{Mod, SidedProxy}
import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkMod
import cpw.mods.fml.common.network.NetworkMod._
import li.cil.oc.client.{PacketHandler => ClientPacketHandler}
import li.cil.oc.common.Proxy
import li.cil.oc.server.{CommandHandler, PacketHandler => ServerPacketHandler}

@Mod(modid = OpenComputers.ID, name = OpenComputers.Name,
  version = OpenComputers.Version, /* certificateFingerprint = OpenComputers.Fingerprint, */
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

  final val Fingerprint = "@FINGERPRINT@"

  val log = Logger.getLogger("OpenComputers")

  @SidedProxy(clientSide = "li.cil.oc.client.Proxy", serverSide = "li.cil.oc.server.Proxy")
  var proxy: Proxy = null

  var tampered: Option[FMLFingerprintViolationEvent] = None

  //  @EventHandler
  //  def invalidFingerprint(e: FMLFingerprintViolationEvent) = tampered = Some(e)

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) = proxy.preInit(e)

  @EventHandler
  def init(e: FMLInitializationEvent) = proxy.init(e)

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) = proxy.postInit(e)

  @EventHandler
  def serverStart(e: FMLServerStartingEvent) = CommandHandler.register(e)
}