package appeng.api.me.tiles;

/**
 * Allows you to drain energy via ME Cables, if you only care if the grid is powered you only need IGridTileEntity.
 */
public abstract interface IGridMachine extends IGridTileEntity
{
    /**
     *  how much power this entity drains to run constantly.
     */
    public float getPowerDrainPerTick();
    
    /**
     * when a network is reset, this is trigger false, until booting is complete, please respect this and disable
     * network related activity during that time.
     * @param isReady
     */
    public void setNetworkReady( boolean isReady );
    
    /**
     * return true, when the machine is properly powered, the network is ready, and any other conditions required for running are met.
     * @return
     */
	public boolean isMachineActive();
}
