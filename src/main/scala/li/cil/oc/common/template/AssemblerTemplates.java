package li.cil.oc.common.template;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.util.Reflection;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class AssemblerTemplates {
    public static final SlotInfo NoSlot = new SlotInfo(Slot.None(), Tier.None(), null, null);

    private static final List<Template> templates = new ArrayList<>();
    private static final List<ItemStackFilter> templateFilters = new ArrayList<>();

    public static void add(final NBTTagCompound template) {
        final ItemStackFilter selector = Reflection.getStaticMethod(template.getString("select"), ItemStackFilter.class);
        final TemplateValidator validator = Reflection.getStaticMethod(template.getString("validate"), TemplateValidator.class);
        final TemplateAssembler assembler = Reflection.getStaticMethod(template.getString("assemble"), TemplateAssembler.class);

        if (selector == null || validator == null || assembler == null) {
            return;
        }

        final Class<?> hostClass = Reflection.getClass(template.getString("hostClass"));
        final SlotInfo[] containerSlots = parseSlots(template.getTagList("containerSlots", NBT.TAG_COMPOUND), 3, Slot.Container(), hostClass);
        final SlotInfo[] upgradeSlots = parseSlots(template.getTagList("upgradeSlots", NBT.TAG_COMPOUND), 9, Slot.Upgrade(), hostClass);
        final SlotInfo[] componentSlots = parseSlots(template.getTagList("componentSlots", NBT.TAG_COMPOUND), 9, Slot.Any(), hostClass);

        templates.add(new Template(selector, validator, assembler, containerSlots, upgradeSlots, componentSlots));
    }

    public static void addFilter(final String method) {
        final ItemStackFilter filter = Reflection.getStaticMethod(method, ItemStackFilter.class);
        if (filter != null) {
            templateFilters.add(filter);
        }
    }

    @Nullable
    public static Template select(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        for (final ItemStackFilter filter : templateFilters) {
            if (!filter.select(stack)) {
                return null;
            }
        }

        for (final Template template : templates) {
            if (template.select(stack)) {
                return template;
            }
        }

        return null;
    }

    // ----------------------------------------------------------------------- //

    public static final class Template {
        private final ItemStackFilter selector;
        private final TemplateValidator validator;
        private final TemplateAssembler assembler;
        public final SlotInfo[] containerSlots;
        public final SlotInfo[] upgradeSlots;
        public final SlotInfo[] componentSlots;

        Template(final ItemStackFilter selector, final TemplateValidator validator, final TemplateAssembler assembler, final SlotInfo[] containerSlots, final SlotInfo[] upgradeSlots, final SlotInfo[] componentSlots) {
            this.selector = selector;
            this.validator = validator;
            this.assembler = assembler;
            this.containerSlots = containerSlots;
            this.upgradeSlots = upgradeSlots;
            this.componentSlots = componentSlots;
        }

        public boolean select(final ItemStack stack) {
            return selector.select(stack);
        }

        public ValidationResult validate(final IItemHandler inventory) {
            final Object[] result = validator.validate(inventory);
            boolean isValid = false;
            ITextComponent progress = null;
            ITextComponent[] warnings = new ITextComponent[0];
            if (result.length > 0 && result[0] instanceof Boolean) {
                isValid = (boolean) result[0];
            }
            if (result.length > 1 && result[1] instanceof ITextComponent) {
                progress = (ITextComponent) result[1];
            }
            if (result.length > 2 && result[2] instanceof ITextComponent[]) {
                warnings = (ITextComponent[]) result[2];
            }
            return new ValidationResult(isValid, progress, warnings);
        }

        public AssemblyResult assemble(final IItemHandler inventory) {
            final Object[] result = assembler.assemble(inventory);
            ItemStack output = ItemStack.EMPTY;
            double requiredEnergy = 0;
            if (result.length > 0 && result[0] instanceof ItemStack) {
                output = (ItemStack) result[0];
            }
            if (result.length > 1 && result[1] instanceof Double) {
                requiredEnergy = (double) result[1];
            }
            return new AssemblyResult(output, requiredEnergy);
        }
    }

    public static final class SlotInfo {
        private final String type;
        private final int tier;
        private final SlotValidator validator;
        private final Class<?> hostClass;

        SlotInfo(final String type, final int tier, @Nullable final SlotValidator validator, @Nullable final Class<?> hostClass) {
            this.type = type;
            this.tier = tier;
            this.validator = validator;
            this.hostClass = hostClass;
        }

        public boolean validate(final IItemHandler inventory, final int slot, final ItemStack stack) {
            if (validator != null) {
                return validator.validate(inventory, slot, tier, stack);
            } else {
                final DriverItem driver;
                if (hostClass != null) {
                    driver = Driver.driverFor(stack, hostClass);
                } else {
                    driver = Driver.driverFor(stack);
                }

                return driver != null && Objects.equals(driver.slot(stack), type) && driver.tier(stack) <= tier;
            }
        }
    }

    public static final class ValidationResult {
        private final boolean isValid;
        private final ITextComponent progress;
        private final ITextComponent[] warnings;

        ValidationResult(final boolean isValid, @Nullable final ITextComponent progress, final ITextComponent[] warnings) {
            this.isValid = isValid;
            this.progress = progress;
            this.warnings = warnings;
        }

        public boolean isValid() {
            return isValid;
        }

        @Nullable
        public ITextComponent getProgress() {
            return progress;
        }

        public ITextComponent[] getWarnings() {
            return warnings;
        }
    }

    public static final class AssemblyResult {
        private final ItemStack output;
        private final double requiredEnergy;

        AssemblyResult(final ItemStack output, final double requiredEnergy) {
            this.output = output;
            this.requiredEnergy = requiredEnergy;
        }

        public ItemStack getOutput() {
            return output;
        }

        public double getRequiredEnergy() {
            return requiredEnergy;
        }
    }

    // ----------------------------------------------------------------------- //

    private static SlotInfo[] parseSlots(final NBTTagList slotsNbt, final int slotCount, @Nullable final String slotType, @Nullable final Class<?> hostClass) {
        final SlotInfo[] result = new SlotInfo[slotCount];
        Arrays.fill(result, NoSlot);
        for (int slot = 0; slot < Math.min(slotCount, slotsNbt.tagCount()); slot++) {
            final NBTTagCompound slotNbt = slotsNbt.getCompoundTagAt(slot);
            parseSlot(slotNbt, slotType, hostClass);
        }
        return result;
    }

    private static SlotInfo parseSlot(final NBTTagCompound slotNbt, @Nullable final String slotType, @Nullable final Class<?> hostClass) {
        final String type = slotType != null ? slotType : slotNbt.hasKey("type", NBT.TAG_STRING) ? slotNbt.getString("type") : Slot.None();
        final int tier = slotNbt.hasKey("tier", NBT.TAG_INT) ? slotNbt.getInteger("tier") : Tier.Any();
        final SlotValidator validator = slotNbt.hasKey("validate", NBT.TAG_STRING) ? Reflection.getStaticMethod(slotNbt.getString("validate"), SlotValidator.class) : null;
        return new SlotInfo(type, tier, validator, hostClass);
    }

    // ----------------------------------------------------------------------- //

    @FunctionalInterface
    private interface ItemStackFilter {
        boolean select(final ItemStack stack);
    }

    @FunctionalInterface
    private interface SlotValidator {
        boolean validate(final IItemHandler inventory, final int slot, final int tier, final ItemStack stack);
    }

    @FunctionalInterface
    private interface TemplateValidator {
        Object[] validate(final IItemHandler inventory);
    }

    @FunctionalInterface
    private interface TemplateAssembler {
        Object[] assemble(final IItemHandler inventory);
    }
}
