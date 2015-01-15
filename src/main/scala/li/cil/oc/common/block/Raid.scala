package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.world.World

class Raid extends SimpleBlock with traits.GUI {
  override protected def customTextures = Array(
    None,
    None,
    Some("RaidSide"),
    Some("RaidFront"),
    Some("RaidSide"),
    Some("RaidSide")
  )

  // ----------------------------------------------------------------------- //

  override def guiType = GuiType.Raid

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Raid()

  // ----------------------------------------------------------------------- //

  override def hasComparatorInputOverride = true

  override def getComparatorInputOverride(world: World, x: Int, y: Int, z: Int, side: Int) =
    world.getTileEntity(x, y, z) match {
      case raid: tileentity.Raid if raid.presence.forall(ok => ok) => 15
      case _ => 0
    }
}
