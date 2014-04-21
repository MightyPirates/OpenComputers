package appeng.api.networking.events;

import appeng.api.networking.IGridHost;

/**
 *  An event that is posted Whenever a spatial IO is Actived, called for IGridCache
 *
 * @param IGridHost ( instance of the SpatialIO block )
 * @param double ( the amount of energy that the SpatialIO uses)
 */
 
 
public class MENetworkSpatialEvent extends MENetworkEvent
{

  public final IGridHost host;
  public final double spatialEnergyUsage;
  
  
  public MENetworkSpatialEvent(IGridHost SpatialIO, double EnergyUsage) {
               host = SpatialIO;
               spatialEnergyUsage = EnergyUsage;
               
        }
  
}
