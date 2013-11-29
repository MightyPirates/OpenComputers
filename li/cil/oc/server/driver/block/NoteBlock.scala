package li.cil.oc.server.driver.block

import li.cil.oc.api.driver
import li.cil.oc.server.component
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntityNote
import net.minecraft.world.World

object NoteBlock extends driver.Block {
  def worksWith(world: World, x: Int, y: Int, z: Int) =
    world.getBlockId(x, y, z) == Block.music.blockID

  def createEnvironment(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case block: TileEntityNote => new component.NoteBlock(block)
      case _ => null
    }
}
