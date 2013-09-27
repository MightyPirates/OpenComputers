package ic2.api.energy.prefab;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.FMLCommonHandler;

import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;

/**
 * BasicSource is a simple adapter to provide an ic2 energy source.
 * 
 * It's designed to be attached to a tile entity as a delegate. Functionally BasicSource acts as a
 * one-time configurable output energy buffer, thus providing a common use case for generators.
 * 
 * The constraints set by BasicSource like the strict tank-like energy buffering should provide a
 * more easy to use and stable interface than using IEnergySource directly while aiming for
 * optimal performance.
 * 
 * Using BasicSource involves the following steps:
 * - create a BasicSource instance in your TileEntity, typically in a field
 * - forward invalidate, onChunkUnload, readFromNBT, writeToNBT and updateEntity to the BasicSource
 *   instance.
 *   If you have other means of determining when the tile entity is fully loaded, notify onLoaded
 *   that way instead of forwarding updateEntity.
 * - call addEnergy whenever appropriate, using getFreeCapacity may determine if e.g. the generator
 *   should run
 * - optionally use getEnergyStored to display the output buffer charge level
 * - optionally use setEnergyStored to sync the stored energy to the client (e.g. in the Container)
 *
 * Example implementation code:
 * @code{.java}
 * public class SomeTileEntity extends TileEntity {
 *     // new basic energy source, 1000 EU buffer, tier 1 (32 EU/t, LV)
 *     private BasicSource ic2EnergySource = new BasicSource(this, 1000, 1);
 * 
 *     @Override
 *     public void invalidate() {
 *         ic2EnergySource.onInvalidate(); // notify the energy source
 *         ...
 *         super.invalidate(); // this is important for mc!
 *     }
 * 
 *     @Override
 *     public void onChunkUnload() {
 *         ic2EnergySource.onOnChunkUnload(); // notify the energy source
 *         ...
 *     }
 * 
 *     @Override
 *     public void readFromNBT(NBTTagCompound tag) {
 *         super.readFromNBT(tag);
 * 
 *         ic2EnergySource.onReadFromNbt(tag);
 *         ...
 *     }
 * 
 *     @Override
 *     public void writeToNBT(NBTTagCompound tag) {
 *         super.writeToNBT(tag);
 * 
 *         ic2EnergySource.onWriteToNbt(tag);
 *         ...
 *     }
 * 
 *     @Override
 *     public void updateEntity() {
 *         ic2EnergySource.onUpdateEntity(); // notify the energy source
 *         ...
 *         ic2EnergySource.addEnergy(5); // add 5 eu to the source's buffer this tick
 *         ...
 *     }
 * 
 *     ...
 * }
 * @endcode
 */
public class BasicSource extends TileEntity implements IEnergySource {

	// **********************************
	// *** Methods for use by the mod ***
	// **********************************

	/**
	 * Constructor for a new BasicSource delegate.
	 * 
	 * @param parent TileEntity represented by this energy source.
	 * @param capacity Maximum amount of eu to store.
	 * @param tier IC2 tier, 1=LV, 2=MV, ...
	 */
	public BasicSource(TileEntity parent, int capacity, int tier) {
		int power = EnergyNet.instance.getPowerFromTier(tier);

		this.parent = parent;
		this.capacity = capacity < power ? power : capacity;
		this.tier = tier;
	}

	// in-world te forwards	>>

	/**
	 * Forward for the TileEntity's updateEntity(), used for creating the energy net link.
	 * Either onUpdateEntity or onLoaded have to be used.
	 */
	public void onUpdateEntity() {
		if (!addedToEnet) onLoaded();
	}

	/**
	 * Notification that the TileEntity finished loaded, for advanced uses.
	 * Either onUpdateEntity or onLoaded have to be used.
	 */
	public void onLoaded() {
		if (!addedToEnet && !FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			worldObj = parent.worldObj;
			xCoord = parent.xCoord;
			yCoord = parent.yCoord;
			zCoord = parent.zCoord;

			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));

			addedToEnet = true;
		}
	}

	/**
	 * Forward for the TileEntity's invalidate(), used for destroying the energy net link.
	 * Both onInvalidate and onOnChunkUnload have to be used.
	 */
	public void onInvalidate() {
		if (addedToEnet) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));

			addedToEnet = false;
		}
	}

	/**
	 * Forward for the TileEntity's onChunkUnload(), used for destroying the energy net link.
	 * Both onInvalidate and onOnChunkUnload have to be used.
	 */
	public void onOnChunkUnload() {
		onInvalidate();
	}

	/**
	 * Forward for the TileEntity's readFromNBT(), used for loading the state.
	 * 
	 * @param tag Compound tag as supplied by TileEntity.readFromNBT()
	 */
	public void onReadFromNbt(NBTTagCompound tag) {
		NBTTagCompound data = tag.getCompoundTag("IC2BasicSource");

		energyStored = data.getDouble("energy");
	}

	/**
	 * Forward for the TileEntity's writeToNBT(), used for saving the state.
	 * 
	 * @param tag Compound tag as supplied by TileEntity.writeToNBT()
	 */
	public void onWriteToNbt(NBTTagCompound tag) {
		NBTTagCompound data = new NBTTagCompound();

		data.setDouble("energy", energyStored);

		tag.setTag("IC2BasicSource", data);
	}

	// << in-world te forwards
	// methods for using this adapter >>

	/**
	 * Determine the energy stored in the source's output buffer.
	 * 
	 * @return amount in EU
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
	 * Determine the free capacity in the source's output buffer.
	 * 
	 * @return amount in EU
	 */
	public double getFreeCapacity() {
		return capacity - energyStored;
	}

	/**
	 * Add some energy to the output buffer.
	 * 
	 * @param amount maximum amount of energy to add
	 * @return amount added/used, NOT remaining
	 */
	public double addEnergy(double amount) {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) return 0;
		if (amount > capacity - energyStored) amount = capacity - energyStored;

		energyStored += amount;

		return amount;
	}

	// << methods for using this adapter

	// ******************************
	// *** Methods for use by ic2 ***
	// ******************************

	// energy net interface >>

	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
		return true;
	}

	@Override
	public double getOfferedEnergy() {
		int power = EnergyNet.instance.getPowerFromTier(tier);

		if (energyStored >= power) {
			return power;
		} else {
			return 0;
		}
	}

	@Override
	public void drawEnergy(double amount) {
		energyStored -= amount;
	}

	// << energy net interface


	public final TileEntity parent;
	public final int capacity;
	public final int tier;

	protected double energyStored;
	protected boolean addedToEnet;
}
