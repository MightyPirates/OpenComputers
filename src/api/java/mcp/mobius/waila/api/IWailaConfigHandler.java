package mcp.mobius.waila.api;

import java.util.HashMap;
import java.util.Set;

public interface IWailaConfigHandler {
	/* Returns a set of all the currently loaded modules in the config handler */
	public Set<String> getModuleNames();
	
	/* Returns all the currently available options for a given module */
	public HashMap<String, String> getConfigKeys(String modName);
	
	/* Add a new option to a given module 
	 * 
	 * modName is the name of the module to add the option to (ie : Buildcraft, IndustrialCraft2, etc)
	 * key is the config key (ie : bc.tankcontent, ic2.inputvalue)
	 * name is the human readable name of the option (ie : "Tank content", "Max EU Input")
	 * */
	//public void addConfig(String modName, String key, String name);
	
	/* Returns the current value of an option (true/false) with a default value defvalue if not set*/
	public boolean getConfig(String key, boolean defvalue);
	
	/* Returns the current value of an option (true/false) with a default value true if not set*/	
	public boolean getConfig(String key);	
	
	//public void setConfig(String key, boolean value);
}
