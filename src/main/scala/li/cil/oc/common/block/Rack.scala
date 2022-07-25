package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.api.component.RackMountable
import li.cil.oc.common.GuiType
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.state.StateContainer
import net.minecraft.util.Direction
import net.minecraft.util.Direction.Axis
import net.minecraft.util.Hand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

class Rack extends RedstoneAware with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Facing)

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.serverRackRate

  override def guiType = GuiType.Rack

  override def newBlockEntity(world: IBlockReader) = new tileentity.Rack()

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    world.getBlockEntity(pos) match {
      case rack: tileentity.Rack => rack.slotAt(side, hitX, hitY, hitZ) match {
        case Some(slot) =>
          // Snap to grid to get same behavior on client and server...
          val hitVec = new Vector3d((hitX * 16f).toInt / 16f, (hitY * 16f).toInt / 16f, (hitZ * 16f).toInt / 16f)
          val rotation = side match {
            case Direction.WEST => Math.toRadians(90).toFloat
            case Direction.NORTH => Math.toRadians(180).toFloat
            case Direction.EAST => Math.toRadians(270).toFloat
            case _ => 0
          }
          // Rotate *centers* of pixels to keep association when reversing axis.
          val localHitVec = rotate(hitVec.add(-0.5 + 1 / 32f, -0.5 + 1 / 32f, -0.5 + 1 / 32f), rotation).add(0.5 - 1 / 32f, 0.5 - 1 / 32f, 0.5 - 1 / 32f)
          val globalX = (localHitVec.x * 16.05f).toInt // [0, 15], work around floating point inaccuracies
          val globalY = (localHitVec.y * 16.05f).toInt // [0, 15], work around floating point inaccuracies
          val localX = (if (side.getAxis != Axis.Z) 15 - globalX else globalX) - 1
          val localY = (15 - globalY) - 2 - 3 * slot
          if (localX >= 0 && localX < 14 && localY >= 0 && localY < 3) rack.getMountable(slot) match {
            case mountable: RackMountable if mountable.onActivate(player, hand, heldItem, localX / 14f, localY / 3f) => return true // Activation handled by mountable.
            case _ =>
          }
        case _ =>
      }
      case _ =>
    }
    super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)
  }

  def rotate(v: Vector3d, t: Float): Vector3d = {
    val cos = Math.cos(t)
    val sin = Math.sin(t)
    new Vector3d(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos)
  }
}
