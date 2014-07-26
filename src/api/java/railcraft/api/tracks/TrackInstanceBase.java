package mods.railcraft.api.tracks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import mods.railcraft.api.core.items.IToolCrowbar;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailBase.Rail;
import net.minecraft.entity.EntityLivingBase;

/**
 * All ITrackInstances should extend this class. It contains a number of default
 * functions and standard behavior for Tracks that should greatly simplify
 * implementing new Tracks when using this API.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 * @see ITrackInstance
 * @see TrackRegistry
 * @see TrackSpec
 */
public abstract class TrackInstanceBase implements ITrackInstance {

    private Block block;
    public TileEntity tileEntity;

    private Block getBlock() {
        if (block == null)
            block = getWorld().getBlock(getX(), getY(), getZ());
        return block;
    }

    @Override
    public void setTile(TileEntity tile) {
        tileEntity = tile;
    }

    @Override
    public TileEntity getTile() {
        return tileEntity;
    }

    @Override
    public int getBasicRailMetadata(EntityMinecart cart) {
        return tileEntity.getBlockMetadata();
    }

    @Override
    public void onMinecartPass(EntityMinecart cart) {
    }

    @Override
    public boolean blockActivated(EntityPlayer player) {
        if (this instanceof ITrackReversable) {
            ItemStack current = player.getCurrentEquippedItem();
            if (current != null && current.getItem() instanceof IToolCrowbar) {
                IToolCrowbar crowbar = (IToolCrowbar) current.getItem();
                if (crowbar.canWhack(player, current, getX(), getY(), getZ())) {
                    ITrackReversable track = (ITrackReversable) this;
                    track.setReversed(!track.isReversed());
                    markBlockNeedsUpdate();
                    crowbar.onWhack(player, current, getX(), getY(), getZ());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onBlockPlaced() {
        switchTrack(true);
        testPower();
        markBlockNeedsUpdate();
    }

    @Override
    public void onBlockPlacedBy(EntityLivingBase entityliving) {
        if (entityliving == null)
            return;
        if (this instanceof ITrackReversable) {
            int dir = MathHelper.floor_double((double) ((entityliving.rotationYaw * 4F) / 360F) + 0.5D) & 3;
            ((ITrackReversable) this).setReversed(dir == 0 || dir == 1);
        }
        markBlockNeedsUpdate();
    }

    @Override
    public void onBlockRemoved() {
    }

    public void sendUpdateToClient() {
        ((ITrackTile) tileEntity).sendUpdateToClient();
    }

    public void markBlockNeedsUpdate() {
        getWorld().markBlockForUpdate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
    }

    protected boolean isRailValid(World world, int x, int y, int z, int meta) {
        boolean valid = true;
        if (!world.isSideSolid(x, y - 1, z, ForgeDirection.UP))
            valid = false;
        if (meta == 2 && !world.isSideSolid(x + 1, y, z, ForgeDirection.UP))
            valid = false;
        else if (meta == 3 && !world.isSideSolid(x - 1, y, z, ForgeDirection.UP))
            valid = false;
        else if (meta == 4 && !world.isSideSolid(x, y, z - 1, ForgeDirection.UP))
            valid = false;
        else if (meta == 5 && !world.isSideSolid(x, y, z + 1, ForgeDirection.UP))
            valid = false;
        return valid;
    }

    @Override
    public void onNeighborBlockChange(Block blockChanged) {
        int meta = tileEntity.getBlockMetadata();
        boolean valid = isRailValid(getWorld(), tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, meta);
        if (!valid) {
            Block blockTrack = getBlock();
            blockTrack.dropBlockAsItem(getWorld(), tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, 0, 0);
            getWorld().setBlockToAir(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
            return;
        }

        if (blockChanged != null && blockChanged.canProvidePower()
                && isFlexibleRail() && RailTools.countAdjecentTracks(getWorld(), tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord) == 3)
            switchTrack(false);
        testPower();
    }

    protected void switchTrack(boolean flag) {
        int x = tileEntity.xCoord;
        int y = tileEntity.yCoord;
        int z = tileEntity.zCoord;
        BlockRailBase blockTrack = (BlockRailBase) getBlock();
        blockTrack.new Rail(getWorld(), x, y, z).func_150655_a(getWorld().isBlockIndirectlyGettingPowered(x, y, z), flag);
    }

    protected void testPower() {
        if (!(this instanceof ITrackPowered))
            return;
        int i = tileEntity.xCoord;
        int j = tileEntity.yCoord;
        int k = tileEntity.zCoord;
        ITrackPowered r = (ITrackPowered) this;
        int meta = tileEntity.getBlockMetadata();
        boolean powered = getWorld().isBlockIndirectlyGettingPowered(i, j, k) || testPowerPropagation(getWorld(), i, j, k, getTrackSpec(), meta, r.getPowerPropagation());
        if (powered != r.isPowered()) {
            r.setPowered(powered);
            Block blockTrack = getBlock();
            getWorld().notifyBlocksOfNeighborChange(i, j, k, blockTrack);
            getWorld().notifyBlocksOfNeighborChange(i, j - 1, k, blockTrack);
            if (meta == 2 || meta == 3 || meta == 4 || meta == 5)
                getWorld().notifyBlocksOfNeighborChange(i, j + 1, k, blockTrack);
            sendUpdateToClient();
            // System.out.println("Setting power [" + i + ", " + j + ", " + k + "]");
        }
    }

    protected boolean testPowerPropagation(World world, int i, int j, int k, TrackSpec spec, int meta, int maxDist) {
        return isConnectedRailPowered(world, i, j, k, spec, meta, true, 0, maxDist) || isConnectedRailPowered(world, i, j, k, spec, meta, false, 0, maxDist);
    }

    protected boolean isConnectedRailPowered(World world, int i, int j, int k, TrackSpec spec, int meta, boolean dir, int dist, int maxDist) {
        if (dist >= maxDist)
            return false;
        boolean powered = true;
        switch (meta) {
            case 0: // '\0'
                if (dir)
                    k++;
                else
                    k--;
                break;

            case 1: // '\001'
                if (dir)
                    i--;
                else
                    i++;
                break;

            case 2: // '\002'
                if (dir)
                    i--;
                else {
                    i++;
                    j++;
                    powered = false;
                }
                meta = 1;
                break;

            case 3: // '\003'
                if (dir) {
                    i--;
                    j++;
                    powered = false;
                } else
                    i++;
                meta = 1;
                break;

            case 4: // '\004'
                if (dir)
                    k++;
                else {
                    k--;
                    j++;
                    powered = false;
                }
                meta = 0;
                break;

            case 5: // '\005'
                if (dir) {
                    k++;
                    j++;
                    powered = false;
                } else
                    k--;
                meta = 0;
                break;
        }
        if (testPowered(world, i, j, k, spec, dir, dist, maxDist, meta))
            return true;
        return powered && testPowered(world, i, j - 1, k, spec, dir, dist, maxDist, meta);
    }

    protected boolean testPowered(World world, int i, int j, int k, TrackSpec spec, boolean dir, int dist, int maxDist, int orientation) {
        // System.out.println("Testing Power at <" + i + ", " + j + ", " + k + ">");
        Block blockToTest = world.getBlock(i, j, k);
        Block blockTrack = getBlock();
        if (blockToTest == blockTrack) {
            int meta = world.getBlockMetadata(i, j, k);
            TileEntity tile = world.getTileEntity(i, j, k);
            if (tile instanceof ITrackTile) {
                ITrackInstance track = ((ITrackTile) tile).getTrackInstance();
                if (!(track instanceof ITrackPowered) || track.getTrackSpec() != spec)
                    return false;
                if (orientation == 1 && (meta == 0 || meta == 4 || meta == 5))
                    return false;
                if (orientation == 0 && (meta == 1 || meta == 2 || meta == 3))
                    return false;
                if (((ITrackPowered) track).isPowered())
                    if (world.isBlockIndirectlyGettingPowered(i, j, k) || world.isBlockIndirectlyGettingPowered(i, j + 1, k))
                        return true;
                    else
                        return isConnectedRailPowered(world, i, j, k, spec, meta, dir, dist + 1, maxDist);
            }
        }
        return false;
    }

    @Override
    public IIcon getIcon() {
        return getTrackSpec().getItemIcon();
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void updateEntity() {
    }

    @Override
    public float getHardness() {
        return 1.05F;
    }

    @Override
    public float getExplosionResistance(double srcX, double srcY, double srcZ, Entity exploder) {
        return 3.5f;
    }

    @Override
    public void writePacketData(DataOutputStream data) throws IOException {
    }

    @Override
    public void readPacketData(DataInputStream data) throws IOException {
    }

    @Override
    public World getWorld() {
        return tileEntity.getWorldObj();
    }

    @Override
    public int getX() {
        return tileEntity.xCoord;
    }

    @Override
    public int getY() {
        return tileEntity.yCoord;
    }

    @Override
    public int getZ() {
        return tileEntity.zCoord;
    }

    /**
     * Return true if the rail can make corners. Used by placement logic.
     *
     * @return true if the rail can make corners.
     */
    @Override
    public boolean isFlexibleRail() {
        return false;
    }

    /**
     * Returns true if the rail can make up and down slopes. Used by placement
     * logic.
     *
     * @return true if the rail can make slopes.
     */
    @Override
    public boolean canMakeSlopes() {
        return true;
    }

    /**
     * Returns the max speed of the rail.
     *
     * @param cart The cart on the rail, may be null.
     * @return The max speed of the current rail.
     */
    @Override
    public float getRailMaxSpeed(EntityMinecart cart) {
        return 0.4f;
    }

}
