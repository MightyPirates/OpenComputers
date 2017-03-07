package li.cil.oc.common.block;

import li.cil.oc.CreativeTab;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.tileentity.Colored;
import li.cil.oc.api.tileentity.RedstoneAware;
import li.cil.oc.api.tileentity.Rotatable;
import li.cil.oc.common.capabilities.CapabilityColored;
import li.cil.oc.common.capabilities.CapabilityRedstoneAware;
import li.cil.oc.common.capabilities.CapabilityRotatable;
import li.cil.oc.common.tileentity.traits.*;
import li.cil.oc.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.List;

public abstract class AbstractBlock extends Block {
    private static final EnumFacing[] VALID_ROTATIONS = new EnumFacing[0];
    private final boolean tileEntityHasComparatorOverride = getTileEntityClass() != null && BlockComparatorOverride.class.isAssignableFrom(getTileEntityClass());
    private final boolean tileEntityIsActivationListener = getTileEntityClass() != null && BlockActivationListener.class.isAssignableFrom(getTileEntityClass());
    private final boolean tileEntityIsBlockChangeListener = getTileEntityClass() != null && BlockChangeListener.class.isAssignableFrom(getTileEntityClass());
    private final boolean tileEntityIsItemStackSerializable = getTileEntityClass() != null && ItemStackSerializable.class.isAssignableFrom(getTileEntityClass());
    private final boolean tileEntityIsRedstoneAware = getTileEntityClass() != null && RedstoneAwareImpl.RedstoneAwareHost.class.isAssignableFrom(getTileEntityClass());

    // ----------------------------------------------------------------------- //

    protected AbstractBlock(final Material material) {
        super(material);
        setHardness(2f);
        setResistance(5);
        setCreativeTab(CreativeTab.INSTANCE);
    }

