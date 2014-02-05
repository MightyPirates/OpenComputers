package appeng.api.me.tiles;

import appeng.api.WorldCoord;
import appeng.api.me.util.IAssemblerCluster;

public interface IAssemblerMB {

    /**
     *  Do this:
     *  	return new WorldCoord( TileEntity.xCoord, TileEntity.yCoord, TileEntity.zCoord );
     */
    public WorldCoord getLocation();
    public IAssemblerCluster getCluster();
    public void updateStatus( IAssemblerCluster ac );
	public boolean isComplete();
	
	void calculateMultiblock( long instanceCalc );
	public long markViewed(long inst);
    
}
