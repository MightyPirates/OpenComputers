package li.cil.oc.common.container;

import li.cil.oc.OpenComputers;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

@Mod.EventBusSubscriber(modid = "opencomputers", bus = Bus.MOD)
@ObjectHolder("opencomputers")
public final class ContainerTypes {
    public static final ContainerType<Adapter> ADAPTER = null;
    public static final ContainerType<Assembler> ASSEMBLER = null;
    public static final ContainerType<Case> CASE = null;
    public static final ContainerType<Charger> CHARGER = null;
    public static final ContainerType<Database> DATABASE = null;
    public static final ContainerType<Disassembler> DISASSEMBLER = null;
    public static final ContainerType<DiskDrive> DISK_DRIVE = null;
    public static final ContainerType<Drone> DRONE = null;
    public static final ContainerType<Printer> PRINTER = null;
    public static final ContainerType<Rack> RACK = null;
    public static final ContainerType<Raid> RAID = null;
    public static final ContainerType<Relay> RELAY = null;
    public static final ContainerType<Robot> ROBOT = null;
    public static final ContainerType<Server> SERVER = null;
    public static final ContainerType<Tablet> TABLET = null;

    @SubscribeEvent
    public static void registerContainers(RegistryEvent.Register<ContainerType<?>> e) {
        register(e.getRegistry(), "adapter", (id, plr, buff) -> new Adapter(ADAPTER, id, plr, new Inventory(1)));
        register(e.getRegistry(), "assembler", (id, plr, buff) -> new Assembler(ASSEMBLER, id, plr, new Inventory(22)));
        register(e.getRegistry(), "case", (id, plr, buff) -> {
            int invSize = buff.readVarInt();
            int tier = buff.readVarInt();
            return new Case(CASE, id, plr, new Inventory(invSize), tier);
        });
        register(e.getRegistry(), "charger", (id, plr, buff) -> new Charger(CHARGER, id, plr, new Inventory(1)));
        register(e.getRegistry(), "database", (id, plr, buff) -> {
            ItemStack containerStack = buff.readItem();
            int invSize = buff.readVarInt();
            int tier = buff.readVarInt();
            return new Database(DATABASE, id, plr, containerStack, new Inventory(invSize), tier);
        });
        register(e.getRegistry(), "disassembler", (id, plr, buff) -> new Disassembler(DISASSEMBLER, id, plr, new Inventory(1)));
        register(e.getRegistry(), "disk_drive", (id, plr, buff) -> new DiskDrive(DISK_DRIVE, id, plr, new Inventory(1)));
        register(e.getRegistry(), "drone", (id, plr, buff) -> {
            int invSize = buff.readVarInt();
            return new Drone(DRONE, id, plr, new Inventory(invSize));
        });
        register(e.getRegistry(), "printer", (id, plr, buff) -> new Printer(PRINTER, id, plr, new Inventory(3)));
        register(e.getRegistry(), "rack", (id, plr, buff) -> new Rack(RACK, id, plr, new Inventory(4)));
        register(e.getRegistry(), "raid", (id, plr, buff) -> new Raid(RAID, id, plr, new Inventory(3)));
        register(e.getRegistry(), "relay", (id, plr, buff) -> new Relay(RELAY, id, plr, new Inventory(4)));
        register(e.getRegistry(), "robot", (id, plr, buff) -> {
            RobotInfo info = RobotInfo$.MODULE$.readRobotInfo(buff);
            return new Robot(ROBOT, id, plr, new Inventory(100), info);
        });
        register(e.getRegistry(), "server", (id, plr, buff) -> {
            ItemStack containerStack = buff.readItem();
            int invSize = buff.readVarInt();
            int tier = buff.readVarInt();
            int rackSlot = buff.readVarInt() - 1;
            return new Server(SERVER, id, plr, containerStack, new Inventory(invSize), tier, rackSlot);
        });
        register(e.getRegistry(), "tablet", (id, plr, buff) -> {
            ItemStack containerStack = buff.readItem();
            int invSize = buff.readVarInt();
            String slot1 = buff.readUtf(32);
            int tier1 = buff.readVarInt();
            return new Tablet(TABLET, id, plr, containerStack, new Inventory(invSize), slot1, tier1);
        });
    }

