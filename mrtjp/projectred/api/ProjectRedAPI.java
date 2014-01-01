package mrtjp.projectred.api;

import net.minecraft.world.World;

/**
 * Central API class for ProjectRed If ProjectRed is installed, the appropriate
 * field will contain an implementor of these methods. <br>
 * <br>
 * It is recommended that mods access this class within a soft dependency class.
 */
public final class ProjectRedAPI
{
    public static ProjectRedAPI instance;

    /**
     * API used for interacting with wires
     */
    public static ITransmissionAPI transmissionAPI;
    
    /**
     * API used for interacting with pipes
     */
    public static ITransportationAPI transportationAPI;
}
