package li.cil.oc.common.multipart

import codechicken.multipart.MultiPartRegistry.{IPartConverter, IPartFactory}
import codechicken.multipart.{MultiPartRegistry, TMultiPart}
import net.minecraft.world.World
import codechicken.lib.vec.BlockCoord
import li.cil.oc.Blocks

class Content extends IPartFactory with IPartConverter {

  override def createPart(name: String, client: Boolean): TMultiPart = {
    if (name.equals("oc:cable")) return new CablePart
    null
  }

  def init() {
    MultiPartRegistry.registerConverter(this)
    MultiPartRegistry.registerParts(this, Array(
      "oc:cable"
    ))
  }

  override def canConvert(blockID: Int): Boolean = {
    blockID == Blocks.cable.createItemStack().itemID

  }

  override def convert(world: World, pos: BlockCoord): TMultiPart = {

    val id = world.getBlockId(pos.x, pos.y, pos.z)
    val meta = world.getBlockMetadata(pos.x, pos.y, pos.z)
    val cable = Blocks.cable.createItemStack()
    if (cable.itemID == id && cable.getItemDamage == meta)
      return new CablePart()
    null
  }
}
