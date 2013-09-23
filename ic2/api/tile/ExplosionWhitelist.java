package ic2.api.tile;

import java.util.*;

import net.minecraft.block.Block;

/**
 * Blocks on this whitelist will not resist an explosion but won't be destroyed.
 * 
 * The explosion code by default ignores blocks which absorb more than 1000 explosion power to
 * prevent abusing personal safes, Trade-O-Mats and other blocks to serve as a cheap and
 * invulnerable reactor chambers. Said blocks will not shield the explosion and won't get
 * destroyed.
 */
public final class ExplosionWhitelist {
	/**
	 * Add a block to the whitelist.
	 * 
	 * @param block block to add
	 */
	public static void addWhitelistedBlock(Block block) {
		whitelist.add(block);
	}
	
	/**
	 * Remove a block from the whitelist.
	 * 
	 * @param block block to remove
	 */
	public static void removeWhitelistedBlock(Block block) {
		whitelist.remove(block);
	}
	
	/**
	 * Check if a block is on the whitelist.
	 * 
	 * @param block block to check if whitelisted
	 * @return Whether the block is whitelisted
	 */
	public static boolean isBlockWhitelisted(Block block) {
		return whitelist.contains(block);
	}

	private static Set<Block> whitelist = new HashSet<Block>();
}

