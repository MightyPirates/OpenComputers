/*
 * Copyright (c) CovertJaguar, 2011 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at railcraft.wikispaces.com.
 */
package mods.railcraft.api.carts.locomotive;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

/**
 * To define a new Locomotive Model Renderer, extend this class and register it
 * with the appropriate LocomotiveRendererType.
 *
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public abstract class LocomotiveModelRenderer {

    private final String rendererTag;
    private boolean renderIn3D = true;

    /**
     *
     * @param rendererTag The Renderer Tag should be in the form of
     * "<modid>:<name>", and should be unique among all the renderers defined
     * for that type of locomotive. The Railcraft default model for each type of
     * locomotive is defined as "railcraft:default".
     */
    public LocomotiveModelRenderer(String rendererTag) {
        this.rendererTag = rendererTag;
    }

    public final String getRendererTag() {
        return rendererTag;
    }

    /**
     * This is the string that will be displayed on the Item tool tip. It should
     * be localized.
     *
     * @return
     */
    public abstract String getDisplayName();

    public final boolean renderItemIn3D() {
        return renderIn3D;
    }

    /**
     * If set to true, then the model will be used to render the items.
     *
     * If you wish to provide an IIcon instead, you must set it to false, and
     * implement getItemIcons().
     *
     * @param renderIn3D
     */
    public final void setRenderItemIn3D(boolean renderIn3D) {
        this.renderIn3D = renderIn3D;
    }

    /**
     * If you return false to renderItemIn3D(), then you must return up to three
     * icons in an array here, one for each render pass.
     *
     * Passes:
     * <ul>
     * <li>0 = primary color
     * <li>1 = secondary color
     * <li>2 = white
     * </ul>
     *
     * @return icons
     */
    public IIcon[] getItemIcons() {
        return null;
    }

    /**
     * Register any item textures you require here.
     *
     * @param iconRegister
     */
    public void registerItemIcons(IIconRegister iconRegister) {
    }

    /**
     * This is the core of the renderer. Here is where you do the actual
     * rendering. You can implement any kind of renderer you like, model based,
     * obj based, whatever.
     *
     * Your renderer should do a three pass render: primary color, secondary
     * color, and white. Use GL11.glColor4f() to change the color for each pass.
     *
     * It is also responsible for rendering the emblem texture onto the
     * Locomotive.
     *
     * You do not need to worry about rotation or anything like that, that is
     * taken care of already by Railcraft.
     *
     * @param renderer
     * @param cart
     * @param primaryColor the primary color
     * @param secondaryColor the secondary color
     * @param emblemTexture the emblem texture to render
     * @param light
     * @param time
     */
    public abstract void renderLocomotive(IRenderer renderer, EntityMinecart cart, int primaryColor, int secondaryColor, ResourceLocation emblemTexture, float light, float time);

}
