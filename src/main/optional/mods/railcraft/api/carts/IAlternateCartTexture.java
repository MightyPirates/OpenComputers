/*
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at railcraft.wikispaces.com.
 */
package mods.railcraft.api.carts;

import net.minecraft.util.ResourceLocation;

/**
 * Used to render a cart with a custom texture using Railcraft's cart renderer.
 * You could always write your own renderer of course.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface IAlternateCartTexture {

    /**
     * The texture to give the cart. If you return null, the default is used.
     *
     * @return the texture file
     */
    public ResourceLocation getTextureFile();
}
