package forestry.api.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Optional annotation to provide additional information on IPlugins. This information will be available via the "/forestry plugin info $pluginID" command ingame.
 * 
 * @author SirSengir
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginInfo {

	/**
	 * @return Unique identifier for the plugin, no spaces!
	 */
	String pluginID();

	/**
	 * @return Nice and readable plugin name.
	 */
	String name();

	/**
	 * @return Plugin author's name.
	 */
	String author() default "";

	/**
	 * @return URL of plugin homepage.
	 */
	String url() default "";

	/**
	 * @return Version of the plugin, if any.
	 */
	String version() default "";

	/**
	 * @return Short description what the plugin does.
	 */
	String description() default "";

	/**
	 * @return Not used (yet?).
	 */
	String help() default "";

}
