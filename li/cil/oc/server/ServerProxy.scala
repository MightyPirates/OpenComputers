package li.cil.oc.server

import scala.collection.convert.WrapAsScala._

import cpw.mods.fml.common.event.FMLInitializationEvent
import li.cil.oc.common.CommonProxy
import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.ChunkEvent

class ServerProxy extends CommonProxy {
  override def init(e: FMLInitializationEvent) = {
    super.init(e)

    MinecraftForge.EVENT_BUS.register(ForgeEventHandler)
  }

  private object ForgeEventHandler {
    @ForgeSubscribe
    def onChunkUnloadEvent(e: ChunkEvent.Unload) = {
      mapAsScalaMap(e.getChunk.chunkTileEntityMap).
        values.filter(_.isInstanceOf[TileEntityComputer]).
        map(_.asInstanceOf[TileEntityComputer]).
        foreach(_.turnOff())
    }
  }
}