package forestry.api.mail;

public interface IPostalState {
	boolean isOk();

	String getIdentifier();
	
	int ordinal();
}
