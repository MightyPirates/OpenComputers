package li.cil.oc.api.event;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.internal.Rack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Fired to allow rendering a custom overlay for {@link li.cil.oc.api.component.RackMountable}s.
 * </p>
 * When this event is fired, the GL state is set up such that the origin is
 * the top left corner of the mountable the event was fired for. It's the
 * event handler's responsibility to not render outside the are of the
 * mountable (unless that's explicitly what they're going for, of course).
 */
public abstract class RackMountableRenderEvent extends Event {
    /**
     * The rack that house the mountable this event is fired for.
     */
    public final Rack rack;

    /**
     * The index of the mountable in the rack the event is fired for.
     */
    public final int mountable;

    /**
     * Some additional data made available by the mountable. May be <tt>null</tt>.
     *
     * @see RackMountable#getData()
     */
    public final NBTTagCompound data;

    public RackMountableRenderEvent(Rack rack, int mountable, NBTTagCompound data) {
        this.rack = rack;
        this.mountable = mountable;
        this.data = data;
    }

    /**
     * Fired when the static rack model is rendered.
     * <p/>
     * Code here runs inside a <tt>ISimpleBlockRenderingHandler</tt>, so functionality
     * is limited to what's possible in there. This is primarily meant to allow setting
     * a custom override texture (<tt>renderer.setOverrideBlockTexture</tt>) for the
     * mountables front.
     * <p/>
     * The bounds will be set up before this call, so you may adjust those, if you wish.
     */
    @Cancelable
    public static class Block extends RackMountableRenderEvent {
        /**
         * The front-facing side, i.e. where the mountable is visible on the rack.
         */
        public final ForgeDirection side;

        /**
         * The renderer used for rendering the block.
         */
        public final RenderBlocks renderer;

        /**
         * Texture to use for the front of the mountable.
         */
        private IIcon frontTextureOverride;

        public Block(final Rack rack, final int mountable, final NBTTagCompound data, final ForgeDirection side, final RenderBlocks renderer) {
            super(rack, mountable, data);
            this.side = side;
            this.renderer = renderer;
        }

        /**
         * The texture currently set to use for the front of the mountable, or <tt>null</tt>.
         */
        public IIcon getFrontTextureOverride() {
            return frontTextureOverride;
        }

        /**
         * Set the texture to use for the front of the mountable.
         *
         * @param texture the texture to use.
         */
        public void setFrontTextureOverride(final IIcon texture) {
            frontTextureOverride = texture;
        }
    }

    /**
     * Fired when the dynamic rack model is rendered.
     * <p/>
     * Code here runs inside a <tt>TileEntitySpecialRenderer</tt>, so go nuts. This is
     * primarily meant to allow rendering custom overlays, such as LEDs. The GL state
     * will have been adjusted such that rendering a one by one quad starting at the
     * origin will fill the full front face of the rack (i.e. rotation and translation
     * have already been applied).
     * <p/>
     * If you wish to have something glowing (like LEDs), you'll have to disable
     * lighting yourself (and enable it again afterwards!).
     * <p/>
     * Use the {@link #renderOverlay(ResourceLocation)} to render a slice from a
     * texture in the vertical area occupied by the mountable.
     */
    public static class TileEntity extends RackMountableRenderEvent {
        /**
         * The vertical low and high texture coordinates for the mountable's slot.
         * <p/>
         * This is purely for convenience; they're computed as <tt>(2/16)+i*(3/16)</tt>.
         */
        public final float v0, v1;

        public TileEntity(final Rack rack, final int mountable, final NBTTagCompound data, final float v0, final float v1) {
            super(rack, mountable, data);
            this.v0 = v0;
            this.v1 = v1;
        }

        /**
         * Utility method for rendering a texture as the front-side overlay.
         *
         * @param texture the texture to use to render the overlay.
         */
        public void renderOverlay(final ResourceLocation texture) {
            renderOverlay(texture, 0, 1);
        }

        /**
         * Utility method for rendering a texture as the front-side overlay
         * over a specified horizontal area.
         *
         * @param texture the texture to use to render the overlay.
         * @param u0      the lower end of the vertical area to render at.
         * @param u1      the upper end of the vertical area to render at.
         */
        public void renderOverlay(final ResourceLocation texture, final float u0, final float u1) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            final Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.addVertexWithUV(u0, v1, 0, u0, v1);
            t.addVertexWithUV(u1, v1, 0, u1, v1);
            t.addVertexWithUV(u1, v0, 0, u1, v0);
            t.addVertexWithUV(u0, v0, 0, u0, v0);
            t.draw();
        }
    }
}
