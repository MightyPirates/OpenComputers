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
	 * @param id1 plant ID
	 * @param size1 plant size
	 * @param statGrowth1 plant growth stat
	 * @param statGain1 plant gain stat
	 * @param statResistance1 plant resistance stat
	 * @param stackSize1 for internal usage only
	 */
	public BaseSeed(int id1, int size1, int statGrowth1, int statGain1, int statResistance1, int stackSize1) {
		super();
		this.id = id1;
		this.size = size1;
		this.statGrowth = statGrowth1;
		this.statGain = statGain1;
		this.statResistance = statResistance1;
		this.stackSize = stackSize1;
	}
}
