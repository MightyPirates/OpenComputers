package appeng.api.config;


// do not change names..
public enum OperationMode implements IConfigEnum {
	Fill,
	Empty;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}
	
	@Override
	public String getName() {
		return "OperationMode";
	}
}