package buildcraft.core;

import buildcraft.api.gates.IAction;

public abstract interface IMachine
{
    public abstract boolean isActive();

    public abstract boolean manageFluids();

    public abstract boolean manageSolids();

    public abstract boolean allowAction(IAction paramIAction);
}