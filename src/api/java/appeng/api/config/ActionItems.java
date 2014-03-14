package appeng.api.config;


public enum ActionItems implements IConfigEnum<ActionItems> {
	Wrench,
	Close;

	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "FuzzyMode";
	}
}