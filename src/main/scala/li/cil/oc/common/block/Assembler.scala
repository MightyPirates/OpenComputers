package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.BlockState
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

class Assembler(props: Properties) extends SimpleBlock(props) with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  override def energyThroughput = Settings.get.assemblerRate

  override def openGui(player: ServerPlayerEntity, world: World, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.Assembler => ContainerTypes.openAssemblerGui(player, te)
    case _ =>
  }

  override def newBlockEntity(world: IBlockReader) = new tileentity.Assembler(tileentity.TileEntityTypes.ASSEMBLER)
}
