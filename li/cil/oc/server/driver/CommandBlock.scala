package li.cil.oc.server.driver

import li.cil.oc.Config
import li.cil.oc.api.driver
import li.cil.oc.server.component
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntityCommandBlock
import net.minecraft.world.World

object CommandBlock extends driver.Block {
  override def api = getClass.getResourceAsStream(Config.driverPath + "command_block.lua")

  def worksWith(world: World, x: Int, y: Int, z: Int) =
    world.getBlockId(x, y, z) == Block.commandBlock.blockID

  override def node(world: World, x: Int, y: Int, z: Int) =
    new component.CommandBlock(world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityCommandBlock])
}
