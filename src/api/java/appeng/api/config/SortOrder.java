package appeng.api.config;


public enum SortOrder implements IConfigEnum
{
	Name,
	Size,
	Priority,
	ItemID;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "SortOrder";
	}
}