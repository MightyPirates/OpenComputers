package mrtjp.projectred.api;

/**
 * Interface for tile entities that emit/receive bundled signal.
 */
public interface IBundledTile extends IBundledEmitter
{
    /**
     * @param side The side of this tile for which connection is being tested (forge direction ordering)
     * @return True if a bundled cable of some form can connect to this side.
     */
    public boolean canConnectBundled(int side);
}