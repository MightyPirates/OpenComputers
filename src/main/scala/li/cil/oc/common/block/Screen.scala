package li.cil.oc.common.block

import java.util

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.GuiType
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.block.property.PropertyTile
import li.cil.oc.common.tileentity
import li.cil.oc.integration.coloredlights.ModColoredLights
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.Color
import li.cil.oc.util.PackedColor
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Screen(val tier: Int) extends RedstoneAware {
  ModColoredLights.setLightLevel(this, 5, 5, 5)

  override def createBlockState(): BlockState = new ExtendedBlockState(this, Array(PropertyRotatable.Pitch, PropertyRotatable.Yaw), Array(PropertyTile.Tile))

  override def getMetaFromState(state: IBlockState): Int = (state.getValue(PropertyRotatable.Pitch).ordinal() << 2) | state.getValue(PropertyRotatable.Yaw).getHorizontalIndex

  override def getStateFromMeta(meta: Int): IBlockState = getDefaultState.withProperty(PropertyRotatable.Pitch, EnumFacing.getFront(meta >> 2)).withProperty(PropertyRotatable.Yaw, EnumFacing.getHorizontal(meta & 0x3))

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState =
    (state, world.getTileEntity(pos)) match {
      case (extendedState: IExtendedBlockState, tile: tileentity.Screen) =>
        extendedState.
          withProperty(property.PropertyTile.Tile, tile).
          withProperty(PropertyRotatable.Pitch, tile.pitch).
          withProperty(PropertyRotatable.Yaw, tile.yaw)
      case _ => state
    }

  override def isSideSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = toLocal(world, pos, side) != EnumFacing.SOUTH

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def getRenderColor(state: IBlockState) = Color.rgbValues(Color.byTier(tier))

  // ----------------------------------------------------------------------- //

  override def rarity(stack: ItemStack) = Rarity.byTier(tier)

  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    val (w, h) = Settings.screenResolutionsByTier(tier)
    val depth = PackedColor.Depth.bits(Settings.screenDepthsByTier(tier))
    tooltip.addAll(Tooltip.get(getClass.getSimpleName, w, h, depth))
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Screen(tier)

  // ----------------------------------------------------------------------- //

  override def onBlockPlacedBy(world: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack)
    world.getTileEntity(pos) match {
      case screen: tileentity.Screen => screen.delayUntilCheckForMultiBlock = 0
      case _ =>
    }
  }

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = rightClick(world, pos, player, side, hitX, hitY, hitZ, force = false)

  def rightClick(world: World, pos: BlockPos, player: EntityPlayer,
                 side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float, force: Boolean) = {
    if (Wrench.holdsApplicableWrench(player, pos) && getValidRotations(world, pos).contains(side) && !force) false
    else if (api.Items.get(player.getHeldItem) == api.Items.get(Constants.ItemName.Analyzer)) false
    else world.getTileEntity(pos) match {
      case screen: tileentity.Screen if screen.hasKeyboard && (force || player.isSneaking == screen.origin.invertTouchMode) =>
        // Yep, this GUI is actually purely client side. We could skip this
        // if, but it is clearer this way (to trigger it from the server we
        // would have to give screens a "container", which we do not want).
        if (world.isRemote) {
          player.openGui(OpenComputers, GuiType.Screen.id, world, pos.getX, pos.getY, pos.getZ)
        }
        true
      case screen: tileentity.Screen if screen.tier > 0 && side == screen.facing =>
        if (world.isRemote && player == Minecraft.getMinecraft.thePlayer) {
          screen.click(hitX, hitY, hitZ)
        }
        else true
      case _ => false
    }
  }

  override def onEntityCollidedWithBlock(world: World, pos: BlockPos, entity: Entity) =
    if (world.isRemote) (entity, world.getTileEntity(pos)) match {
      case (arrow: EntityArrow, screen: tileentity.Screen) if screen.tier > 0 =>
        val hitX = math.max(0, math.min(1, arrow.posX - pos.getX))
        val hitY = math.max(0, math.min(1, arrow.posY - pos.getY))
        val hitZ = math.max(0, math.min(1, arrow.posZ - pos.getZ))
        val absX = math.abs(hitX - 0.5)
        val absY = math.abs(hitY - 0.5)
        val absZ = math.abs(hitZ - 0.5)
        val side = if (absX > absY && absX > absZ) {
          if (hitX < 0.5) EnumFacing.WEST
          else EnumFacing.EAST
        }
        else if (absY > absZ) {
          if (hitY < 0.5) EnumFacing.DOWN
          else EnumFacing.UP
        }
        else {
          if (hitZ < 0.5) EnumFacing.NORTH
          else EnumFacing.SOUTH
        }
        if (side == screen.facing) {
          screen.shot(arrow)
        }
      case _ =>
    }
    else world.getTileEntity(pos) match {
      case screen: tileentity.Screen if screen.tier > 0 && screen.facing == EnumFacing.UP => screen.walk(entity)
      case _ =>
    }

  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, pos: BlockPos) =
    world.getTileEntity(pos) match {
      case screen: tileentity.Screen =>
        if (screen.facing == EnumFacing.UP || screen.facing == EnumFacing.DOWN) EnumFacing.values
        else EnumFacing.values.filter {
          d => d != screen.facing && d != screen.facing.getOpposite
        }
      case _ => super.getValidRotations(world, pos)
    }
}
