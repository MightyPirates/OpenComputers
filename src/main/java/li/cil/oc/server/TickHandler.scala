package li.cil.oc.server

import codechicken.multipart.TMultiPart
import cpw.mods.fml.common.{Optional, TickType, ITickHandler}
import java.util
import li.cil.oc.api.Network
import net.minecraft.tileentity.TileEntity
import scala.collection.mutable

object TickHandler extends ITickHandler {
  val pendingAdds = mutable.Buffer.empty[() => Unit]

  def schedule(tileEntity: TileEntity) = pendingAdds.synchronized {
    pendingAdds += (() => Network.joinOrCreateNetwork(tileEntity))
  }

  @Optional.Method(modid = "ForgeMultipart")
  def schedule(part: TMultiPart) = pendingAdds.synchronized {
    pendingAdds += (() => Network.joinOrCreateNetwork(part.tile))
  }

  override def getLabel = "OpenComputers Network Initialization Ticker"

  override def ticks() = util.EnumSet.of(TickType.SERVER)

  override def tickStart(`type`: util.EnumSet[TickType], tickData: AnyRef*) {}

  override def tickEnd(`type`: util.EnumSet[TickType], tickData: AnyRef*) = pendingAdds.synchronized {
    for (callback <- pendingAdds) {
      callback()
    }
    pendingAdds.clear()
  }
}
