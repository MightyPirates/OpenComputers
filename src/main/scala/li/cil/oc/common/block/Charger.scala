package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import li.cil.oc.server.PacketSender
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class Charger extends RedstoneAware with traits.PowerAcceptor with traits.Rotatable {
  override protected def setDefaultExtendedState(state: IBlockState) = setDefaultState(state)

  override def energyThroughput = Settings.get.chargerRate

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Charger()

  // ----------------------------------------------------------------------- //

  override def canConnectRedstone(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = true

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) =
    world.getTileEntity(pos) match {
      case charger: tileentity.Charger =>
        if (Wrench.holdsApplicableWrench(player, pos)) {
          if (!world.isRemote) {
            charger.invertSignal = !charger.invertSignal
            charger.chargeSpeed = 1.0 - charger.chargeSpeed
            PacketSender.sendChargerState(charger)
            Wrench.wrenchUsed(player, pos)
          }
          true
        }
        else if (!player.isSneaking) {
          if (!world.isRemote) {
            player.openGui(OpenComputers, GuiType.Charger.id, world, pos.getX, pos.getY, pos.getZ)
          }
          true
        }
        else false
      case _ => super.localOnBlockActivated(world, pos, player, side, hitX, hitY, hitZ)
    }

  override def onNeighborBlockChange(world: World, pos: BlockPos, state: IBlockState, neighborBlock: Block) {
    world.getTileEntity(pos) match {
      case charger: tileentity.Charger => charger.onNeighborChanged()
      case _ =>
    }
    super.onNeighborBlockChange(world, pos, state, neighborBlock)
  }
}
