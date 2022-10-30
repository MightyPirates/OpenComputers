package li.cil.oc.server.loot;

import li.cil.oc.OpenComputers;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootFunctionType;
import net.minecraft.loot.functions.ILootFunction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

public final class LootFunctions {
    public static final ResourceLocation DYN_ITEM_DATA = new ResourceLocation(OpenComputers.ID(), "item_data");
    public static final ResourceLocation DYN_VOLATILE_CONTENTS = new ResourceLocation(OpenComputers.ID(), "volatile_contents");

    public static final LootFunctionType SET_COLOR = register("set_color", new SetColor.Serializer());
    public static final LootFunctionType COPY_COLOR = register("copy_color", new CopyColor.Serializer());

    private static LootFunctionType register(String name, ILootSerializer<? extends ILootFunction> serializer) {
        LootFunctionType type = new LootFunctionType(serializer);
        Registry.register(Registry.LOOT_FUNCTION_TYPE, new ResourceLocation(OpenComputers.ID(), name), type);
        return type;
    }

    public static final void init() {
        // No registry events or ObjectHolder - this is to load the class.
    }

    private LootFunctions() {
    }
}
