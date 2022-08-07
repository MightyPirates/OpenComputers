package li.cil.oc.common.block

import java.util

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.GuiType
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.PackedColor
import li.cil.oc.util.Rarity
import li.cil.oc.util.RotationHelper
import li.cil.oc.util.Tooltip
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.item.ItemStack
import net.minecraft.state.StateContainer
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.{AxisAlignedBB, BlockPos}
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.{IBlockReader, World}
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}

import scala.collection.convert.ImplicitConversionsToScala._

class Screen(val tier: Int) extends RedstoneAware {
  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]) =
    builder.add(PropertyRotatable.Pitch, PropertyRotatable.Yaw)

  // ----------------------------------------------------------------------- //

  override def rarity(stack: ItemStack) = Rarity.byTier(tier)

  override protected def tooltipBody(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) {
    val (w, h) = Settings.screenResolutionsByTier(tier)
    val depth = PackedColor.Depth.bits(Settings.screenDepthsByTier(tier))
    for (curr <- Tooltip.get(getClass.getSimpleName.toLowerCase, w, h, depth)) {
      tooltip.add(new StringTextComponent(curr).setStyle(Tooltip.DefaultStyle))
    }
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(world: IBlockReader) = new tileentity.Screen(tileentity.TileEntityTypes.SCREEN, tier)

  // ----------------------------------------------------------------------- //

  override def setPlacedBy(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity, stack: ItemStack) {
    super.setPlacedBy(world, pos, state, placer, stack)
    world.getBlockEntity(pos) match {
      case screen: tileentity.Screen => screen.delayUntilCheckForMultiBlock = 0
      case _ =>
    }
  }

  override def localOnBlockActivated(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = rightClick(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ, force = false)

  def rightClick(world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, heldItem: ItemStack,
                 side: Direction, hitX: Float, hitY: Float, hitZ: Float, force: Boolean) = {
    if (Wrench.holdsApplicableWrench(player, pos) && getValidRotations(world, pos).contains(side) && !force) false
    else if (api.Items.get(heldItem) == api.Items.get(Constants.ItemName.Analyzer)) false
    else world.getBlockEntity(pos) match {
      case screen: tileentity.Screen if screen.hasKeyboard && (force || player.isCrouching == screen.origin.invertTouchMode) =>
        // Yep, this GUI is actually purely client side. We could skip this
        // if, but it is clearer this way (to trigger it from the server we
        // would have to give screens a "container", which we do not want).
        if (world.isClientSide) {
          OpenComputers.openGui(player, GuiType.Screen.id, world, pos.getX, pos.getY, pos.getZ)
        }
        true
      case screen: tileentity.Screen if screen.tier > 0 && side == screen.facing =>
        if (world.isClientSide && player == Minecraft.getInstance.player) {
          screen.click(hitX, hitY, hitZ)
        }
        else true
      case _ => false
    }
  }

  override def stepOn(world: World, pos: BlockPos, entity: Entity): Unit =
    if (!world.isClientSide) world.getBlockEntity(pos) match {
      case screen: tileentity.Screen if screen.tier > 0 && screen.facing == Direction.UP => screen.walk(entity)
      case _ => super.stepOn(world, pos, entity)
    }

  override def entityInside(state: BlockState, world: World, pos: BlockPos, entity: Entity): Unit =
    if (world.isClientSide) (entity, world.getBlockEntity(pos)) match {
      case (arrow: ArrowEntity, screen: tileentity.Screen) if screen.tier > 0 =>
        val hitX = math.max(0, math.min(1, arrow.getX - pos.getX))
        val hitY = math.max(0, math.min(1, arrow.getY - pos.getY))
        val hitZ = math.max(0, math.min(1, arrow.getZ - pos.getZ))
        val absX = math.abs(hitX - 0.5)
        val absY = math.abs(hitY - 0.5)
        val absZ = math.abs(hitZ - 0.5)
        val side = if (absX > absY && absX > absZ) {
          if (hitX < 0.5) Direction.WEST
          else Direction.EAST
        }
        else if (absY > absZ) {
          if (hitY < 0.5) Direction.DOWN
          else Direction.UP
        }
        else {
          if (hitZ < 0.5) Direction.NORTH
          else Direction.SOUTH
        }
        if (side == screen.facing) {
          screen.shot(arrow)
        }
      case _ =>
    }

  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, pos: BlockPos) =
    world.getBlockEntity(pos) match {
      case screen: tileentity.Screen =>
        if (screen.facing == Direction.UP || screen.facing == Direction.DOWN) Direction.values
        else Direction.values.filter {
          d => d != screen.facing && d != screen.facing.getOpposite
        }
      case _ => super.getValidRotations(world, pos)
    }
}
