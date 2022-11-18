package li.cil.oc.server.loot;

import java.util.OptionalInt;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootFunction;
import net.minecraft.loot.LootFunctionType;
import net.minecraft.loot.functions.ILootFunction;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.JSONUtils;

public final class SetColor extends LootFunction {
    private OptionalInt color;

    private SetColor(ILootCondition[] conditions, OptionalInt color) {
        super(conditions);
        this.color = color;
    }

    @Override
    public LootFunctionType getType() {
        return LootFunctions.SET_COLOR;
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext ctx) {
        if (stack.isEmpty()) return stack;
        if (color.isPresent()) {
            ItemColorizer.setColor(stack, color.getAsInt());
        }
        else ItemColorizer.removeColor(stack);
        return stack;
    }

    public static class Builder extends LootFunction.Builder<Builder> {
        private OptionalInt color = OptionalInt.empty();

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder withoutColor() {
            color = OptionalInt.empty();
            return this;
        }

        public Builder withColor(int color) {
            if (color < 0 || color > 0xFFFFFF) throw new IllegalArgumentException("Invalid RGB color: " + color);
            this.color = OptionalInt.of(color);
            return this;
        }

        @Override
        public ILootFunction build() {
            return new SetColor(getConditions(), color);
        }
    }

    public static Builder setColor() {
        return new Builder();
    }

    public static class Serializer extends LootFunction.Serializer<SetColor> {
        @Override
        public void serialize(JsonObject dst, SetColor src, JsonSerializationContext ctx) {
            super.serialize(dst, src, ctx);
            src.color.ifPresent(v -> dst.add("color", new JsonPrimitive(v)));
        }

        @Override
        public SetColor deserialize(JsonObject src, JsonDeserializationContext ctx, ILootCondition[] conditions) {
            if (src.has("color")) {
                int color = JSONUtils.getAsInt(src, "color");
                if (color < 0 || color > 0xFFFFFF) throw new JsonParseException("Invalid RGB color: " + color);
                return new SetColor(conditions, OptionalInt.of(color));
            }
            else return new SetColor(conditions, OptionalInt.empty());
        }
    }
}
