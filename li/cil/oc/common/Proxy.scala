package li.cil.oc.common

import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.LanguageRegistry
import li.cil.oc._
import li.cil.oc.api.{OpenComputersAPI, INetworkNode, NetworkAPI}
import li.cil.oc.common.tileentity.TileEntityComputer
import li.cil.oc.server.computer.Drivers
import li.cil.oc.server.drivers._
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.ChunkEvent
import scala.collection.JavaConversions._

class Proxy {
  def preInit(e: FMLPreInitializationEvent): Unit = {
    Config.load(e.getSuggestedConfigurationFile)

    LanguageRegistry.instance.loadLocalization(
      "/assets/opencomputers/lang/en_US.lang", "en_US", false)
  }

  def init(e: FMLInitializationEvent): Unit = {
    Blocks.init()
    Items.init()

    NetworkRegistry.instance.registerGuiHandler(OpenComputers, GuiHandler)

    OpenComputersAPI.addDriver(GraphicsCardDriver)

    MinecraftForge.EVENT_BUS.register(ForgeEventHandler)
  }

  def postInit(e: FMLPostInitializationEvent): Unit = {
    // Lock the driver registry to avoid drivers being added after computers
    // may have already started up. This makes sure the driver API won't change
    // over the course of a game, since that could lead to weird effects.
    Drivers.locked = true
  }

  private object ForgeEventHandler {
    @ForgeSubscribe
    def onChunkUnload(e: ChunkEvent.Unload) =
      onUnload(e.world, e.getChunk.chunkTileEntityMap.values.map(_.asInstanceOf[TileEntity]))

    @ForgeSubscribe
    def onChunkLoad(e: ChunkEvent.Load) =
      onLoad(e.world, e.getChunk.chunkTileEntityMap.values.map(_.asInstanceOf[TileEntity]))

    private def onUnload(w: World, tileEntities: Iterable[TileEntity]) =
      if (!w.isRemote) {
        // Shut down any computers.
        tileEntities.
          filter(_.isInstanceOf[TileEntityComputer]).
          map(_.asInstanceOf[TileEntityComputer]).
          foreach(_.turnOff())

        // Remove all network nodes from their networks.
        // TODO add a more efficient batch remove operation? something along the lines of if #remove > #nodes*factor remove all, re-add remaining?
        tileEntities.
          filter(_.isInstanceOf[INetworkNode]).
          map(_.asInstanceOf[TileEntity with INetworkNode]).
          foreach(t => t.network.remove(t))
      }

    private def onLoad(w: World, tileEntities: Iterable[TileEntity]) =
      if (!w.isRemote) {
        // Add all network nodes to networks.
        tileEntities.foreach(t => NetworkAPI.joinOrCreateNetwork(w, t.xCoord, t.yCoord, t.zCoord))
      }
  }

}