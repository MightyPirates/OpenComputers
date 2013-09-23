package ic2.api.crops;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

/**
 * Interface implemented by the crop tile entity.
 */
public interface ICropTile {
	/**
	 * Get the crop's plant ID.
	 * 
	 * @return Plant ID, or -1 if there is no plant currently on the crop
	 */
	public short getID();
	
	/**
	 * Set the crop's plant ID.
	 * 
	 * @param id Plant ID, or -1 for no plant
	 */
	public void setID(short id);
	
	/**
	 * Get the crop's plant size.
	 * 
	 * @return Plant size, starting with 1 and maximum varies depending on plant
	 */
	public byte getSize();
	
	/**
	 * Set the crop's plant size.
	 * 
	 * @param size Plant size
	 */
	public void setSize(byte size);
	
	/**
	 * Get the crop's plant growth stat.
	 * Higher values indicate faster growth.
	 * 
	 * @return Plant growth stat
	 */
	public byte getGrowth();
	
	/**
	 * Set the crop's plant growth stat.
	 * 
	 * @param growth Plant growth stat
	 */
	public void setGrowth(byte growth);
	
	/**
	 * Get the crop's plant gain stat.
	 * Higher values indicate more drops.
	 * 
	 * @return Plant gain stat
	 */
	public byte getGain();
	
	/**
	 * Set the crop's plant gain stat.
	 * 
	 * @param gain Plant gain stat
	 */
	public void setGain(byte gain);
	
	/**
	 * Get the crop's plant resistance stat.
	 * Higher values indicate more resistance against trampling.
	 * 
	 * @return Plant resistance stat
	 */
	public byte getResistance();
	
	/**
	 * Set the crop's plant resistance stat.
	 * 
	 * @param resistance Plant resistance stat
	 */
	public void setResistance(byte resistance);
	
	/**
	 * Get the crop's plant scan level.
	 * Increases every time the seed is analyzed.
	 * 
	 * @return Plant scan level
	 */
	public byte getScanLevel();
	
	/**
	 * Set the crop's plant scan level.
	 * 
	 * @param scanLevel Plant scan level
	 */
	public void setScanLevel(byte scanLevel);
	
	/**
	 * Get the crop's plant custom data, stored alongside the crop.
	 * Can be modified in place.
	 * 
	 * @return Plant custom data
	 */
	public NBTTagCompound getCustomData();
	
	/**
	 * Get the crop's nutrient storage.
	 * Ranges from 0 to 100.
	 * 
	 * @return Crop nutrient storage
	 */
	public int getNutrientStorage();
	
	/**
	 * Set the crop's nutrient storage.
	 * 
	 * @param nutrientStorage Crop nutrient storage
	 */
	public void setNutrientStorage(int nutrientStorage);
	
	/**
	 * Get the crop's hydration storage.
	 * 0 indicates nothing, 1-10 indicate water hydration and 11-100 for hydration cells.
	 * 
	 * @return Crop hydration storage
	 */
	public int getHydrationStorage();
	
	/**
	 * Set the crop's hydration storage.
	 * 
	 * @param hydrationStorage Crop hydration storage
	 */
	public void setHydrationStorage(int hydrationStorage);
	
	/**
	 * Get the crop's Weed-Ex storage.
	 * 
	 * @return Crop Weed-Ex storage
	 */
	public int getWeedExStorage();
	
	/**
	 * Set the crop's Weed-Ex storage.
	 * 
	 * @param weedExStorage Crop Weed-Ex storage
	 */
	public void setWeedExStorage(int weedExStorage);
	
	/**
	 * Get the crop's humidity.
	 * Ranges from 0 (dry) to 10 (humid).
	 * Updates every couple of seconds or when an update is requested.
	 * 
	 * @see #updateState()
	 * 
	 * @return Crop humidity level
	 */
	public byte getHumidity();
	
	/**
	 * Get the crop's nutrient level.
	 * Ranges from 0 (empty) to 10 (full).
	 * Updates every couple of seconds or when an update is requested.
	 * 
	 * @see #updateState()
	 * 
	 * @return Crop nutrient level
	 */
	public byte getNutrients();
	
	/**
	 * Get the crop's air quality. 
	 * Ranges from 0 (cluttered) to 10 (fresh).
	 * Updates every couple of seconds or when an update is requested.
	 * 
	 * @see #updateState()
	 * 
	 * @return Crop air quality
	 */
	public byte getAirQuality();
	
	/**
	 * Get the crop's world.
	 * 
	 * @return Crop world
	 */
	public World getWorld();
	
	/**
	 * Get the crop's location.
	 * 
	 * @return Crop location
	 */
	public ChunkCoordinates getLocation();
	
	/**
	 * Get the crop's light level.
	 * 
	 * @return Crop light level
	 */
	public int getLightLevel();	
	
	/**
	 * Pick the crop, removing and giving seeds for the plant. 
	 * 
	 * @param manual whether it was done by hand (not automated)
	 * @return true if successfully picked
	 */
	public boolean pick(boolean manual);
	
	/**
	 * Harvest the crop, turning it into gain and resetting its size.
	 * 
	 * @param manual whether it one by hand (not automated)
	 * @return true if successfully harvested
	 */
	public boolean harvest(boolean manual);
	
	/**
	 * Fully clears the crop without dropping anything.
	 */
	public void reset();

	/**
	 * Request a texture and lighting update.
	 */
	public void updateState();

	/**
	 * Check if a block is under the farmland containing the crop.
	 * Searches up to 2 blocks below the farmland or an air space, whichever appears first.
	 * 
	 * @param block block to search
	 * @return Whether the block was found
	 */
	public boolean isBlockBelow(Block block);

	/**
	 * Generate plant seeds with the given parameters.
	 * 
	 * @param plant plant ID
	 * @param growth plant growth stat
	 * @param gain plant gain stat
	 * @param resis plant resistance stat
	 * @param scan plant scan level
	 * @return Plant seed item
	 */
	public ItemStack generateSeeds(short plant, byte growth, byte gain, byte resis, byte scan);
}
