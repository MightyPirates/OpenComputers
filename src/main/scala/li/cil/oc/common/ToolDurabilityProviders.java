package li.cil.oc.common;

import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class ToolDurabilityProviders {
    @FunctionalInterface
    public interface ToolDurabilityProvider {
        double getDurability(final ItemStack stack);
    }

    private static final List<ToolDurabilityProvider> providers = new ArrayList<>();

    public static void add(@Nullable final ToolDurabilityProvider provider) {
        if (provider == null) {
            return;
        }

        if (!providers.contains(provider)) {
            providers.add(provider);
        }
    }

    public static double getDurability(final ItemStack stack) {
        if (stack.isEmpty()) {
            return Double.NaN;
        }

        for (final ToolDurabilityProvider provider : providers) {
            final double durability = provider.getDurability(stack);
            if (!Double.isNaN(durability)) {
                return durability;
            }
        }

        if (stack.isItemStackDamageable()) {
            return 1 - stack.getItemDamage() / (double) stack.getMaxDamage();
        }

        return Double.NaN;
    }
}