    private static void register(IForgeRegistry<ContainerType<?>> registry, String name, IContainerFactory<?> factory) {
        ContainerType<?> type = IForgeContainerType.create(factory);
        type.setRegistryName(new ResourceLocation(OpenComputers.ID(), name));
        registry.register(type);
    }

    public static void openAdapterGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.Adapter adapter) {
        NetworkHooks.openGui(player, adapter);
    }

    public static void openAssemblerGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.Assembler assembler) {
        NetworkHooks.openGui(player, assembler);
    }

    public static void openCaseGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.Case computer) {
        NetworkHooks.openGui(player, computer, buff -> {
            buff.writeVarInt(computer.getContainerSize());
            buff.writeVarInt(computer.tier());
        });
    }

    public static void openChargerGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.Charger charger) {
        NetworkHooks.openGui(player, charger);
    }

    public static void openDatabaseGui(ServerPlayerEntity player, li.cil.oc.common.inventory.DatabaseInventory database) {
        NetworkHooks.openGui(player, database, buff -> {
            buff.writeItem(database.container());
            buff.writeVarInt(database.getContainerSize());
            buff.writeVarInt(database.tier());
        });
    }

    public static void openDisassemblerGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.Disassembler disassembler) {
        NetworkHooks.openGui(player, disassembler);
    }

    public static void openDiskDriveGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.DiskDrive diskDrive) {
        NetworkHooks.openGui(player, diskDrive);
    }

    public static void openDiskDriveGui(ServerPlayerEntity player, li.cil.oc.server.component.DiskDriveMountable diskDrive) {
        NetworkHooks.openGui(player, diskDrive);
    }

    public static void openDiskDriveGui(ServerPlayerEntity player, li.cil.oc.common.inventory.DiskDriveMountableInventory diskDrive) {
        NetworkHooks.openGui(player, diskDrive);
    }

    public static void openDroneGui(ServerPlayerEntity player, li.cil.oc.common.entity.Drone drone) {
        NetworkHooks.openGui(player, drone.containerProvider(), buff -> {
            buff.writeVarInt(drone.mainInventory().getContainerSize());
        });
    }

    public static void openPrinterGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.Printer printer) {
        NetworkHooks.openGui(player, printer);
    }

    public static void openRackGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.Rack rack) {
        NetworkHooks.openGui(player, rack);
    }

    public static void openRaidGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.Raid raid) {
        NetworkHooks.openGui(player, raid);
    }

    public static void openRelayGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.Relay relay) {
        NetworkHooks.openGui(player, relay);
    }

    public static void openRobotGui(ServerPlayerEntity player, li.cil.oc.common.tileentity.Robot robot) {
        NetworkHooks.openGui(player, robot, buff -> {
            RobotInfo$.MODULE$.writeRobotInfo(buff, new RobotInfo(robot));
        });
    }

    public static void openServerGui(ServerPlayerEntity player, li.cil.oc.common.inventory.ServerInventory server, int rackSlot) {
        NetworkHooks.openGui(player, server, buff -> {
            buff.writeItem(server.container());
            buff.writeVarInt(server.getContainerSize());
            buff.writeVarInt(server.tier());
            buff.writeVarInt(rackSlot + 1);
        });
    }

    public static void openTabletGui(ServerPlayerEntity player, li.cil.oc.common.item.TabletWrapper tablet) {
        NetworkHooks.openGui(player, tablet, buff -> {
            buff.writeItem(tablet.stack());
            buff.writeVarInt(tablet.getContainerSize());
            buff.writeUtf(tablet.containerSlotType(), 32);
            buff.writeVarInt(tablet.containerSlotTier());
        });
    }

    private ContainerTypes() {
        throw new Error();
    }
}
