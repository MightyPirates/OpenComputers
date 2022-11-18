package li.cil.oc.common.recipe;

import li.cil.oc.OpenComputers;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder("opencomputers")
public class RecipeSerializers {
    public static final IRecipeSerializer<?> CRAFTING_LOOTDISK_CYCLING = null;
    public static final IRecipeSerializer<?> CRAFTING_COLORIZE = null;
    public static final IRecipeSerializer<?> CRAFTING_DECOLORIZE = null;
    public static final IRecipeSerializer<?> CRAFTING_SHAPED_EXTENDED = null;
    public static final IRecipeSerializer<?> CRAFTING_SHAPELESS_EXTENDED = null;

    @SubscribeEvent
    public static void registerSerializers(RegistryEvent.Register<IRecipeSerializer<?>> e) {
        register(e.getRegistry(), "crafting_lootdisk_cycling", new SpecialRecipeSerializer<>(LootDiskCyclingRecipe::new));
        register(e.getRegistry(), "crafting_colorize", new ItemSpecialSerializer<>(ColorizeRecipe::new, ColorizeRecipe::targetItem));
        register(e.getRegistry(), "crafting_decolorize", new ItemSpecialSerializer<>(DecolorizeRecipe::new, DecolorizeRecipe::targetItem));
        register(e.getRegistry(), "crafting_shaped_extended", new ExtendedShapedRecipe.Serializer());
        register(e.getRegistry(), "crafting_shapeless_extended", new ExtendedShapelessRecipe.Serializer());
    }

    private static <S extends IForgeRegistryEntry<IRecipeSerializer<?>> & IRecipeSerializer<?>>
        void register(IForgeRegistry<IRecipeSerializer<?>> registry, String name, S serializer) {

        serializer.setRegistryName(new ResourceLocation(OpenComputers.ID(), name));
        registry.register(serializer);
    }

    private RecipeSerializers() {
        throw new Error();
    }
}
