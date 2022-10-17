package li.cil.oc.common.recipe;

import li.cil.oc.OpenComputers;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.ObjectHolder;

@Mod.EventBusSubscriber(modid = "opencomputers", bus = Bus.MOD)
@ObjectHolder("opencomputers")
public class RecipeSerializers {
    public static final IRecipeSerializer<?> CRAFTING_LOOTDISK_CYCLING = null;

    @SubscribeEvent
    public static void registerSerializers(RegistryEvent.Register<IRecipeSerializer<?>> e) {
        register(e.getRegistry(), "crafting_lootdisk_cycling", new SpecialRecipeSerializer<>(LootDiskCyclingRecipe::new));
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
