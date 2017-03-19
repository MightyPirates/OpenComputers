package li.cil.oc;

import li.cil.oc.api.internal.TextBuffer;

import java.util.regex.Pattern;

public final class Constants {
    public static final Pattern CIDR_PATTERN = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?:/(\\d{1,2}))");

    public static final String resourceDomain = "opencomputers";
    public static final String namespace = "oc:";
    public static final String savePath = "opencomputers/";
    public static final String scriptPath = "/assets/" + resourceDomain + "/lua/";
    public static final int[][] screenResolutionsByTier = new int[][]{new int[]{50, 16}, new int[]{80, 25}, new int[]{160, 50}};
    public static final TextBuffer.ColorDepth[] screenDepthsByTier = new TextBuffer.ColorDepth[]{TextBuffer.ColorDepth.OneBit, TextBuffer.ColorDepth.FourBit, TextBuffer.ColorDepth.EightBit};
    public static final int[] deviceComplexityByTier = new int[]{12, 24, 32, 9001};
    public static boolean rTreeDebugRenderer = false;
    public static int blockRenderId = -1;
    public static int[] databaseEntriesPerTier = new int[]{9, 25, 81};

    public static int getBasicScreenPixels() {
        return screenResolutionsByTier[0][0] * screenResolutionsByTier[0][1];
    }

    public static final class BlockName {
        public static final String AccessPoint = "accessPoint";
        public static final String Adapter = "adapter";
        public static final String Assembler = "assembler";
        public static final String Cable = "cable";
        public static final String Capacitor = "capacitor";
        public static final String CaseCreative = "caseCreative";
        public static final String CaseTier1 = "case1";
        public static final String CaseTier2 = "case2";
        public static final String CaseTier3 = "case3";
        public static final String ChameliumBlock = "chameliumBlock";
        public static final String Charger = "charger";
        public static final String Disassembler = "disassembler";
        public static final String DiskDrive = "diskDrive";
        public static final String Endstone = "endstone";
        public static final String Geolyzer = "geolyzer";
        public static final String HologramTier1 = "hologram1";
        public static final String HologramTier2 = "hologram2";
        public static final String Keyboard = "keyboard";
        public static final String Microcontroller = "microcontroller";
        public static final String MotionSensor = "motionSensor";
        public static final String NetSplitter = "netSplitter";
        public static final String PowerConverter = "powerConverter";
        public static final String PowerDistributor = "powerDistributor";
        public static final String Print = "print";
        public static final String Printer = "printer";
        public static final String Raid = "raid";
        public static final String Redstone = "redstone";
        public static final String Relay = "relay";
        public static final String Robot = "robot";
        public static final String RobotAfterimage = "robotAfterimage";
        public static final String ScreenTier1 = "screen1";
        public static final String ScreenTier2 = "screen2";
        public static final String ScreenTier3 = "screen3";
        public static final String Rack = "rack";
        public static final String Switch = "switch";
        public static final String Transposer = "transposer";
        public static final String Waypoint = "waypoint";

//    def Case(tier: Int) = ItemUtils.caseNameWithTierSuffix("case", tier)
    }

    public static final class ItemName {
        public static final String AbstractBusCard = "abstractBusCard";
        public static final String Acid = "acid";
        public static final String Alu = "alu";
        public static final String Analyzer = "analyzer";
        public static final String AngelUpgrade = "angelUpgrade";
        public static final String APUCreative = "apuCreative";
        public static final String APUTier1 = "apu1";
        public static final String APUTier2 = "apu2";
        public static final String ArrowKeys = "arrowKeys";
        public static final String BatteryUpgradeTier1 = "batteryUpgrade1";
        public static final String BatteryUpgradeTier2 = "batteryUpgrade2";
        public static final String BatteryUpgradeTier3 = "batteryUpgrade3";
        public static final String ButtonGroup = "buttonGroup";
        public static final String Card = "card";
        public static final String CardContainerTier1 = "cardContainer1";
        public static final String CardContainerTier2 = "cardContainer2";
        public static final String CardContainerTier3 = "cardContainer3";
        public static final String Chamelium = "chamelium";
        public static final String ChipTier1 = "chip1";
        public static final String ChipTier2 = "chip2";
        public static final String ChipTier3 = "chip3";
        public static final String ChunkloaderUpgrade = "chunkloaderUpgrade";
        public static final String CircuitBoard = "circuitBoard";
        public static final String ComponentBusTier1 = "componentBus1";
        public static final String ComponentBusTier2 = "componentBus2";
        public static final String ComponentBusTier3 = "componentBus3";
        public static final String CPUTier1 = "cpu1";
        public static final String CPUTier2 = "cpu2";
        public static final String CPUTier3 = "cpu3";
        public static final String CraftingUpgrade = "craftingUpgrade";
        public static final String ControlUnit = "cu";
        public static final String CuttingWire = "cuttingWire";
        public static final String DatabaseUpgradeTier1 = "databaseUpgrade1";
        public static final String DatabaseUpgradeTier2 = "databaseUpgrade2";
        public static final String DatabaseUpgradeTier3 = "databaseUpgrade3";
        public static final String DataCardTier1 = "dataCard1";
        public static final String DataCardTier2 = "dataCard2";
        public static final String DataCardTier3 = "dataCard3";
        public static final String DebugCard = "debugCard";
        public static final String Debugger = "debugger";
        public static final String DiamondChip = "chipDiamond";
        public static final String Disk = "disk";
        public static final String DiskDriveMountable = "diskDriveMountable";
        public static final String Drone = "drone";
        public static final String DroneCaseCreative = "droneCaseCreative";
        public static final String DroneCaseTier1 = "droneCase1";
        public static final String DroneCaseTier2 = "droneCase2";
        public static final String EEPROM = "eeprom";
        public static final String ExperienceUpgrade = "experienceUpgrade";
        public static final String Floppy = "floppy";
        public static final String GeneratorUpgrade = "generatorUpgrade";
        public static final String GraphicsCardTier1 = "graphicsCard1";
        public static final String GraphicsCardTier2 = "graphicsCard2";
        public static final String GraphicsCardTier3 = "graphicsCard3";
        public static final String HDDTier1 = "hdd1";
        public static final String HDDTier2 = "hdd2";
        public static final String HDDTier3 = "hdd3";
        public static final String HoverBoots = "hoverBoots";
        public static final String HoverUpgradeTier1 = "hoverUpgrade1";
        public static final String HoverUpgradeTier2 = "hoverUpgrade2";
        public static final String InkCartridgeEmpty = "inkCartridgeEmpty";
        public static final String InkCartridge = "inkCartridge";
        public static final String InternetCard = "internetCard";
        public static final String Interweb = "interweb";
        public static final String InventoryControllerUpgrade = "inventoryControllerUpgrade";
        public static final String InventoryUpgrade = "inventoryUpgrade";
        public static final String IronNugget = "nuggetIron";
        public static final String LeashUpgrade = "leashUpgrade";
        public static final String LinkedCard = "linkedCard";
        public static final String LuaBios = "luaBios";
        public static final String MFU = "mfu";
        public static final String Manual = "manual";
        public static final String MicrocontrollerCaseCreative = "microcontrollerCaseCreative";
        public static final String MicrocontrollerCaseTier1 = "microcontrollerCase1";
        public static final String MicrocontrollerCaseTier2 = "microcontrollerCase2";
        public static final String Nanomachines = "nanomachines";
        public static final String NavigationUpgrade = "navigationUpgrade";
        public static final String NetworkCard = "lanCard";
        public static final String NumPad = "numPad";
        public static final String OpenOS = "openos";
        public static final String PistonUpgrade = "pistonUpgrade";
        public static final String Present = "present";
        public static final String PrintedCircuitBoard = "printedCircuitBoard";
        public static final String RAMTier1 = "ram1";
        public static final String RAMTier2 = "ram2";
        public static final String RAMTier3 = "ram3";
        public static final String RAMTier4 = "ram4";
        public static final String RAMTier5 = "ram5";
        public static final String RAMTier6 = "ram6";
        public static final String RawCircuitBoard = "rawCircuitBoard";
        public static final String RedstoneCardTier1 = "redstoneCard1";
        public static final String RedstoneCardTier2 = "redstoneCard2";
        public static final String ServerCreative = "serverCreative";
        public static final String ServerTier1 = "server1";
        public static final String ServerTier2 = "server2";
        public static final String ServerTier3 = "server3";
        public static final String SignUpgrade = "signUpgrade";
        public static final String SolarGeneratorUpgrade = "solarGeneratorUpgrade";
        public static final String Tablet = "tablet";
        public static final String TabletCaseCreative = "tabletCaseCreative";
        public static final String TabletCaseTier1 = "tabletCase1";
        public static final String TabletCaseTier2 = "tabletCase2";
        public static final String TankControllerUpgrade = "tankControllerUpgrade";
        public static final String TankUpgrade = "tankUpgrade";
        public static final String Terminal = "terminal";
        public static final String TerminalServer = "terminalServer";
        public static final String TexturePicker = "texturePicker";
        public static final String TractorBeamUpgrade = "tractorBeamUpgrade";
        public static final String TradingUpgrade = "tradingUpgrade";
        public static final String Transistor = "transistor";
        public static final String UpgradeContainerTier1 = "upgradeContainer1";
        public static final String UpgradeContainerTier2 = "upgradeContainer2";
        public static final String UpgradeContainerTier3 = "upgradeContainer3";
        public static final String WirelessNetworkCard = "wlanCard";
        public static final String WorldSensorCard = "worldSensorCard";
        public static final String Wrench = "wrench";

//    def DroneCase(tier: Int) = ItemUtils.caseNameWithTierSuffix("droneCase", tier)
//
//    def MicrocontrollerCase(tier: Int) = ItemUtils.caseNameWithTierSuffix("microcontrollerCase", tier)
//
//    def TabletCase(tier: Int) = ItemUtils.caseNameWithTierSuffix("tabletCase", tier)
    }

    public static final class DeviceInfo {
        public static final String DefaultVendor = "MightyPirates GmbH & Co. KG";
        public static final String Scummtech = "Scummtech, Inc.";
    }
}
