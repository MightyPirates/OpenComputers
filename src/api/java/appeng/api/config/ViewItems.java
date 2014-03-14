package appeng.api.config;


public enum ViewItems implements IConfigEnum
{
	ALL,
	STORED,
	CRAFTABLE;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "ViewItems";
	}
}