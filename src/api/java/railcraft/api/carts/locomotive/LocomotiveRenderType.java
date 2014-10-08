/*
 * Copyright (c) CovertJaguar, 2011 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at railcraft.wikispaces.com.
 */
package mods.railcraft.api.carts.locomotive;

import cpw.mods.fml.common.registry.GameRegistry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * This class is used to register new Locomotive Skins with Railcraft.
 *
 * Usage example: LocomotiveRenderType.STEAM_SOLID.registerRenderer(new
 * MyRenderer());
 * 
 * Registration must be done in the Client side initialization.
 *
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public enum LocomotiveRenderType {

    STEAM_SOLID("cart.loco.steam.solid"),
    STEAM_MAGIC("cart.loco.steam.magic"),
    ELECTRIC("cart.loco.electric");
    private final Map<String, LocomotiveModelRenderer> renderers = new HashMap<String, LocomotiveModelRenderer>();
    private final String cartTag;

    private LocomotiveRenderType(String cartTag) {
        this.cartTag = cartTag;
    }

    /**
     * This is how you register a new renderer. It can be a model renderer, an
     * obj renderer, or anything else you want. It just needs to extend
     * LocomotiveModelRenderer.
     *
     * @param renderer
     */
    public void registerRenderer(LocomotiveModelRenderer renderer) {
        renderers.put(renderer.getRendererTag(), renderer);
    }

    /**
     * Railcraft calls this method, you don't need to worry about it.
     *
     * @param iconRegister
     */
    public void registerIcons(IIconRegister iconRegister) {
        Set<LocomotiveModelRenderer> set = new HashSet<LocomotiveModelRenderer>(renderers.values());
        for (LocomotiveModelRenderer renderer : set) {
            renderer.registerItemIcons(iconRegister);
        }
    }

    /**
     * Railcraft calls this method, you don't need to worry about it.
     *
     * @param tag
     * @return
     */
    public LocomotiveModelRenderer getRenderer(String tag) {
        LocomotiveModelRenderer renderer = renderers.get(tag);
        if (renderer == null)
            renderer = renderers.get("railcraft:default");
        return renderer;
    }

    /**
     * This function will return a Locomotive item with the skin identifier
     * saved in the NBT. Use it to create a recipe for your skin.
     *
     * @param rendererTag
     * @return
     */
    public ItemStack getItemWithRenderer(String rendererTag) {
        ItemStack stack = GameRegistry.findItemStack("Railcraft", cartTag, 1);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("model", rendererTag);
        stack.setTagCompound(nbt);
        return stack;
    }

    /**
     * Railcraft calls this method, you don't need to worry about it.
     *
     * @return
     */
    public Set<String> getRendererTags() {
        return renderers.keySet();
    }

}
