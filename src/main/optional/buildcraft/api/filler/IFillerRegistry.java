package buildcraft.api.filler;

import buildcraft.api.gates.IAction;
import java.util.Set;

public interface IFillerRegistry {

	public void addPattern(IFillerPattern pattern);

	public IFillerPattern getPattern(String patternName);

	public IFillerPattern getNextPattern(IFillerPattern currentPattern);

	public IFillerPattern getPreviousPattern(IFillerPattern currentPattern);
	
	public Set<? extends IAction> getActions();
}
