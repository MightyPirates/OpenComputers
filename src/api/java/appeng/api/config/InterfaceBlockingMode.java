package appeng.api.config;

public enum InterfaceBlockingMode implements IConfigEnum {
	NonBlocking,
	Blocking;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "InterfaceBlockingMode";
	}
	
}