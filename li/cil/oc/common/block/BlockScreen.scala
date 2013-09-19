package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import li.cil.oc.Blocks

class BlockScreen extends SubBlock {
  val unlocalizedName = "Screen"

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new TileEntityScreen

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
    side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    player.openGui(OpenComputers, GuiType.Screen.id, world, x, y, z)
    true
  }
}