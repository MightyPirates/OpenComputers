/*******************************************************************************
 * Copyright 2011-2014 SirSengir
 * 
 * This work (the API) is licensed under the "MIT" License, see LICENSE.txt for details.
 ******************************************************************************/
package forestry.api.genetics;

/**
 * Biological classifications from domain down to genus.
 * 
 * Used by the *alyzers to display hierarchies.
 */
public interface IClassification {

	public enum EnumClassLevel {

		DOMAIN(0x777fff, true), KINGDOM(0x77c3ff), PHYLUM(0x77ffb6, true), DIVISION(0x77ffb6, true), CLASS(0x7bff77), ORDER(0xbeff77), FAMILY(0xfffd77),
		SUBFAMILY(0xfffd77), TRIBE(0xfffd77), GENUS(0xffba77);

		private int colour;
		private boolean isDroppable;

		private EnumClassLevel(int colour) {
			this(colour, false);
		}

		private EnumClassLevel(int colour, boolean isDroppable) {
			this.colour = colour;
			this.isDroppable = isDroppable;
		}

		/**
		 * @return Colour to use for displaying this classification.
		 */
		public int getColour() {
			return colour;
		}

		/**
		 * @return Indicates whether display of this classification level can be ommitted in case of space constraints.
		 */
		public boolean isDroppable() {
			return isDroppable;
		}
	}

	/**
	 * @return Level inside the full hierarchy this particular classification is located at.
	 */
	EnumClassLevel getLevel();

	/**
	 * @return Unique String identifier.
	 */
	String getUID();

	/**
	 * @return Localized branch name for user display.
	 */
	String getName();

	/**
	 * A branch approximates a "genus" in real life. Real life examples: "Micrapis", "Megapis"
	 * 
	 * @return flavour text (may be null)
	 */
	String getScientific();

	/**
	 * @return Localized description of this branch. (May be null.)
	 */
	String getDescription();

	/**
	 * @return Member groups of this one.
	 */
	IClassification[] getMemberGroups();

	/**
	 * Adds subgroups to this group.
	 */
	void addMemberGroup(IClassification group);

	/**
	 * @return Member species of this group.
	 */
	IAlleleSpecies[] getMemberSpecies();

	/**
	 * Used by the allele registry to populate internal collection of branch members on the fly.
	 * 
	 * @param species
	 */
	void addMemberSpecies(IAlleleSpecies species);

	/**
	 * @return Parent classification, null if this is root.
	 */
	IClassification getParent();

	/**
	 * Only used internally by the AlleleRegistry if this classification has been added to another one.
	 * 
	 * @param parent
	 */
	void setParent(IClassification parent);
}
