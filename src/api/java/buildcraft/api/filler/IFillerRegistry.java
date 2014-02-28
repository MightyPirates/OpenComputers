/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
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
