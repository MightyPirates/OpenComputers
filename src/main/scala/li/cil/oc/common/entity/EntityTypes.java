package li.cil.oc.common.entity;

import li.cil.oc.OpenComputers;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder("opencomputers")
public final class EntityTypes {
    public static final EntityType<Drone> DRONE = null;

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityType<?>> e) {
        register(e.getRegistry(), "drone", EntityType.Builder.of(Drone::new, EntityClassification.MISC)
            .sized(12 / 16f, 6 / 16f).fireImmune());
    }

    private static void register(IForgeRegistry<EntityType<?>> registry, String name, EntityType.Builder<?> builder) {
        EntityType<?> type = builder.build(name);
        type.setRegistryName(new ResourceLocation(OpenComputers.ID(), name));
        registry.register(type);
    }

    private EntityTypes() {
        throw new Error();
    }
}
