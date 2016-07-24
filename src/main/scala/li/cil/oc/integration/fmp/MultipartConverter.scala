package li.cil.oc.integration.fmp

import java.util

import codechicken.lib.vec.BlockCoord
import li.cil.oc.Constants
import li.cil.oc.api.Items
import li.cil.oc.common.tileentity.Cable
import li.cil.oc.common.tileentity.Print
import net.minecraft.world.World

object MultipartConverter extends IPartConverter {
  def init() {
    MultiPartRegistry.registerConverter(this)
  }

  override def blockTypes = util.Arrays.asList(
    Items.get(Constants.BlockName.Cable).block,
    Items.get(Constants.BlockName.Print).block
  )

  override def convert(world: World, pos: BlockCoord) = {
    world.getTileEntity(pos.x, pos.y, pos.z) match {
      case cable: Cable => new CablePart(Some(cable))
      case print: Print => new PrintPart(Some(print))
      case _ => null
    }
  }
}
