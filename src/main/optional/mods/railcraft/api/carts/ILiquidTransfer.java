package mods.railcraft.api.carts;

import net.minecraftforge.fluids.FluidStack;

/**
 * This interface allows carts to transfer liquid between each other as well as
 * adding a couple other functions related to liquids.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface ILiquidTransfer {

    /**
     * Offers liquid to this object.
     *
     * Is not used by the Liquid Loader to load carts, the traditional
     * ILiquidContainer is used for that.
     *
     * @param source The Object offering the liquid, used to prevent request
     * loops in trains
     * @param quantity The quantity offered
     * @param id The liquid id offered
     * @return the liquid used
     */
    public int offerLiquid(Object source, FluidStack offer);

    /**
     * Requests liquid from this object.
     *
     * Is not used by the Liquid Unloader to drain carts, the traditional
     * ILiquidContainer is used for that.
     *
     * @param source The Object requesting the liquid, used to prevent request
     * loops in trains
     * @param quantity The quantity requested
     * @param id The liquid type requested
     * @return the liquid provided
     */
    public int requestLiquid(Object source, FluidStack request);

    /**
     * Set by the Liquid Loader while filling, primarily used for rendering a
     * visible change while being filled.
     *
     * @param filling
     */
    public void setFilling(boolean filling);

    /**
     *
     * @return true if being filled
     */
    public boolean isFilling();
}
