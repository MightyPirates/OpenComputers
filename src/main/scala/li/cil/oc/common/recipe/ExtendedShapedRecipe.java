package li.cil.oc.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class ExtendedShapedRecipe implements ICraftingRecipe, IShapedRecipe<CraftingInventory> {
    private ShapedRecipe wrapped;

    public ExtendedShapedRecipe(ShapedRecipe wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        return wrapped.matches(inv, world);
    }

    @Override
    public ItemStack assemble(CraftingInventory inv) {
        return ExtendedRecipe.addNBTToResult(this, wrapped.assemble(inv), inv);
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return wrapped.canCraftInDimensions(w, h);
    }

    @Override
    public ItemStack getResultItem() {
        return wrapped.getResultItem();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
        return wrapped.getRemainingItems(inv);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return wrapped.getIngredients();
    }

    @Override
    public ResourceLocation getId() {
        return wrapped.getId();
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return RecipeSerializers.CRAFTING_SHAPED_EXTENDED;
    }

    @Override
    public String getGroup() {
        return wrapped.getGroup();
    }

    @Override
    public int getRecipeWidth() {
        return wrapped.getRecipeWidth();
    }

    @Override
    public int getRecipeHeight() {
        return wrapped.getRecipeHeight();
    }

    public static final class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
        implements IRecipeSerializer<ExtendedShapedRecipe> {

        @Override
        public ExtendedShapedRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            ShapedRecipe wrapped = IRecipeSerializer.SHAPED_RECIPE.fromJson(recipeId, json);
            return new ExtendedShapedRecipe(wrapped);
        }

        @Override
        public ExtendedShapedRecipe fromNetwork(ResourceLocation recipeId, PacketBuffer buff) {
            ShapedRecipe wrapped = IRecipeSerializer.SHAPED_RECIPE.fromNetwork(recipeId, buff);
            return new ExtendedShapedRecipe(wrapped);
        }

        @Override
        public void toNetwork(PacketBuffer buff, ExtendedShapedRecipe recipe) {
            IRecipeSerializer<ShapedRecipe> serializer =
                (IRecipeSerializer<ShapedRecipe>) recipe.wrapped.getSerializer();
            serializer.toNetwork(buff, recipe.wrapped);
        }
    }
}
