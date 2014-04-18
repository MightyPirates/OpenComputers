package appeng.api.config;

public enum FuzzyMode
{
	// Note that percentage damaged, is the inverse of percentage durability.
	IGNORE_ALL(-1), PERCENT_99(0), PERCENT_75(25), PERCENT_50(50), PERCENT_25(75);

	final public float breakPoint;
	final public int percentage;

	private FuzzyMode(int p) {
		percentage = p;
		breakPoint = calculateBreakPoint( 1 );
	}

	public int calculateBreakPoint(int maxDamage)
	{
		return (percentage * maxDamage) / 100;
	}

}