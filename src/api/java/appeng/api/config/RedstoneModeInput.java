package appeng.api.config;


// do not change names..
public enum RedstoneModeInput implements IConfigEnum  {
	Ignore,
	WhenOff,
	WhenOn,
	OnPulse;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "RedstoneMode";
	}
}