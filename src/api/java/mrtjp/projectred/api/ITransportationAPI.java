package mrtjp.projectred.api;

public interface ITransportationAPI
{
    /**
     * Used to register a special link-state for routed pipes such as TE
     * Tesseracts.
     * 
     * @param link
     */
    public void registerSpecialLinkState(ISpecialLinkState link);
}
