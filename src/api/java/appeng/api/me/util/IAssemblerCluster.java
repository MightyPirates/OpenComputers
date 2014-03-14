package appeng.api.me.util;

import net.minecraft.tileentity.TileEntity;

/**
 * Definition for a MAC
 *
 */
public interface IAssemblerCluster {
	
	/**
	 * cycles the cpus, should be called once a tick, if called more, crafting will accelerate.
	 */
	public void cycleCpus();
	
	/**
	 * Tells the MAC one of the cpus was used.
	 */
	void addCraft();
	
	/**
	 * is there a CPU available for Crafting?
	 * @return hasCPUReady
	 */
	boolean canCraft();
	
	/**
	 * Gets a TileAssembler from the MAC
	 * @param assemblerOffset
	 * @return TileEntity for that Assembler or Null
	 */
	TileEntity getAssembler( int assemblerOffset );

	public int getLastOffset();
	
	public void setLastOffset( int x );
}
