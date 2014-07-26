/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
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
