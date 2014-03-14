package appeng.api.config;


public enum ListMode implements IConfigEnum<ListMode> {
	
	WHITELIST, BLACKLIST;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "ListMode";
	}
	
}