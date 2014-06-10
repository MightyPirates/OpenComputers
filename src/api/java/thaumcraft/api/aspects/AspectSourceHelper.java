package thaumcraft.api.aspects;

import java.lang.reflect.Method;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.FMLLog;

public class AspectSourceHelper {

	static Method drainEssentia;
	static Method findEssentia;
	/**
	 * This method is what is used to drain essentia from jars and other sources for things like 
	 * infusion crafting or powering the arcane furnace. A record of possible sources are kept track of
	 * and refreshed as needed around the calling tile entity. This also renders the essentia trail particles.
	 * Only 1 essentia is drained at a time
	 * @param tile the tile entity that is draining the essentia
	 * @param aspect the aspect that you are looking for
	 * @param direction the direction from which you wish to drain. Forgedirection.Unknown simply seeks in all directions. 
	 * @param range how many blocks you wish to search for essentia sources. 
	 * @return boolean returns true if essentia was found and removed from a source.
	 */
	public static boolean drainEssentia(TileEntity tile, Aspect aspect, ForgeDirection direction, int range) {
	    try {
	        if(drainEssentia == null) {
	            Class fake = Class.forName("thaumcraft.common.lib.events.EssentiaHandler");
	            drainEssentia = fake.getMethod("drainEssentia", TileEntity.class, Aspect.class, ForgeDirection.class, int.class);
	        }
	        return (Boolean) drainEssentia.invoke(null, tile, aspect, direction, range);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.lib.events.EssentiaHandler method drainEssentia");
	    }
		return false;
	}
	
	/**
	 * This method returns if there is any essentia of the passed type that can be drained. It in no way checks how
	 * much there is, only if an essentia container nearby contains at least 1 point worth.
	 * @param tile the tile entity that is checking the essentia
	 * @param aspect the aspect that you are looking for
	 * @param direction the direction from which you wish to drain. Forgedirection.Unknown simply seeks in all directions. 
	 * @param range how many blocks you wish to search for essentia sources. 
	 * @return boolean returns true if essentia was found and removed from a source.
	 */
	public static boolean findEssentia(TileEntity tile, Aspect aspect, ForgeDirection direction, int range) {
	    try {
	        if(findEssentia == null) {
	            Class fake = Class.forName("thaumcraft.common.lib.events.EssentiaHandler");
	            findEssentia = fake.getMethod("findEssentia", TileEntity.class, Aspect.class, ForgeDirection.class, int.class);
	        }
	        return (Boolean) findEssentia.invoke(null, tile, aspect, direction, range);
	    } catch(Exception ex) { 
	    	FMLLog.warning("[Thaumcraft API] Could not invoke thaumcraft.common.lib.events.EssentiaHandler method findEssentia");
	    }
		return false;
	}
}
