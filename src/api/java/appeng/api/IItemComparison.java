package appeng.api;


public interface IItemComparison {
	
	public boolean sameAsPrecise( IItemComparison comp);

	public boolean sameAsFuzzy(IItemComparison comp);	
	
}
