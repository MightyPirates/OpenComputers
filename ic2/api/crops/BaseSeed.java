package ic2.api.crops;

/**
 * Base agriculture seed. Used to determine the state of a plant once it is planted from an item.
 */
public class BaseSeed {
	/**
	 * Plant ID.
	 */
	public int id;
	
	/**
	 * Plant size.
	 */
	public int size;
	
	/**
	 * Plant growth stat.
	 */
	public int statGrowth;
	
	/**
	 * Plant gain stat.
	 */
	public int statGain;
	
	/**
	 * Plant resistance stat.
	 */
	public int statResistance;
	
	/**
	 * For internal usage only.
	 */
	public int stackSize;
	
	/**
	 * Create a BaseSeed object.
	 * 
	 * @param id plant ID
	 * @param size plant size
	 * @param statGrowth plant growth stat
	 * @param statGain plant gain stat
	 * @param statResistance plant resistance stat
	 * @param stackSize for internal usage only
	 */
	public BaseSeed(int id, int size, int statGrowth, int statGain, int statResistance, int stackSize) {
		super();
		this.id = id;
		this.size = size;
		this.statGrowth = statGrowth;
		this.statGain = statGain;
		this.statResistance = statResistance;
		this.stackSize = stackSize;
	}
}
