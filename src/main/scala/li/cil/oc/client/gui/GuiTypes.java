package li.cil.oc.client.gui;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.container.ContainerTypes;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = {Dist.CLIENT}, modid = "opencomputers", bus = Bus.MOD)
public final class GuiTypes {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent e) {
        // ScreenManager.register is not thread-safe.
        e.enqueueWork(() -> {
            ScreenManager.register(ContainerTypes.ADAPTER, Adapter::new);
            ScreenManager.register(ContainerTypes.ASSEMBLER, Assembler::new);
            ScreenManager.register(ContainerTypes.CASE, Case::new);
            ScreenManager.register(ContainerTypes.CHARGER, Charger::new);
            ScreenManager.register(ContainerTypes.DATABASE, Database::new);
            ScreenManager.register(ContainerTypes.DISASSEMBLER, Disassembler::new);
            ScreenManager.register(ContainerTypes.DISK_DRIVE, DiskDrive::new);
            ScreenManager.register(ContainerTypes.DRONE, Drone::new);
            ScreenManager.register(ContainerTypes.PRINTER, Printer::new);
            ScreenManager.register(ContainerTypes.RACK, Rack::new);
            ScreenManager.register(ContainerTypes.RAID, Raid::new);
            ScreenManager.register(ContainerTypes.RELAY, Relay::new);
            ScreenManager.register(ContainerTypes.ROBOT, Robot::new);
            ScreenManager.register(ContainerTypes.SERVER, Server::new);
            ScreenManager.register(ContainerTypes.TABLET, Tablet::new);
        });
    }

    private GuiTypes() {
        throw new Error();
    }
}
