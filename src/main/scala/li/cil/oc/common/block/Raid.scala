package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class Raid extends SimpleBlock {
  override protected def customTextures = Array(
    None,
    None,
    Some("RaidSide"),
    Some("RaidFront"),
    Some("RaidSide"),
    Some("RaidSide")
  )

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Raid()

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    world.getTileEntity(x, y, z) match {
      case drive: tileentity.Raid if !player.isSneaking =>
        if (!world.isRemote) {
          player.openGui(OpenComputers, GuiType.Raid.id, world, x, y, z)
        }
        true
      case _ => false
    }
  }
}
