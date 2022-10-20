package li.cil.oc.common.block

import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.shapes.IBooleanFunction
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.util.math.shapes.VoxelShapes
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

class Printer(props: Properties) extends SimpleBlock(props) with traits.StateAware with traits.GUI {
  val blockShape = {
    val base = Block.box(0, 0, 0, 16, 8, 16)
    val pillars = VoxelShapes.or(Block.box(0, 8, 0, 3, 13, 3), Block.box(13, 8, 0, 16, 13, 3),
      Block.box(13, 8, 13, 16, 13, 16), Block.box(0, 8, 13, 3, 13, 16))
    val ring = VoxelShapes.join(Block.box(0, 13, 0, 16, 16, 16),
      Block.box(3, 13, 3, 13, 16, 13), IBooleanFunction.ONLY_FIRST)
    VoxelShapes.or(base, pillars, ring)
  }

  override def getShape(state: BlockState, world: IBlockReader, pos: BlockPos, ctx: ISelectionContext): VoxelShape = blockShape

  override def openGui(player: ServerPlayerEntity, world: World, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.Printer => ContainerTypes.openPrinterGui(player, te)
    case _ =>
  }

  override def newBlockEntity(world: IBlockReader) = new tileentity.Printer(tileentity.TileEntityTypes.PRINTER)
}
