package li.cil.oc.integration.igwmod

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import igwmod.api.BlockWikiEvent
import igwmod.api.ItemWikiEvent
import li.cil.oc.common.block._
import li.cil.oc.common.item._
import li.cil.oc.OpenComputers.log
import net.minecraftforge.common.MinecraftForge

object ModIGWMod {
  def init() {
    log.info(s"IGWMod integration loading")
    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent
  def onPageRequest(event: BlockWikiEvent) {
    //Blocks
    if (event.isInstanceOf[BlockWikiEvent]) {
      if (matchBlocks(event.block)) {
        log.info((s"Here goes the implemenatation for " + event.block.getUnlocalizedName + ".usage").replaceAll("(\\d+,\\d+)|\\d+", ""))
      }
    }
  }

  @SubscribeEvent
  def onPageRequest(event: ItemWikiEvent) {
    //Items + changing pages inside the Wiki GUI
    if (event.isInstanceOf[ItemWikiEvent]) {
      if (matchItems(event.itemStack.getItem)){
        log.info((s"Here goes the implemenatation for " + event.itemStack.getUnlocalizedName + ".usage").replaceAll("(\\d+,\\d+)|\\d+", ""))
      }
    }
  }

  def matchBlocks(x: Any): Boolean = x match {
    case ap: AccessPoint => true
    case adapter: Adapter => true
    case assembler: Assembler => true
    case cable: Cable => true
    case capacitor: Capacitor => true
    case casing: Case => true
    case chamelium: ChameliumBlock => true
    case charger: Charger => true
    case disassembler: Disassembler => true
    case diskDrive: DiskDrive => true
    case geolyzer: Geolyzer => true
    case hologram: Hologram => true
    case item: Item => true
    case keyboard: Keyboard => true
    case microcontroller: Microcontroller => true
    case motionSensor: MotionSensor => true
    case powerConv: PowerConverter => true
    case powerDist: PowerDistributor => true
    case print: Print => true
    case printer: Printer => true
    case raid: Raid => true
    case redstone: Redstone => true
    case redstoneAware: RedstoneAware => true
    case robotAfterImage: RobotAfterimage => true
    case robotProxy: RobotProxy => true //Tells unreachable code at this line, JUST this line
    case screen: Screen => true
    case rack: ServerRack => true
    case simpleBlock: SimpleBlock => true
    case switch: Switch => true
    case _ => false
  }

  def matchItems(x: Any): Boolean = x match {
    case abstractBusCard: AbstractBusCard => true
    case alu: ALU => true
    case arrowKeys: ArrowKeys => true
    case buttonGroup: ButtonGroup => true
    case cardBase: CardBase => true
    case chamelium: Chamelium => true
    case cb: CircuitBoard => true
    case componentBus: ComponentBus => true
    case cu: ControlUnit => true
    case cpu: CPU => true
    case cuttingWire: CuttingWire => true
    case debugCard: DebugCard => true
    case debugger: Debugger => true
    case delegator: Delegator => true
    case disk: Disk => true
    case drone: Drone => true
    case droneCase: DroneCase => true
    case eeprom: EEPROM => true
    case floppy: FloppyDisk => true
    case gpu: GraphicsCard => true
    case hdd:HardDiskDrive => true
    case ink: InkCartridge => true
    case inkE: InkCartridgeEmpty => true
    case iCard: InternetCard => true
    case iWeb: Interweb => true
    case nugget: IronNugget => true
    case linkedCard: LinkedCard => true
    case mem: Memory => true
    case microchip: Microchip => true
    case microCase: MicrocontrollerCase => true
    case netCard: NetworkCard => true
    case numPad: NumPad => true
    case present: Present => true
    case pcb: PrintedCircuitBoard => true
    case rcb: RawCircuitBoard => true
    case rsCard: RedstoneCard => true
    case server: Server => true
    case simpleItem: SimpleItem => true
    case tablet: Tablet => true
    case tabletC: TabletCase => true
    case terminal: Terminal => true
    case texturePicker: TexturePicker => true
    case transistor: Transistor => true

    case upgradeA: UpgradeAngel => true
    case upgradeB: UpgradeBattery => true
    case upgradeC: UpgradeChunkloader => true
    case upgradeCC: UpgradeContainerCard => true
    case upgradeCU: UpgradeContainerUpgrade => true
    case upgradeC: UpgradeCrafting => true
    case upgradeD: UpgradeDatabase => true
    case upgradeE: UpgradeExperience => true
    case upgradeG: UpgradeGenerator => true
    case upgradeI: UpgradeInventory => true
    case upgradeIC: UpgradeInventoryController => true
    case upgradeL: UpgradeLeash => true
    case upgradeN: UpgradeNavigation => true
    case upgradeP: UpgradePiston => true
    case upgradeS: UpgradeSign => true
    case upgradeSG: UpgradeSolarGenerator => true
    case upgradeT: UpgradeTank => true
    case upgradeTC: UpgradeTankController => true
    case upgradeTB: UpgradeTractorBeam => true

    case worldSensorCard: WirelessNetworkCard => true
    case worldSensorCard: WorldSensorCard => true
    case _ => false
  }
}