package li.cil.oc.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/**
 * @author asie, Vexatos, Sangar
 */
public final class ItemColorizer {
    /**
     * Return whether the specified armor ItemStack has a color.
     */
    public static boolean hasColor(final ItemStack stack) {
        final NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null &&
                nbt.hasKey(DISPLAY_TAG, Constants.NBT.TAG_COMPOUND) &&
                nbt.getCompoundTag(DISPLAY_TAG).hasKey(COLOR_TAG, Constants.NBT.TAG_INT);
    }

    /**
     * Return the color for the specified armor ItemStack.
     */
    public static int getColor(final ItemStack stack) {
        if (hasColor(stack)) {
            final NBTTagCompound nbt = stack.getTagCompound();
            assert nbt != null : "hasColor lied.";
            return nbt.getCompoundTag(DISPLAY_TAG).getInteger(COLOR_TAG);
        }
        return -1;
    }

    public static void removeColor(final ItemStack stack) {
        if (hasColor(stack)) {
            final NBTTagCompound nbt = stack.getTagCompound();
            assert nbt != null : "hasColor lied.";
            nbt.getCompoundTag(DISPLAY_TAG).removeTag(COLOR_TAG);
        }
    }

    public static void setColor(final ItemStack stack, final int color) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (!tag.hasKey(DISPLAY_TAG, Constants.NBT.TAG_COMPOUND)) {
            tag.setTag(DISPLAY_TAG, new NBTTagCompound());
        }
        tag.getCompoundTag(DISPLAY_TAG).setInteger(COLOR_TAG, color);
    }

    // ----------------------------------------------------------------------- //

    private static final String DISPLAY_TAG = "display";
    private static final String COLOR_TAG = "color";

    // ----------------------------------------------------------------------- //

    private ItemColorizer() {
    }
}
