package li.cil.oc.server.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootFunction;
import net.minecraft.loot.LootFunctionType;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.functions.ILootFunction;
import net.minecraft.loot.conditions.ILootCondition;

public final class CopyColor extends LootFunction {
    private CopyColor(ILootCondition[] conditions) {
        super(conditions);
    }

    @Override
    public LootFunctionType getType() {
        return LootFunctions.COPY_COLOR;
    }

    public static class Builder extends LootFunction.Builder<Builder> {
        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public ILootFunction build() {
            return new CopyColor(getConditions());
        }
    }

    public static Builder copyColor() {
        return new Builder();
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext ctx) {
        if (stack.isEmpty()) return stack;
        TileEntity te = ctx.getParamOrNull(LootParameters.BLOCK_ENTITY);
        if (te != null && te instanceof Colored) {
            // Can't use capability because it's already invalid - block breaks before drops are calculated.
            ItemColorizer.setColor(stack, ((Colored) te).getColor());
        }
        else ItemColorizer.removeColor(stack);
        return stack;
    }

    public static class Serializer extends LootFunction.Serializer<CopyColor> {
        @Override
        public CopyColor deserialize(JsonObject src, JsonDeserializationContext ctx, ILootCondition[] conditions) {
            return new CopyColor(conditions);
        }
    }
}
