package forestry.api.arboriculture;

public enum EnumGermlingType {
	SAPLING("Sapling"), BLOSSOM("Blossom"), POLLEN("Pollen"), GERMLING("Germling"), NONE("None");

	public static final EnumGermlingType[] VALUES = values();
	
	String name;

	private EnumGermlingType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
