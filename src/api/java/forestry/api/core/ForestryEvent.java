/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.core;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.common.eventhandler.Event;

import com.mojang.authlib.GameProfile;

import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IBreedingTracker;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.ISpeciesRoot;

public abstract class ForestryEvent extends Event {

	private static abstract class BreedingEvent extends ForestryEvent {
		public final ISpeciesRoot root;
		public final IBreedingTracker tracker;
		public final GameProfile username;

		private BreedingEvent(ISpeciesRoot root, GameProfile username, IBreedingTracker tracker) {
			super();
			this.root = root;
			this.username = username;
			this.tracker = tracker;
		}
	}

	public static class SpeciesDiscovered extends BreedingEvent {
		public final IAlleleSpecies species;
		public SpeciesDiscovered(ISpeciesRoot root, GameProfile username, IAlleleSpecies species, IBreedingTracker tracker) {
			super(root, username, tracker);
			this.species = species;
		}
	}

	public static class MutationDiscovered extends BreedingEvent {
		public final IMutation allele;
		public MutationDiscovered(ISpeciesRoot root, GameProfile username, IMutation allele,  IBreedingTracker tracker) {
			super(root, username, tracker);
			this.allele = allele;
		}
	}

	public static class SyncedBreedingTracker extends ForestryEvent {
		public final IBreedingTracker tracker;
		public final EntityPlayer player;
		public SyncedBreedingTracker(IBreedingTracker tracker, EntityPlayer player) {
			super();
			this.tracker = tracker;
			this.player = player;
		}

	}
}
