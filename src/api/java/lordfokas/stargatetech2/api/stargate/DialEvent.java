package stargatetech2.api.stargate;


import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

public abstract class DialEvent extends Event {
	public final Address sourceAddress;
	public final Address destAddress;
	public final int duration;
	
	public DialEvent(Address src, Address dst, int dur) {
		sourceAddress = src;
		destAddress = dst;
		duration = dur;
	}

	@Cancelable
	public static class Pre extends DialEvent {
		public Pre(Address src, Address dst, int dur) {
			super(src, dst, dur);
		}
	}
	
	public static class Success extends DialEvent {
		public Success(Address src, Address dst, int dur) {
			super(src, dst, dur);
		}
	}
	
	public static class Error extends DialEvent {
		public final DialError error;
		
		public Error(Address src, Address dst, DialError error) {
			super(src, dst, -1);
			this.error = error;
		}
	}
}
