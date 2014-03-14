package forestry.api.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.Event;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IBreedingTracker;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.ISpeciesRoot;

public abstract class ForestryEvent extends Event {
	
	private static abstract class BreedingEvent extends ForestryEvent {
		public final ISpeciesRoot root;
		public final IBreedingTracker tracker;
		public final String username;
		
		private BreedingEvent(ISpeciesRoot root, String username, IBreedingTracker tracker) {
			super();
			this.root = root;
			this.username = username;
			this.tracker = tracker;
		}
	}
	
	public static class SpeciesDiscovered extends BreedingEvent {
		public final IAlleleSpecies species;
		public SpeciesDiscovered(ISpeciesRoot root, String username, IAlleleSpecies species, IBreedingTracker tracker) {
			super(root, username, tracker);
			this.species = species;
		}
	}
	
	public static class MutationDiscovered extends BreedingEvent {
		public final IMutation allele;
		public MutationDiscovered(ISpeciesRoot root, String username, IMutation allele,  IBreedingTracker tracker) {
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
