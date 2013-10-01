package li.cil.oc

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.network.NetworkMod
import cpw.mods.fml.common.network.NetworkMod._
import java.util.logging.Logger
import li.cil.oc.client.{PacketHandler => ClientPacketHandler}
import li.cil.oc.common.Proxy
import li.cil.oc.server.{PacketHandler => ServerPacketHandler}
import scala.reflect.runtime.{universe => ru}

@Mod(modid = "OpenComputers", name = "OpenComputers", version = "0.0.0", dependencies = "required-after:Forge@[9.10.0.804,)", modLanguage = "scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = false,
  clientPacketHandlerSpec = new SidedPacketHandler(
    channels = Array("OpenComp"), packetHandler = classOf[ClientPacketHandler]),
  serverPacketHandlerSpec = new SidedPacketHandler(
    channels = Array("OpenComp"), packetHandler = classOf[ServerPacketHandler]))
object OpenComputers {
  /** Logger used all throughout this mod. */
  val log = Logger.getLogger("OpenComputers")

  // Workaround for threading issues in Scala 2.10's runtime reflection: just
  // initialize it once in the beginning. For more on this issue see
  // http://docs.scala-lang.org/overviews/reflection/thread-safety.html
  val mirror = ru.runtimeMirror(OpenComputers.getClass.getClassLoader)

  @SidedProxy(
    clientSide = "li.cil.oc.client.Proxy",
    serverSide = "li.cil.oc.server.Proxy")
  var proxy: Proxy = null

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) = proxy.preInit(e)

  @EventHandler
  def init(e: FMLInitializationEvent) = proxy.init(e)

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) = proxy.postInit(e)
}