package appeng.api.config;

import cpw.mods.fml.common.Loader;


public enum SearchBoxMode implements IConfigEnum<ActionItems> {
	Autosearch,
	Standard,
	NEIAutoSearch,
	NEIStandard;

	@Override
	public IConfigEnum[] getValues() {
		if ( Loader.isModLoaded( "NotEnoughItems" ) )
			return values();
		return new SearchBoxMode[]{ Autosearch, Standard };
	}

	@Override
	public String getName() {
		return "SearchBoxMode";
	}
}