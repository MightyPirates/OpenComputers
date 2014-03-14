package ic2.api.energy.prefab;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.FMLCommonHandler;

import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.info.Info;
import ic2.api.item.ElectricItem;

/**
 * BasicSink is a simple adapter to provide an ic2 energy sink.
 * 
 * It's designed to be attached to a tile entity as a delegate. Functionally BasicSink acts as a
 * one-time configurable input energy buffer, thus providing a common use case for machines.
 * 
 * Sub-classing BasicSink instead of using it as a delegate works as well, but isn't recommended.
 * The delegate can be extended with additional functionality through a sub class though.
 * 
 * The constraints set by BasicSink like the strict tank-like energy buffering should provide a
 * more easy to use and stable interface than using IEnergySink directly while aiming for
 * optimal performance.
 * 
 * Using BasicSink involves the following steps:
 * - create a BasicSink instance in your TileEntity, typically in a field
 * - forward invalidate, onChunkUnload, readFromNBT, writeToNBT and updateEntity to the BasicSink
 *   instance.
 *   If you have other means of determining when the tile entity is fully loaded, notify onLoaded
 *   that way instead of forwarding updateEntity.
 * - call useEnergy whenever appropriate. canUseEnergy determines if enough energy is available
 *   without consuming the energy.
 * - optionally use getEnergyStored to display the output buffer charge level
 * - optionally use setEnergyStored to sync the stored energy to the client (e.g. in the Container)
 *
 * Example implementation code:
 * @code{.java}
 * public class SomeTileEntity extends TileEntity {
 *     // new basic energy sink, 1000 EU buffer, tier 1 (32 EU/t, LV)
 *     private BasicSink ic2EnergySink = new BasicSink(this, 1000, 1);
 * 
 *     @Override
 *     public void invalidate() {
 *         ic2EnergySink.invalidate(); // notify the energy sink
 *         ...
 *         super.invalidate(); // this is important for mc!
 *     }
 * 
 *     @Override
 *     public void onChunkUnload() {
 *         ic2EnergySink.onChunkUnload(); // notify the energy sink
 *         ...
 *     }
 * 
 *     @Override
 *     public void readFromNBT(NBTTagCompound tag) {
 *         super.readFromNBT(tag);
 * 
 *         ic2EnergySink.readFromNBT(tag);
 *         ...
 *     }
 * 
 *     @Override
 *     public void writeToNBT(NBTTagCompound tag) {
 *         super.writeToNBT(tag);
 * 
 *         ic2EnergySink.writeToNBT(tag);
 *         ...
 *     }
 * 
 *     @Override
 *     public void updateEntity() {
 *         ic2EnergySink.updateEntity(); // notify the energy sink
 *         ...
 *         if (ic2EnergySink.useEnergy(5)) { // use 5 eu from the sink's buffer this tick
 *             ... // do something with the energy
 *         }
 *         ...
 *     }
 * 
 *     ...
 * }
 * @endcode
 */
public class BasicSink extends TileEntity implements IEnergySink {

	// **********************************
	// *** Methods for use by the mod ***
	// **********************************

	/**
	 * Constructor for a new BasicSink delegate.
	 * 
	 * @param parent1 TileEntity represented by this energy sink.
	 * @param capacity1 Maximum amount of eu to store.
	 * @param tier1 IC2 tier, 1=LV, 2=MV, ...
	 */
	public BasicSink(TileEntity parent1, int capacity1, int tier1) {
		this.parent = parent1;
		this.capacity = capacity1;
		this.tier = tier1;
	}

	// in-world te forwards	>>

	/**
	 * Forward for the base TileEntity's updateEntity(), used for creating the energy net link.
	 * Either updateEntity or onLoaded have to be used.
	 */
	@Override
	public void updateEntity() {
		if (!addedToEnet) onLoaded();
	}

	/**
	 * Notification that the base TileEntity finished loaded, for advanced uses.
	 * Either updateEntity or onLoaded have to be used.
	 */
	public void onLoaded() {
		if (!addedToEnet &&
				!FMLCommonHandler.instance().getEffectiveSide().isClient() &&
				Info.isIc2Available()) {
			worldObj = parent.worldObj;
			xCoord = parent.xCoord;
			yCoord = parent.yCoord;
			zCoord = parent.zCoord;

			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));

