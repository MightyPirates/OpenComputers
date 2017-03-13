package li.cil.oc.integration.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class Wrench {
    @FunctionalInterface
    public interface WrenchValidator {
        boolean isWrench(final ItemStack stack);
    }

    @FunctionalInterface
    public interface WrenchConsumer {
        boolean useWrench(final ItemStack stack, final BlockPos pos, final boolean simulate);
    }

    private static final List<WrenchValidator> validators = new ArrayList<>();
    private static final List<WrenchConsumer> consumers = new ArrayList<>();

    public static void addUsage(@Nullable final WrenchConsumer value) {
        if (value == null) {
            return;
        }

        if (!consumers.contains(value)) {
            consumers.add(value);
        }
    }

    public static void addValidator(@Nullable final WrenchValidator value) {
        if (value == null) {
            return;
        }

        if (!validators.contains(value)) {
            validators.add(value);
        }
    }

    public static boolean isWrench(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        for (final WrenchValidator validator : validators) {
            if (validator.isWrench(stack)) {
                return true;
            }
        }

        return false;
    }

    public static boolean holdsApplicableWrench(final EntityPlayer player, final EnumHand hand, final BlockPos pos) {
        final ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem.isEmpty()) {
            return false;
        }

        for (final WrenchConsumer consumer : consumers) {
            if (consumer.useWrench(heldItem, pos, true)) {
                return true;
            }
        }

        return false;
    }

    public static void wrenchUsed(final EntityPlayer player, final EnumHand hand, final BlockPos pos) {
        final ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem.isEmpty()) {
            return;
        }

        for (final WrenchConsumer consumer : consumers) {
            consumer.useWrench(heldItem, pos, false);
        }
    }
}
