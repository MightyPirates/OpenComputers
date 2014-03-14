package atomicscience.api;

public interface IReactor extends ITemperature
{
	public void heat(long energy);

	public boolean isOverToxic();
}
