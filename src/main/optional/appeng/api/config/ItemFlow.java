package appeng.api.config;


public enum ItemFlow implements IConfigEnum<ItemFlow> {
	
	READ, WRITE, READ_WRITE;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "ItemFlow";
	}
	
}