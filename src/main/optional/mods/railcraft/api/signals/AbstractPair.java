/*
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at railcraft.wikispaces.com.
 */
package mods.railcraft.api.signals;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import mods.railcraft.api.core.WorldCoordinate;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public abstract class AbstractPair {

    private static final int SAFE_TIME = 32;
    private static final int PAIR_CHECK_INTERVAL = 128;
    protected static final Random rand = new Random();
    public final TileEntity tile;
    private WorldCoordinate coords;
    public final String name;
    public final int maxPairings;
    private boolean isBeingPaired;
    protected Deque<WorldCoordinate> pairings = new LinkedList<WorldCoordinate>();
    protected Set<WorldCoordinate> invalidPairings = new HashSet<WorldCoordinate>();
    private int update = rand.nextInt();
    private int ticksExisted;
    private boolean needsInit = true;

    public AbstractPair(String name, TileEntity tile, int maxPairings) {
        this.tile = tile;
        this.maxPairings = maxPairings;
        this.name = name;
    }

    protected void addPairing(WorldCoordinate other) {
        pairings.add(other);
        while (pairings.size() > getMaxPairings()) {
            pairings.remove();
        }
        SignalTools.packetBuilder.sendPairPacketUpdate(this);
    }

    public void clearPairing(WorldCoordinate other) {
        invalidPairings.add(other);
    }

    public void endPairing() {
        isBeingPaired = false;
    }

    public void tickClient() {
        if (needsInit) {
            needsInit = false;
            SignalTools.packetBuilder.sendPairPacketRequest(this);
        }
    }

    public void tickServer() {
        ticksExisted++;
        update++;
        if (ticksExisted >= SAFE_TIME && update % PAIR_CHECK_INTERVAL == 0)
            validatePairings();
    }

    protected void validatePairings() {
        for (WorldCoordinate coord : pairings) {
            getPairAt(coord);
        }
        cleanPairings();
    }

    public void cleanPairings() {
        boolean changed = pairings.removeAll(invalidPairings);
        invalidPairings.clear();
        if (changed)
            SignalTools.packetBuilder.sendPairPacketUpdate(this);
    }

    protected TileEntity getPairAt(WorldCoordinate coord) {
        if (!pairings.contains(coord))
            return null;

        if (coord.y < 0) {
            clearPairing(coord);
            return null;
        }

        int x = coord.x;
        int y = coord.y;
        int z = coord.z;
        if (!tile.worldObj.blockExists(x, y, z))
            return null;

        TileEntity target = tile.worldObj.getBlockTileEntity(x, y, z);
        if (isValidPair(target))
            return target;

        clearPairing(coord);
        return null;
    }

    public abstract boolean isValidPair(TileEntity tile);

    public WorldCoordinate getCoords() {
        if (coords == null)
            coords = new WorldCoordinate(tile.worldObj.provider.dimensionId, tile.xCoord, tile.yCoord, tile.zCoord);
        return coords;
    }

    public String getName() {
        return name;
    }

    public int getMaxPairings() {
        return maxPairings;
    }

    public int getNumPairs() {
        return pairings.size();
    }

    public boolean isPaired() {
        return !pairings.isEmpty();
    }

    public Collection<WorldCoordinate> getPairs() {
        return Collections.unmodifiableCollection(pairings);
    }

    public TileEntity getTile() {
        return tile;
    }

    public void startPairing() {
        isBeingPaired = true;
    }

    public boolean isBeingPaired() {
        return isBeingPaired;
    }

    public boolean isPairedWith(WorldCoordinate other) {
        return pairings.contains(other);
    }

    protected abstract String getTagName();

    public final void writeToNBT(NBTTagCompound data) {
        NBTTagCompound tag = new NBTTagCompound();
        saveNBT(tag);
        data.setCompoundTag(getTagName(), tag);
    }

    protected void saveNBT(NBTTagCompound data) {
        NBTTagList list = new NBTTagList();
        for (WorldCoordinate c : pairings) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setIntArray("coords", new int[]{c.dimension, c.x, c.y, c.z});
            list.appendTag(tag);
        }
        data.setTag("pairings", list);
    }

    public final void readFromNBT(NBTTagCompound data) {
        NBTTagCompound tag = data.getCompoundTag(getTagName());
        loadNBT(tag);
    }

    protected void loadNBT(NBTTagCompound data) {
        NBTTagList list = data.getTagList("pairings");
        for (byte entry = 0; entry < list.tagCount(); entry++) {
            NBTTagCompound tag = (NBTTagCompound) list.tagAt(entry);
            int[] c = tag.getIntArray("coords");
            pairings.add(new WorldCoordinate(c[0], c[1], c[2], c[3]));
        }
    }

    @SideOnly(Side.CLIENT)
    public void addPair(int x, int y, int z) {
        pairings.add(new WorldCoordinate(tile.worldObj.provider.dimensionId, x, y, z));
    }

    @SideOnly(Side.CLIENT)
    public void removePair(int x, int y, int z) {
        pairings.remove(new WorldCoordinate(tile.worldObj.provider.dimensionId, x, y, z));
    }

    public void clearPairings() {
        pairings.clear();
        if (!tile.worldObj.isRemote)
            SignalTools.packetBuilder.sendPairPacketUpdate(this);
    }

}
