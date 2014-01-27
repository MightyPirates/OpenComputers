/**
 * 
 */
package universalelectricity.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Calclavia
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UniversalClass
{
	/**
	 * The mods to integrate with.
	 * 
	 * e.g: "IndustrialCraft;ThermalExpansion" <- Enable IC and TE compatibility.
	 * e.g: "" <- Enable all mod compatibility
	 * 
	 * @return Return an empty string to be compatible with all available mods, or each
	 * CompatibilityType's enum.moduleName separated by semi-columns.
	 * 
	 */
	public String integration() default "";
}
