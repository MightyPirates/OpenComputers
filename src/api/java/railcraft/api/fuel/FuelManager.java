package mods.railcraft.api.fuel;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraftforge.fluids.Fluid;
import org.apache.logging.log4j.Level;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class FuelManager {

    public static final Map<Fluid, Integer> boilerFuel = new HashMap<Fluid, Integer>();

    /**
     * Register the amount of heat in a bucket of liquid fuel.
     *
     * @param fluid
     * @param heatValuePerBucket
     */
    public static void addBoilerFuel(Fluid fluid, int heatValuePerBucket) {
        ModContainer mod = Loader.instance().activeModContainer();
        String modName = mod != null ? mod.getName() : "An Unknown Mod";
        if (fluid == null) {
            FMLLog.log("Railcraft", Level.WARN, String.format("An error occured while %s was registering a Boiler fuel source", modName));
            return;
        }
        boilerFuel.put(fluid, heatValuePerBucket);
        FMLLog.log("Railcraft", Level.INFO, String.format("%s registered \"%s\" as a valid Boiler fuel source with %d heat.", modName, fluid.getName(), heatValuePerBucket));
    }

    public static int getBoilerFuelValue(Fluid fluid) {
        for (Entry<Fluid, Integer> entry : boilerFuel.entrySet()) {
            if (entry.getKey() == fluid)
                return entry.getValue();
        }
        return 0;
    }

}
