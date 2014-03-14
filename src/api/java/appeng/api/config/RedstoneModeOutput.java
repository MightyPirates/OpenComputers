package appeng.api.config;


// do not change names..
public enum RedstoneModeOutput implements IConfigEnum  {
	WhenOff,
	WhenOn;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "RedstoneMode";
	}
}