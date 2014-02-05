package appeng.api.config;


public enum MatchingMode implements IConfigEnum<MatchingMode> {
	Fuzzy,
	Precision;
	
	@Override
	public IConfigEnum[] getValues() {
		return MatchingMode.values();
	}

	@Override
	public String getName() {
		return "MatchingMode";
	}
}