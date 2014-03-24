package li.cil.oc.server

import codechicken.multipart.TMultiPart
import cpw.mods.fml.common.{FMLCommonHandler, Optional, TickType, ITickHandler}
import ic2.api.energy.event.{EnergyTileUnloadEvent, EnergyTileLoadEvent}
import java.util
import li.cil.oc.api.Network
import li.cil.oc.common.tileentity.traits.power
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import scala.collection.mutable
import li.cil.oc.OpenComputers
import java.util.logging.Level

object TickHandler extends ITickHandler {
  val pending = mutable.Buffer.empty[() => Unit]

  def schedule(tileEntity: TileEntity) =
    if (FMLCommonHandler.instance.getEffectiveSide.isServer) pending.synchronized {
      pending += (() => Network.joinOrCreateNetwork(tileEntity))
    }

  @Optional.Method(modid = "ForgeMultipart")
  def schedule(part: TMultiPart) =
    if (FMLCommonHandler.instance.getEffectiveSide.isServer) pending.synchronized {
      pending += (() => Network.joinOrCreateNetwork(part.tile))
    }

  @Optional.Method(modid = "IC2")
  def scheduleIC2Add(tileEntity: power.IndustrialCraft2) = pending.synchronized {
    pending += (() => if (!tileEntity.addedToPowerGrid && !tileEntity.isInvalid) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(tileEntity))
      tileEntity.addedToPowerGrid = true
    })
  }

  @Optional.Method(modid = "IC2")
  def scheduleIC2Remove(tileEntity: power.IndustrialCraft2) = pending.synchronized {
    pending += (() => if (tileEntity.addedToPowerGrid) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(tileEntity))
      tileEntity.addedToPowerGrid = false
    })
  }

  override def getLabel = "OpenComputers Network Initialization Ticker"

  override def ticks() = util.EnumSet.of(TickType.SERVER)

  override def tickStart(`type`: util.EnumSet[TickType], tickData: AnyRef*) {}

  override def tickEnd(`type`: util.EnumSet[TickType], tickData: AnyRef*) = pending.synchronized {
    for (callback <- pending) {
      try callback() catch {
        case t: Throwable => OpenComputers.log.log(Level.WARNING, "Error in scheduled tick action.", t)
      }
    }
    pending.clear()
  }
}
