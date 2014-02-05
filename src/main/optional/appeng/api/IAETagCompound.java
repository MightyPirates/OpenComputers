package appeng.api;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Don't cast this... either compare with it, or copy it.
 * 
 * Don't Implement.
 */
public interface IAETagCompound {
	
	/**
	 * Create a copy ( the copy will not be a IAETagCompount, it will be a NBTTagCompound. )
	 * @return
	 */
	public NBTTagCompound getNBTTagCompoundCopy();
	
	/**
	 * compare to other NBTTagCompounds or IAETagCompounds
	 * @param a
	 * @return true, if they are the same.
	 */
	boolean equals( Object a );
	
	/**
	 * returns the special comparison for this tag.
	 * @return
	 */
	IItemComparison getSpecialComparison();
	
}
