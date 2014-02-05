package appeng.api.config;

public interface IConfigEnum <E> {
	
	IConfigEnum[] getValues();
	
	int ordinal();

	String getName();
	
}
