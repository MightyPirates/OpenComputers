package cofh.api.world;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

/**
 * This interface should be implemented on classes which define a world feature to be generated in a {@link IFeatureHandler}. It is essentially a more robust
 * version of {@link WorldGenerator}, and may include one or more WorldGenerators should you wish.
 * 
 * @author King Lemming
 * 
 */
public interface IFeatureGenerator {

	/**
	 * Returns the name of the feature, used for unique identification in configs and retrogen.
	 */
	public String getFeatureName();

	/**
	 * Generates the world feature.
	 * 
	 * @param random
	 *            Random derived from the world seed.
	 * @param chunkX
	 *            Minimum X chunk-coordinate of the chunk. (x16 for block coordinate)
	 * @param chunkZ
	 *            Minimum Z chunk-coordinate of the chunk. (x16 for block coordinate)
	 * @param world
	 *            The world to generate in.
	 * @param newGen
	 *            True on initial generation, false on retrogen.
	 * @return True if generation happened, false otherwise.
	 */
	public boolean generateFeature(Random random, int chunkX, int chunkZ, World world, boolean newGen);

}
