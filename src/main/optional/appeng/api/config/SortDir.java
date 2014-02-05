package appeng.api.config;


public enum SortDir implements IConfigEnum
{
	ASC,
	DESC;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "SortDir";
	}
}