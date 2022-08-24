package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.container.ContainerTypes
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

class Relay extends SimpleBlock with traits.GUI with traits.PowerAcceptor {
  override def openGui(player: ServerPlayerEntity, world: World, pos: BlockPos): Unit = world.getBlockEntity(pos) match {
    case te: tileentity.Relay => ContainerTypes.openRelayGui(player, te)
    case _ =>
  }

  override def energyThroughput = Settings.get.accessPointRate

  override def newBlockEntity(world: IBlockReader) = new tileentity.Relay(tileentity.TileEntityTypes.RELAY)
}