			addedToEnet = true;
		}
	}

	/**
	 * Forward for the base TileEntity's invalidate(), used for destroying the energy net link.
	 * Both invalidate and onChunkUnload have to be used.
	 */
	@Override
	public void invalidate() {
		super.invalidate();

		onChunkUnload();
	}

	/**
	 * Forward for the base TileEntity's onChunkUnload(), used for destroying the energy net link.
	 * Both invalidate and onChunkUnload have to be used.
	 */
	@Override
	public void onChunkUnload() {
		if (addedToEnet &&
				Info.isIc2Available()) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));

			addedToEnet = false;
		}
	}

	/**
	 * Forward for the base TileEntity's readFromNBT(), used for loading the state.
	 * 
	 * @param tag Compound tag as supplied by TileEntity.readFromNBT()
	 */
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);

		NBTTagCompound data = tag.getCompoundTag("IC2BasicSink");

		energyStored = data.getDouble("energy");
	}

	/**
	 * Forward for the base TileEntity's writeToNBT(), used for saving the state.
	 * 
	 * @param tag Compound tag as supplied by TileEntity.writeToNBT()
	 */
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		try {
			super.writeToNBT(tag);
		} catch (RuntimeException e) {
			// happens if this is a delegate, ignore
		}

		NBTTagCompound data = new NBTTagCompound();

		data.setDouble("energy", energyStored);

		tag.setTag("IC2BasicSink", data);
	}

	// << in-world te forwards
	// methods for using this adapter >>

	/**
	 * Get the maximum amount of energy this sink can hold in its buffer.
	 * 
	 * @return Capacity in EU.
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * Set the maximum amount of energy this sink can hold in its buffer.
	 * 
	 * @param capacity1 Capacity in EU.
	 */
	public void setCapacity(int capacity1) {
		this.capacity = capacity1;
	}

	/**
	 * Get the IC2 energy tier for this sink.
	 * 
	 * @return IC2 Tier.
	 */
	public int getTier() {
		return tier;
	}

	/**
	 * Set the IC2 energy tier for this sink.
	 * 
	 * @param tier1 IC2 Tier.
	 */
	public void setTier(int tier1) {
		this.tier = tier1;
	}

	/**
	 * Determine the energy stored in the sink's input buffer.
	 * 
	 * @return amount in EU, may be above capacity
	 */
	public double getEnergyStored() {
		return energyStored;
	}

	/**
	 * Set the stored energy to the specified amount.
	 * 
	 * This is intended for server -> client synchronization, e.g. to display the stored energy in
	 * a GUI through getEnergyStored().
	 * 
	 * @param amount
	 */
	public void setEnergyStored(double amount) {
		energyStored = amount;
	}

	/**
	 * Determine if the specified amount of energy is available.
	 * 
	 * @param amount in EU
	 * @return true if the amount is available
	 */
	public boolean canUseEnergy(double amount) {
		return energyStored >= amount;
	}

	/**
	 * Use the specified amount of energy, if available.
	 * 
	 * @param amount amount to use
	 * @return true if the amount was available
	 */
	public boolean useEnergy(double amount) {
		if (canUseEnergy(amount) && !FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			energyStored -= amount;

			return true;
		}
		return false;
	}

	/**
	 * Discharge the supplied ItemStack into this sink's energy buffer.
	 * 
	 * @param stack ItemStack to discharge (null is ignored)
	 * @param limit Transfer limit, values <= 0 will use the battery's limit
	 * @return true if energy was transferred
	 */
	public boolean discharge(ItemStack stack, int limit) {
		if (stack == null || !Info.isIc2Available()) return false;

		int amount = (int) Math.floor(capacity - energyStored);
		if (amount <= 0) return false;

		if (limit > 0 && limit < amount) amount = limit;

		amount = ElectricItem.manager.discharge(stack, amount, tier, limit > 0, false);

		energyStored += amount;

		return amount > 0;
	}

	// << methods for using this adapter

	// backwards compatibility (ignore these) >>

	@Deprecated
	public void onUpdateEntity() {
		updateEntity();
	}

	@Deprecated
	public void onInvalidate() {
		invalidate();
	}

	@Deprecated
	public void onOnChunkUnload() {
		onChunkUnload();
	}

	@Deprecated
	public void onReadFromNbt(NBTTagCompound tag) {
		readFromNBT(tag);
	}

	@Deprecated
	public void onWriteToNbt(NBTTagCompound tag) {
		writeToNBT(tag);
	}

	// << backwards compatibility

	// ******************************
	// *** Methods for use by ic2 ***
	// ******************************

	// energy net interface >>

	@Override
	public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction) {
		return true;
	}

	@Override
	public double demandedEnergyUnits() {
		return Math.max(0, capacity - energyStored);
	}

	@Override
	public double injectEnergyUnits(ForgeDirection directionFrom, double amount) {
		energyStored += amount;

		return 0;
	}

	@Override
	public int getMaxSafeInput() {
		return EnergyNet.instance.getPowerFromTier(tier);
	}

	// << energy net interface


	public final TileEntity parent;

	protected int capacity;
	protected int tier;
	protected double energyStored;
	protected boolean addedToEnet;
}