    protected AbstractBlock() {
        this(Material.IRON);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean canHarvestBlock(final IBlockAccess world, final BlockPos pos, final EntityPlayer player) {
        return true;
    }

    @Override
    public boolean hasTileEntity(final IBlockState state) {
        return getTileEntityClass() != null;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final World world, final IBlockState state) {
        final Class<? extends TileEntity> tileEntityClass = getTileEntityClass();
        if (tileEntityClass != null) {
            try {
                return tileEntityClass.newInstance();
            } catch (final InstantiationException | IllegalAccessException e) {
                OpenComputers.log().warn(String.format("Failed instantiating TileEntity of type '%s'.", tileEntityClass), e);
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final IBlockState state, final World world, final BlockPos pos, final Block blockIn, final BlockPos neighborPos) {
        super.neighborChanged(state, world, pos, blockIn, neighborPos);

        if (tileEntityIsBlockChangeListener) {
            final TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof BlockChangeListener) {
                final BlockChangeListener listener = (BlockChangeListener) tileEntity;
                listener.onBlockChanged(neighborPos);
            }
        }

        if (tileEntityIsRedstoneAware) {
            final RedstoneAware redstoneAware = CapabilityUtils.getCapability(world, pos, CapabilityRedstoneAware.REDSTONE_AWARE_CAPABILITY, BlockPosUtils.getNeighborSide(pos, neighborPos));
            if (redstoneAware != null) {
                redstoneAware.scheduleInputUpdate();
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasComparatorInputOverride(final IBlockState state) {
        return tileEntityHasComparatorOverride;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getComparatorInputOverride(final IBlockState state, final World world, final BlockPos pos) {
        if (tileEntityHasComparatorOverride) {
            final TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof BlockComparatorOverride) {
                final BlockComparatorOverride comparatorOverride = (BlockComparatorOverride) tileEntity;
                return comparatorOverride.getComparatorValue();
            }
        }
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canProvidePower(final IBlockState state) {
        return tileEntityIsRedstoneAware;
    }

    @Override
    public boolean canConnectRedstone(final IBlockState state, final IBlockAccess world, final BlockPos pos, @Nullable final EnumFacing side) {
        if (!tileEntityIsRedstoneAware) {
            return false;
        }

        final RedstoneAware redstoneAware = CapabilityUtils.getCapability(world, pos, CapabilityRedstoneAware.REDSTONE_AWARE_CAPABILITY, side);
        return redstoneAware != null && redstoneAware.isOutputEnabled();
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getStrongPower(final IBlockState state, final IBlockAccess world, final BlockPos pos, final EnumFacing side) {
        if (!tileEntityIsRedstoneAware) {
            return 0;
        }

        final RedstoneAware redstoneAware = CapabilityUtils.getCapability(world, pos, CapabilityRedstoneAware.REDSTONE_AWARE_CAPABILITY, side);
        if (redstoneAware != null && redstoneAware.isOutputEnabled() && redstoneAware.isOutputStrong()) {
            return redstoneAware.getOutput(side);
        }

        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getWeakPower(final IBlockState state, final IBlockAccess world, final BlockPos pos, final EnumFacing side) {
        if (!tileEntityIsRedstoneAware) {
            return 0;
        }

        final RedstoneAware redstoneAware = CapabilityUtils.getCapability(world, pos, CapabilityRedstoneAware.REDSTONE_AWARE_CAPABILITY, side);
        if (redstoneAware != null && redstoneAware.isOutputEnabled()) {
            return redstoneAware.getOutput(side);
        }

        return 0;
    }

    @Override
    public boolean onBlockActivated(final World world, final BlockPos pos, final IBlockState state, final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (tileEntityIsActivationListener) {
            final TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof BlockActivationListener) {
                final BlockActivationListener listener = (BlockActivationListener) tileEntity;
                return listener.onActivated(player, hand, side, hitX, hitY, hitZ);
            }
        }
        return recolorBlock(world, pos, side, player.getHeldItem(hand)) || super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }

    @Override
    public void breakBlock(final World world, final BlockPos pos, final IBlockState state) {
        if (!world.isRemote) {
            final TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity != null) {
                final IItemHandler itemHandler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                if (itemHandler != null) {
                    InventoryUtils.dropAllSlots(world, pos, itemHandler);
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Nullable
    @Override
    public EnumFacing[] getValidRotations(final World world, final BlockPos pos) {
        final Rotatable rotatable = CapabilityUtils.getCapability(world, pos, CapabilityRotatable.ROTATABLE_CAPABILITY, null);
        if (rotatable != null) {
            return rotatable.getValidRotations();
        }
        return VALID_ROTATIONS;
    }

    @Override
    public boolean rotateBlock(final World world, final BlockPos pos, final EnumFacing axis) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity != null) {
            final Rotatable rotatable = tileEntity.getCapability(CapabilityRotatable.ROTATABLE_CAPABILITY, null);
            if (rotatable != null && rotatable.rotate(axis.getAxis())) {
                final IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, WorldUtils.FLAG_REGULAR_UPDATE);
            }
        }
        return false;
    }

    @Override
    public boolean recolorBlock(final World world, final BlockPos pos, final EnumFacing side, final EnumDyeColor color) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity != null) {
            final Colored colored = tileEntity.getCapability(CapabilityColored.COLORED_CAPABILITY, side);
            if (colored != null && colored.setColor(DyeUtils.rgbFromDye(color))) {
                final IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, WorldUtils.FLAG_REGULAR_UPDATE);
                return true;
            }
        }
        return false;
    }

    public boolean recolorBlock(final World world, final BlockPos pos, final EnumFacing side, final ItemStack stack) {
        if (DyeUtils.isDye(stack)) {
            final EnumDyeColor color = DyeUtils.findDye(stack);
            return recolorBlock(world, pos, side, color);
        }
        return false;
    }

    @Override
    public ItemStack getPickBlock(final IBlockState state, final RayTraceResult target, final World world, final BlockPos pos, final EntityPlayer player) {
        if (tileEntityIsItemStackSerializable) {
            final TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof ItemStackSerializable) {
                final ItemStackSerializable serializable = (ItemStackSerializable) tileEntity;
                return serializable.writeItemStack();
            }
        }
        return super.getPickBlock(state, target, world, pos, player);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, final EntityPlayer player, final List<String> tooltip, final boolean advanced) {
        tooltipHead(stack, player, tooltip, advanced);
        tooltipBody(stack, player, tooltip, advanced);
        tooltipTail(stack, player, tooltip, advanced);
    }

    // ----------------------------------------------------------------------- //

    @Nullable
    protected Class<? extends TileEntity> getTileEntityClass() {
        return null;
    }

    protected void tooltipHead(final ItemStack stack, final EntityPlayer player, final List<String> tooltip, final boolean advanced) {
    }

    protected void tooltipBody(final ItemStack stack, final EntityPlayer player, final List<String> tooltip, final boolean advanced) {
        tooltip.addAll(Tooltip.get(getClass().getSimpleName(), null));
    }

    protected void tooltipTail(final ItemStack stack, final EntityPlayer player, final List<String> tooltip, final boolean advanced) {
    }
}
