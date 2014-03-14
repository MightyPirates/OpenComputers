package appeng.api.events;

import net.minecraftforge.event.Event;
import appeng.api.me.tiles.ILocateable;

/**
 * Used to Notify the Location Registry of objects, and their availability.
 */
public class LocateableEventAnnounce extends Event {
	
	public enum LocateableEvent {
		Register, // Adds the locateable to the registry
		Unregister // Removes the Locatable from the registry
	};
	
	final public ILocateable target;
	final public LocateableEvent change;
	
	public LocateableEventAnnounce( ILocateable o, LocateableEvent ev )
	{
		target = o;
		change = ev;
	}
	
}
