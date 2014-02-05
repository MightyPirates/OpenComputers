package forestry.api.apiculture;

import java.util.Locale;

public enum EnumBeeType {
	DRONE, PRINCESS, QUEEN, LARVAE, NONE;

	public static final EnumBeeType[] VALUES = values();

	String name;

	private EnumBeeType() {
		this.name = "bees." + this.toString().toLowerCase(Locale.ENGLISH);
	}

	public String getName() {
		return name;
	}
}
