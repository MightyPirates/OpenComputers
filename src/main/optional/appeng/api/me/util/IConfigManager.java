package appeng.api.me.util;

import appeng.api.config.IConfigEnum;
import appeng.api.me.tiles.IConfigureableTile;

public interface IConfigManager extends IConfigureableTile
{
	
	IConfigEnum getSetting( String sName );
}
