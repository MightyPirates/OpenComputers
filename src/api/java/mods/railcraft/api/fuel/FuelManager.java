package mods.railcraft.api.fuel;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraftforge.fluids.Fluid;

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
        boilerFuel.put(fluid, heatValuePerBucket);
    }

    public static int getBoilerFuelValue(Fluid fluid) {
        for (Entry<Fluid, Integer> entry : boilerFuel.entrySet()) {
            if (entry.getKey() == fluid) {
                return entry.getValue();
            }
        }
        return 0;
    }

}
