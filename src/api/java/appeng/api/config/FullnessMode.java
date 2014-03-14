package appeng.api.config;


// do not change names..
public enum FullnessMode implements IConfigEnum {
	Empty,
	Half,
	Full;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}
	
	@Override
	public String getName() {
		return "FullnessMode";
	}
}