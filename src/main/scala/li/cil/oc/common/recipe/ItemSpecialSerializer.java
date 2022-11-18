package li.cil.oc.common.recipe;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class ItemSpecialSerializer<T extends IRecipe<?>> extends ForgeRegistryEntry<IRecipeSerializer<?>>
    implements IRecipeSerializer<T> {

    private BiFunction<ResourceLocation, IItemProvider, T> ctor;
    private Function<T, Item> getter;

    public ItemSpecialSerializer(BiFunction<ResourceLocation, IItemProvider, T> ctor, Function<T, Item> getter) {
        this.ctor = ctor;
        this.getter = getter;
    }

    @Override
    public T fromJson(ResourceLocation recipeId, JsonObject json) {
        ResourceLocation loc = new ResourceLocation(JSONUtils.getAsString(json, "item"));
        if (!ForgeRegistries.ITEMS.containsKey(loc)) {
            throw new JsonSyntaxException("Unknown item '" + loc + "'");
        }
        return ctor.apply(recipeId, ForgeRegistries.ITEMS.getValue(loc));
    }

    @Override
    public T fromNetwork(ResourceLocation recipeId, PacketBuffer buff) {
        return ctor.apply(recipeId, buff.readRegistryIdUnsafe(ForgeRegistries.ITEMS));
    }

    @Override
    public void toNetwork(PacketBuffer buff, T recipe) {
        buff.writeRegistryIdUnsafe(ForgeRegistries.ITEMS, getter.apply(recipe));
    }
}
