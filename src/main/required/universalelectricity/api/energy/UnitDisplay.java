package universalelectricity.api.energy;

/**
 * An easy way to display information on electricity for the client.
 * 
 * @author Calclavia
 */
public class UnitDisplay
{
	/**
	 * Universal Electricity's units are in KILOJOULES, KILOWATTS and KILOVOLTS. Try to make your
	 * energy ratio as close to real life as possible.
	 */
	public static enum Unit
	{
		AMPERE("Amp", "I"), AMP_HOUR("Amp Hour", "Ah"), VOLTAGE("Volt", "V"), WATT("Watt", "W"),
		WATT_HOUR("Watt Hour", "Wh"), RESISTANCE("Ohm", "R"), CONDUCTANCE("Siemen", "S"),
		JOULES("Joule", "J"), LITER("Liter", "L"), NEWTON_METER("Newton Meter", "Nm"),
		REDFLUX("Redstone-Flux", "Rf"), MINECRAFT_JOULES("Minecraft-Joules", "Mj"),
		ELECTRICAL_UNITS("Electrical-Units", "Eu");

		public String name;
		public String symbol;

		private Unit(String name, String symbol)
		{
			this.name = name;
			this.symbol = symbol;
		}

		public String getPlural()
		{
			return this.name + "s";
		}
	}

	/** Metric system of measurement. */
	public static enum UnitPrefix
	{
		MICRO("Micro", "u", 0.000001), MILLI("Milli", "m", 0.001), BASE("", "", 1),
		KILO("Kilo", "k", 1000), MEGA("Mega", "M", 1000000), GIGA("Giga", "G", 1000000000),
		TERA("Tera", "T", 1000000000000d), PETA("Peta", "P", 1000000000000000d),
		EXA("Exa", "E", 1000000000000000000d), ZETTA("Zetta", "Z", 1000000000000000000000d),
		YOTTA("Yotta", "Y", 1000000000000000000000000d);

		/** long name for the unit */
		public String name;
		/** short unit version of the unit */
		public String symbol;
		/** Point by which a number is consider to be of this unit */
		public double value;

		private UnitPrefix(String name, String symbol, double value)
		{
			this.name = name;
			this.symbol = symbol;
			this.value = value;
		}

		public String getName(boolean getShort)
		{
			if (getShort)
			{
				return symbol;
			}
			else
			{
				return name;
			}
		}

		/** Divides the value by the unit value start */
		public double process(double value)
		{
			return value / this.value;
		}

		/** Checks if a value is above the unit value start */
		public boolean isAbove(double value)
		{
			return value > this.value;
		}

		/** Checks if a value is lower than the unit value start */
		public boolean isBellow(double value)
		{
			return value < this.value;
		}
	}

	public static String getDisplay(double value, Unit unit, int decimalPlaces, boolean isShort)
	{
		return getDisplay(value, unit, decimalPlaces, isShort, 1);
	}

	/**
	 * Displays the unit as text. Does handle negative numbers, and will place a negative sign in
	 * front of the output string showing this. Use string.replace to remove the negative sign if
	 * unwanted
	 */
	public static String getDisplay(double value, Unit unit, int decimalPlaces, boolean isShort, double multiplier)
	{
		String unitName = unit.name;
		String prefix = "";

		if (value < 0)
		{
			value = Math.abs(value);
			prefix = "-";
		}

		value *= multiplier;

		if (isShort)
		{
			unitName = unit.symbol;
		}
		else if (value > 1)
		{
			unitName = unit.getPlural();
		}

		if (value == 0)
		{
			return value + " " + unitName;
		}
		else
		{
			for (int i = 0; i < UnitPrefix.values().length; i++)
			{
				UnitPrefix lowerMeasure = UnitPrefix.values()[i];

				if (lowerMeasure.isBellow(value) && lowerMeasure.ordinal() == 0)
				{
					return prefix + roundDecimals(lowerMeasure.process(value), decimalPlaces) + " " + lowerMeasure.getName(isShort) + unitName;
				}
				if (lowerMeasure.ordinal() + 1 >= UnitPrefix.values().length)
				{
					return prefix + roundDecimals(lowerMeasure.process(value), decimalPlaces) + " " + lowerMeasure.getName(isShort) + unitName;
				}

				UnitPrefix upperMeasure = UnitPrefix.values()[i + 1];

				if ((lowerMeasure.isAbove(value) && upperMeasure.isBellow(value)) || lowerMeasure.value == value)
				{
					return prefix + roundDecimals(lowerMeasure.process(value), decimalPlaces) + " " + lowerMeasure.getName(isShort) + unitName;
				}
			}
		}

		return prefix + roundDecimals(value, decimalPlaces) + " " + unitName;
	}

	public static String getDisplay(double value, Unit unit)
	{
		return getDisplay(value, unit, 2, false);
	}

	public static String getDisplay(double value, Unit unit, UnitPrefix prefix)
	{
		return getDisplay(value, unit, 2, false, prefix.value);
	}

	public static String getDisplayShort(double value, Unit unit)
	{
		return getDisplay(value, unit, 2, true);
	}

	/**
	 * Gets a display for the value with a unit that is in the specific prefix.
	 */
	public static String getDisplayShort(double value, Unit unit, UnitPrefix prefix)
	{
		return getDisplay(value, unit, 2, true, prefix.value);
	}

	public static String getDisplayShort(double value, Unit unit, int decimalPlaces)
	{
		return getDisplay(value, unit, decimalPlaces, true);
	}

	public static String getDisplaySimple(double value, Unit unit, int decimalPlaces)
	{
		if (value > 1)
		{
			if (decimalPlaces < 1)
			{
				return (int) value + " " + unit.getPlural();
			}

			return roundDecimals(value, decimalPlaces) + " " + unit.getPlural();
		}

		if (decimalPlaces < 1)
		{
			return (int) value + " " + unit.name;
		}

		return roundDecimals(value, decimalPlaces) + " " + unit.name;
	}

	/**
	 * Rounds a number to a specific number place places
	 * 
	 * @param The number
	 * @return The rounded number
	 */
	public static double roundDecimals(double d, int decimalPlaces)
	{
		int j = (int) (d * Math.pow(10, decimalPlaces));
		return j / Math.pow(10, decimalPlaces);
	}

	public static double roundDecimals(double d)
	{
		return roundDecimals(d, 2);
	}
}
