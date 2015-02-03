package li.cil.oc.api.internal;

import net.minecraftforge.fluids.IFluidTank;

/**
 * Implemented by objects with multiple internal tanks.
 * <p/>
 * This is specifically for containers where the side does not matter when
 * accessing the internal tanks, only the index of the tank; unlike with the
 * {@link net.minecraftforge.fluids.IFluidHandler} interface.
 */
public interface MultiTank {
    /**
     * The number of tanks currently installed.
     */
    int tankCount();

    /**
     * Get the installed fluid tank with the specified index.
     *
     * @param index the index of the tank to get.
     * @return the tank with the specified index.
     */
    IFluidTank getFluidTank(int index);
}
