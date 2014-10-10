package li.cil.oc.integration.fmp

import java.util

import codechicken.lib.vec.BlockCoord
import codechicken.multipart.MultiPartRegistry.IPartConverter
import li.cil.oc.api.Items
import li.cil.oc.common.tileentity.Cable
import net.minecraft.world.World

object MultipartConverter extends IPartConverter {
  override def blockTypes = util.Arrays.asList(Items.get("cable").block)

  override def convert(world: World, pos: BlockCoord) = {
    world.getTileEntity(pos.x, pos.y, pos.z) match {
      case cable: Cable => new CablePart(Some(cable))
      case _ => null
    }
  }
}
