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

@Mod(modid = "OpenComputers", name = "OpenComputers", version = "1.1.0",
  dependencies = "required-after:Forge@[9.11.1.940,);after:BuildCraft|Energy;after:ComputerCraft;after:IC2;after:MineFactoryReloaded;after:ProjRed|Transmission;after:RedLogic;after:ThermalExpansion",
  modLanguage = "scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = false,
  clientPacketHandlerSpec = new SidedPacketHandler(
    channels = Array("OpenComp"), packetHandler = classOf[ClientPacketHandler]),
  serverPacketHandlerSpec = new SidedPacketHandler(
    channels = Array("OpenComp"), packetHandler = classOf[ServerPacketHandler]))
object OpenComputers {
  val log = Logger.getLogger("OpenComputers")

  @SidedProxy(clientSide = "li.cil.oc.client.Proxy", serverSide = "li.cil.oc.server.Proxy")
  var proxy: Proxy = null

  @EventHandler
  def preInit(e: FMLPreInitializationEvent) = proxy.preInit(e)

  @EventHandler
  def init(e: FMLInitializationEvent) = proxy.init(e)

  @EventHandler
  def postInit(e: FMLPostInitializationEvent) = proxy.postInit(e)
}