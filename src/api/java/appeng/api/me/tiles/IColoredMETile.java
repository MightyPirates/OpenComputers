package appeng.api.me.tiles;

import net.minecraftforge.common.ForgeDirection;

/**
 * Used to signify which color a particular IGridTileEntity or IGridMachine is, you must implement both, if you wish to have color bias.
 */
public interface IColoredMETile
{
	/**
	 * Named Colors by int
	 */
	public static String[] Colors = {
		"Blue",
		"Black",
		"White",
		"Brown",
		"Red",
		"Yellow",
		"Green"
	};

	public static int[] ColorValues = {
		0x514AFF,
		0x666666,
		0xDBDBDB,
		0x724E35,
		0xFF003C,
		0xFFC651,
		0x7CFF4A
	};

	public static int[] ColorValuesBright = {
		0xCAEAFC,
		0xE7E7E7,
		0xFCFCFC,
		0xD5AD91,
		0xFCECF0,
		0xFCF2C7,
		0xDBFCCF
	};
	
	/**
	 * Fluix Colored, color for isColored() == false
	 */
	public static String ClearName = "Clear";
	public static int ClearColor = 0x895CA8;
	public static int ClearColorBright = 0xBFADD8;
	
	/**
	 * return true, if your block has a color, or false, if it dosn't.
	 * this allows you to have a colored block that can pretend to be colorless.
	 * @return true, if colored, false if not.
	 */
	boolean isColored( ForgeDirection dir );
	
	/**
	 * Change the color, AE dosn't call this except for its own blocks, its simply included for completeness.
	 * @param offset
	 */
	void setColor( int offset );
	
	/**
	 * which color is this tile?
	 * @return index into the above ColorsList.
	 */
	int getColor();
}
