package appeng.api.config;


// do not change names..
public enum RedstoneModeInputOnOff implements IConfigEnum  {
	Ignore,
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