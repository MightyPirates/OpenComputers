package li.cil.oc.integration.util;

import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class ItemCharge {
    @FunctionalInterface
    public interface ItemChargeValidator {
        boolean canCharge(final ItemStack stack);
    }

    @FunctionalInterface
    public interface ItemCharger {
        double changeEnergy(final ItemStack stack, final double delta);
    }

    private static final List<ItemChargeInfo> chargeInfo = new ArrayList<>();

    public static void add(@Nullable final ItemChargeValidator validator, @Nullable final ItemCharger charger) {
        if (validator == null || charger == null) {
            return;
        }

        final ItemChargeInfo info = new ItemChargeInfo(validator, charger);
        if (chargeInfo.contains(info)) {
            return;
        }

        chargeInfo.add(info);
    }

    public static boolean canCharge(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        for (final ItemChargeInfo info : chargeInfo) {
            if (info.validator.canCharge(stack)) {
                return true;
            }
        }

        return false;
    }

    public static double charge(final ItemStack stack, final double delta) {
        if (stack.isEmpty()) {
            return 0;
        }

        for (final ItemChargeInfo info : chargeInfo) {
            if (info.validator.canCharge(stack)) {
                return info.charger.changeEnergy(stack, delta);
            }
        }

        return 0;
    }

    private static final class ItemChargeInfo {
        final ItemChargeValidator validator;
        final ItemCharger charger;

        private ItemChargeInfo(final ItemChargeValidator validator, final ItemCharger charger) {
            this.validator = validator;
            this.charger = charger;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ItemChargeInfo that = (ItemChargeInfo) o;

            return validator.equals(that.validator) && charger.equals(that.charger);
        }

        @Override
        public int hashCode() {
            int result = validator.hashCode();
            result = 31 * result + charger.hashCode();
            return result;
        }
    }

//  def canCharge(stack: ItemStack): Boolean = stack != null && chargers.exists(charger => IMC.tryInvokeStatic(charger._1, stack)(false))
//
//  def charge(stack: ItemStack, amount: Double): Double = {
//    if (stack != null) chargers.find(charger => IMC.tryInvokeStatic(charger._1, stack)(false)) match {
//      case Some(charger) => IMC.tryInvokeStatic(charger._2, stack, Double.box(amount), java.lang.Boolean.FALSE)(0.0)
//      case _ => 0.0
//    }
//    else 0.0
//  }
}
