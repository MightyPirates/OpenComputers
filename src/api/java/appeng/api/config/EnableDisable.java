package appeng.api.config;


public enum EnableDisable implements IConfigEnum {
	Enabled,
	Disabled;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "CondenserOutput";
	}
	
}