package appeng.api.config;

public enum InterfaceCraftingMode implements IConfigEnum {
	Craft,
	DontCraft;
	
	@Override
	public IConfigEnum[] getValues() {
		return values();
	}

	@Override
	public String getName() {
		return "InterfaceCraftingMode";
	}
	
}
