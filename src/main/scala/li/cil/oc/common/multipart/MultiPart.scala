package li.cil.oc.common.multipart

/* TODO FMP
import codechicken.lib.vec.BlockCoord
import codechicken.multipart.MultiPartRegistry.{IPartConverter, IPartFactory}
import codechicken.multipart.{TMultiPart, MultiPartRegistry}
import li.cil.oc.Blocks
import li.cil.oc.common.tileentity.Cable
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge

object MultiPart extends IPartFactory with IPartConverter {
  def init() {
    MultiPartRegistry.registerConverter(this)
    MultiPartRegistry.registerParts(this, Array("oc:cable"))
    MinecraftForge.EVENT_BUS.register(EventHandler)
  }

  override def createPart(name: String, client: Boolean): TMultiPart = {
    if (name.equals("oc:cable"))
      return new CablePart()
    null
  }

  override def canConvert(blockID: Int): Boolean = {
    blockID == Blocks.cable.parent.blockID
  }

  override def convert(world: World, pos: BlockCoord) = {
    world.getBlockTileEntity(pos.x, pos.y, pos.z) match {
      case cable: Cable => new CablePart(Some(cable.node))
      case _ => null
    }
  }
}
*/