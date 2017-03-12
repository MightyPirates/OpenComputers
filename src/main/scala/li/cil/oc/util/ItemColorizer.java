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
                nbt.hasKey(TAG_DISPLAY, Constants.NBT.TAG_COMPOUND) &&
                nbt.getCompoundTag(TAG_DISPLAY).hasKey(TAG_COLOR, Constants.NBT.TAG_INT);
    }

    /**
     * Return the color for the specified armor ItemStack.
     */
    public static int getColor(final ItemStack stack) {
        if (hasColor(stack)) {
            final NBTTagCompound nbt = stack.getTagCompound();
            assert nbt != null : "hasColor lied.";
            return nbt.getCompoundTag(TAG_DISPLAY).getInteger(TAG_COLOR);
        }
        return -1;
    }

    public static void removeColor(final ItemStack stack) {
        if (hasColor(stack)) {
            final NBTTagCompound nbt = stack.getTagCompound();
            assert nbt != null : "hasColor lied.";
            nbt.getCompoundTag(TAG_DISPLAY).removeTag(TAG_COLOR);
        }
    }

    public static void setColor(final ItemStack stack, final int color) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (!tag.hasKey(TAG_DISPLAY, Constants.NBT.TAG_COMPOUND)) {
            tag.setTag(TAG_DISPLAY, new NBTTagCompound());
        }
        tag.getCompoundTag(TAG_DISPLAY).setInteger(TAG_COLOR, color);
    }

    // ----------------------------------------------------------------------- //

    private static final String TAG_DISPLAY = "display";
    private static final String TAG_COLOR = "color";

    // ----------------------------------------------------------------------- //

    private ItemColorizer() {
    }
}
