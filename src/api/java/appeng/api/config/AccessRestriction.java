package appeng.api.config;

public enum AccessRestriction
{
	NO_ACCESS(0), READ(1), WRITE(2), READ_WRITE(3);

	private final int permisionBit;

	private AccessRestriction(int v) {
		permisionBit = v;
	}

	public boolean hasPermission(AccessRestriction ar)
	{
		return (permisionBit & ar.permisionBit) == ar.permisionBit;
	}

	public AccessRestriction restrictPermissions(AccessRestriction ar)
	{
		return getPermByBit( permisionBit & ar.permisionBit );
	}

	public AccessRestriction addPermissions(AccessRestriction ar)
	{
		return getPermByBit( permisionBit | ar.permisionBit );
	}

	public AccessRestriction removePermissions(AccessRestriction ar)
	{
		return getPermByBit( permisionBit & (~ar.permisionBit) );
	}

	private AccessRestriction getPermByBit(int bit)
	{
		switch (bit)
		{
		default:
		case 0:
			return NO_ACCESS;
		case 1:
			return READ;
		case 2:
			return WRITE;
		case 3:
			return READ_WRITE;
		}
	}
}