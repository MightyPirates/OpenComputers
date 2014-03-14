package appeng.api.config;


public enum CondenserOuput implements IConfigEnum {
	Trash,
	MatterBalls,
	Singularity;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "CondenserOutput";
	}
	
}