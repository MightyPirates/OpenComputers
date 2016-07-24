package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import li.cil.oc.server.PacketSender
import net.minecraft.block.Block
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class Charger extends RedstoneAware with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  override def createBlockState() = new BlockStateContainer(this, PropertyRotatable.Facing)

  override def getStateFromMeta(meta: Int): IBlockState = getDefaultState.withProperty(PropertyRotatable.Facing, EnumFacing.getHorizontal(meta))

  override def getMetaFromState(state: IBlockState): Int = state.getValue(PropertyRotatable.Facing).getHorizontalIndex

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.chargerRate

  override def guiType = GuiType.Charger

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Charger()

  // ----------------------------------------------------------------------- //

  override def canConnectRedstone(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing): Boolean = true

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) =
    if (Wrench.holdsApplicableWrench(player, pos)) world.getTileEntity(pos) match {
      case charger: tileentity.Charger =>
        if (!world.isRemote) {
          charger.invertSignal = !charger.invertSignal
          charger.chargeSpeed = 1.0 - charger.chargeSpeed
          PacketSender.sendChargerState(charger)
          Wrench.wrenchUsed(player, pos)
        }
        true
      case _ => false
    }
    else super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)

  override def neighborChanged(state: IBlockState, world: World, pos: BlockPos, neighborBlock: Block): Unit = {
    world.getTileEntity(pos) match {
      case charger: tileentity.Charger => charger.onNeighborChanged()
      case _ =>
    }
    super.neighborChanged(state, world, pos, neighborBlock)
  }
}
