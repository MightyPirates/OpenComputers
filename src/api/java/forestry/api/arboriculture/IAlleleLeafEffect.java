package forestry.api.arboriculture;

import net.minecraft.world.World;
import forestry.api.genetics.IAlleleEffect;
import forestry.api.genetics.IEffectData;

/**
 * Simple allele encapsulating a leaf effect. (Not implemented)
 */
public interface IAlleleLeafEffect extends IAlleleEffect {

	IEffectData doEffect(ITreeGenome genome, IEffectData storedData, World world, int x, int y, int z);

}
