package appeng.api.config;


// do not change names..
public enum StackModeOutput implements IConfigEnum {
	Single,
	Stack,
	Craft,
	CraftOnly;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "StackMode";
	}
}