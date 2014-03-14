package appeng.api.me.tiles;

import java.util.List;

/**
 * Some ME Tiles can be configured, generally via interface, but these let you do this programatically.
 */
public interface IConfigureableTile {
	
	/** Switches configuration to next option... ( returns new configuration. ) */
	String prevConfiguration(String name);
	
	/** Switches configuration to next option... ( returns new configuration. ) */
	String nextConfiguration(String name); 
	
	/** Gets a list off all the configurations... */
	List<String> getConfigurations();
	
	/** Gets the options of a particular configuration. */
	List<String> getConfiguationOptions( String name );
	
	/** Set a configuration to a specific option... ( returns the old one. )  */
	String setConfiguration(String name, String value);
	
	/** The the current value for a configuration. */
	String getConfiguration(String name);
	
}
