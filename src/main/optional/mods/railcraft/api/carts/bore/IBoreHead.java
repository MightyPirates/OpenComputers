package mods.railcraft.api.carts.bore;

import net.minecraft.util.ResourceLocation;

/**
 * This interface it used to define an item that can
 * be used as a bore head for the Tunnel Bore.
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface IBoreHead
{

    /**
     * Return the texture file used for this bore head.
     * @return The texture file path
     */
    public ResourceLocation getBoreTexture();

    /**
     * Return the harvest level of this bore head.
     *
     * This value is compared against the tool classes
     * "pickaxe", "axe", and "shovel" to determine if the
     * block is harvestable by the bore head.
     *
     * @return The harvest level
     */
    public int getHarvestLevel();

    /**
     * Return the dig speed modifier of this bore head.
     *
     * This value controls how much faster or slow this bore head
     * mines each layer compared to the default time.
     *
     * @return The dig speed modifier
     */
    public float getDigModifier();
}
