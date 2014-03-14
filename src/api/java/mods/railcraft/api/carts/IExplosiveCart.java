package mods.railcraft.api.carts;

public interface IExplosiveCart
{

    /**
     * If set to true the cart should explode after
     * whatever fuse duration is set.
     *
     * @param primed
     */
    public void setPrimed(boolean primed);

    /**
     * Returns whether the cart is primed to explode.
     *
     * @return primed
     */
    public boolean isPrimed();

    /**
     * Returns the length of the current fuse.
     *
     * @return fuse length in ticks
     */
    public int getFuse();

    /**
     * Optional function to allow setting the fuse duration.
     *
     * Used by the Priming Track.
     *
     * @param fuse in ticks
     */
    public void setFuse(int fuse);

    /**
     * Returns the blast radius, but I don't think anything currently uses this.
     *
     * @return blast radius
     */
    public float getBlastRadius();

    /**
     * Optional function to allow setting the blast radius.
     *
     * @param radius
     */
    public void setBlastRadius(float radius);

    /**
     * Causes the cart to explode immediately.
     *
     */
    public void explode();
}
