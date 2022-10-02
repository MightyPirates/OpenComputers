package li.cil.oc.common.init

import java.util.concurrent.Callable

import li.cil.oc.Constants
import li.cil.oc.CreativeTab
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.detail.ItemAPI
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.api.fs.FileSystem
import li.cil.oc.common
import li.cil.oc.common.Loot
import li.cil.oc.common.Tier
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.common.item
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.common.item.data.HoverBootsData
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.common.item.traits.SimpleItem
import li.cil.oc.server.machine.luac.LuaStateFactory
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.item.DyeColor
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.ToolType
import net.minecraftforge.registries.GameData

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Items extends ItemAPI {
  val descriptors = mutable.Map.empty[String, ItemInfo]

  val names = mutable.Map.empty[Any, String]

  val aliases = Map(
    "datacard" -> Constants.ItemName.DataCardTier1,
    "wlancard" -> Constants.ItemName.WirelessNetworkCardTier2
  )

  override def get(name: String): ItemInfo = descriptors.get(name).orNull

  override def get(stack: ItemStack): ItemInfo = names.get(getBlockOrItem(stack)) match {
    case Some(name) => get(name)
    case _ => null
  }

  private def defaultProps = new Properties().tab(CreativeTab)

  def registerBlockOnly(instance: Block, id: String): Block = {
    if (!descriptors.contains(id)) {
      instance match {
        case simple: SimpleBlock =>
          simple.setUnlocalizedName("oc." + id)
          simple.setRegistryName(OpenComputers.ID, id)
          GameData.register_impl[Block](simple)
        case _ =>
      }
      descriptors += id -> new ItemInfo {
        override def name: String = id

        override def block = instance

        override def item = null

        override def createItemStack(size: Int): ItemStack = {
          OpenComputers.log.warn(s"Attempt to get ItemStack for block ${instance} without item form")
          ItemStack.EMPTY
        }
      }
      names += instance -> id
    }
    instance
  }

  def registerBlock(instance: Block, id: String): Block = {
    if (!descriptors.contains(id)) {
      instance match {
        case simple: SimpleBlock =>
          simple.setUnlocalizedName("oc." + id)
          simple.setRegistryName(OpenComputers.ID, id)
          GameData.register_impl[Block](simple)

          val props = defaultProps.tab(simple.getCreativeTab)
          val item : Item = new common.block.Item(simple, props)
          item.setRegistryName(OpenComputers.ID, id)
          GameData.register_impl(item)
          OpenComputers.proxy.registerModel(item, id)
        case _ =>
      }
      descriptors += id -> new ItemInfo {
        override def name: String = id

        override def block = instance

        override def item = item

        override def createItemStack(size: Int): ItemStack = instance match {
          case simple: SimpleBlock => simple.createItemStack(size)
          case _ => new ItemStack(instance, size)
        }
      }
      names += instance -> id
    }
    instance
  }

  def registerItem(instance: Item, id: String): Item = {
    if (!descriptors.contains(id)) {
      instance match {
        case simple: SimpleItem =>
          GameData.register_impl(simple.setRegistryName(new ResourceLocation(Settings.resourceDomain, id)))
          OpenComputers.proxy.registerModel(simple, id)
        case _ =>
      }
      descriptors += id -> new ItemInfo {
        override def name: String = id

        override def block = null

        override def item: Item = instance

        override def createItemStack(size: Int): ItemStack = instance match {
          case simple: SimpleItem => simple.createItemStack(size)
          case _ => new ItemStack(instance, size)
        }
      }
      names += instance -> id
    }
    instance
  }

  def registerStack(stack: ItemStack, id: String): ItemStack = {
    val immutableStack = stack.copy()
    descriptors += id -> new ItemInfo {
      override def name: String = id

      override def block = null

      override def createItemStack(size: Int): ItemStack = {
        val copy = immutableStack.copy()
        copy.setCount(size)
        copy
      }

      override def item: Item = immutableStack.getItem
    }
    stack
  }

  private def getBlockOrItem(stack: ItemStack): Any =
    if (stack.isEmpty) null
    else stack.getItem match {
      case block: BlockItem => block.getBlock
      case item => item
    }

  // ----------------------------------------------------------------------- //

  val registeredItems: ArrayBuffer[ItemStack] = mutable.ArrayBuffer.empty[ItemStack]

  override def registerFloppy(name: String, color: DyeColor, factory: Callable[FileSystem], doRecipeCycling: Boolean): ItemStack = {
    val stack = Loot.registerLootDisk(name, color, factory, doRecipeCycling)

    registeredItems += stack

    stack.copy()
  }

  override def registerEEPROM(name: String, code: Array[Byte], data: Array[Byte], readonly: Boolean): ItemStack = {
    val stack = get(Constants.ItemName.EEPROM).createItemStack(1)
    val nbt = stack.getOrCreateTagElement(Settings.namespace + "data")
    if (name != null) {
      nbt.putString(Settings.namespace + "label", name.trim.take(24))
    }
    if (code != null) {
      nbt.putByteArray(Settings.namespace + "eeprom", code.take(Settings.get.eepromSize))
    }
    if (data != null) {
      nbt.putByteArray(Settings.namespace + "userdata", data.take(Settings.get.eepromDataSize))
    }
    nbt.putBoolean(Settings.namespace + "readonly", readonly)

    registeredItems += stack

    stack.copy()
  }

  // ----------------------------------------------------------------------- //

  private def safeGetStack(name: String) = Option(get(name)).map(_.createItemStack(1)).getOrElse(ItemStack.EMPTY)

  def createConfiguredDrone(): ItemStack = {
    val data = new DroneData()

    data.name = "Crecopter"
    data.tier = Tier.Four
    data.storedEnergy = Settings.get.bufferDrone.toInt
    data.components = Array(
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryControllerUpgrade),
      safeGetStack(Constants.ItemName.TankUpgrade),
      safeGetStack(Constants.ItemName.TankControllerUpgrade),
      safeGetStack(Constants.ItemName.LeashUpgrade),
      safeGetStack(Constants.ItemName.AngelUpgrade),

      safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),

      LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
      safeGetStack(Constants.ItemName.RAMTier6),
      safeGetStack(Constants.ItemName.RAMTier6)
    ).filter(!_.isEmpty)

    data.createItemStack()
  }

  def createConfiguredMicrocontroller(): ItemStack = {
    val data = new MicrocontrollerData()

    data.tier = Tier.Four
    data.storedEnergy = Settings.get.bufferMicrocontroller.toInt
    data.components = Array(
      safeGetStack(Constants.ItemName.SignUpgrade),
      safeGetStack(Constants.ItemName.PistonUpgrade),

      safeGetStack(Constants.ItemName.RedstoneCardTier2),
      safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),

      LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
      safeGetStack(Constants.ItemName.RAMTier6),
      safeGetStack(Constants.ItemName.RAMTier6)
    ).filter(!_.isEmpty)

    data.createItemStack()
  }

  def createConfiguredRobot(): ItemStack = {
    val data = new RobotData()

    data.name = "Creatix"
    data.tier = Tier.Four
    data.robotEnergy = Settings.get.bufferRobot.toInt
    data.totalEnergy = data.robotEnergy
    data.components = Array(
      safeGetStack(Constants.BlockName.ScreenTier1),
      safeGetStack(Constants.BlockName.Keyboard),
      safeGetStack(Constants.BlockName.Geolyzer),
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryUpgrade),
      safeGetStack(Constants.ItemName.InventoryControllerUpgrade),
      safeGetStack(Constants.ItemName.TankUpgrade),
      safeGetStack(Constants.ItemName.TankControllerUpgrade),
      safeGetStack(Constants.ItemName.CraftingUpgrade),
      safeGetStack(Constants.ItemName.HoverUpgradeTier2),
      safeGetStack(Constants.ItemName.AngelUpgrade),
      safeGetStack(Constants.ItemName.TradingUpgrade),
      safeGetStack(Constants.ItemName.ExperienceUpgrade),

      safeGetStack(Constants.ItemName.GraphicsCardTier3),
      safeGetStack(Constants.ItemName.RedstoneCardTier2),
      safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),
      safeGetStack(Constants.ItemName.InternetCard),

      LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
      safeGetStack(Constants.ItemName.RAMTier6),
      safeGetStack(Constants.ItemName.RAMTier6),

      safeGetStack(Constants.ItemName.LuaBios),
      safeGetStack(Constants.ItemName.OpenOS),
      safeGetStack(Constants.ItemName.HDDTier3)
    ).filter(!_.isEmpty)
    data.containers = Array(
      safeGetStack(Constants.ItemName.CardContainerTier3),
      safeGetStack(Constants.ItemName.UpgradeContainerTier3),
      safeGetStack(Constants.BlockName.DiskDrive)
    ).filter(!_.isEmpty)

    data.createItemStack()
  }

  def createConfiguredTablet(): ItemStack = {
    val data = new TabletData()

    data.tier = Tier.Four
    data.energy = Settings.get.bufferTablet
    data.maxEnergy = data.energy
    data.items = Array(
      safeGetStack(Constants.BlockName.ScreenTier1),
      safeGetStack(Constants.BlockName.Keyboard),

      safeGetStack(Constants.ItemName.SignUpgrade),
      safeGetStack(Constants.ItemName.PistonUpgrade),
      safeGetStack(Constants.BlockName.Geolyzer),
      safeGetStack(Constants.ItemName.NavigationUpgrade),
      safeGetStack(Constants.ItemName.Analyzer),

      safeGetStack(Constants.ItemName.GraphicsCardTier2),
      safeGetStack(Constants.ItemName.RedstoneCardTier2),
      safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),

      LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
      safeGetStack(Constants.ItemName.RAMTier6),
      safeGetStack(Constants.ItemName.RAMTier6),

      safeGetStack(Constants.ItemName.LuaBios),
      safeGetStack(Constants.ItemName.HDDTier3)
    ).padTo(32, ItemStack.EMPTY)
    data.items(31) = safeGetStack(Constants.ItemName.OpenOS)
    data.container = safeGetStack(Constants.BlockName.DiskDrive)

    data.createItemStack()
  }

  def createChargedHoverBoots(): ItemStack = {
    val data = new HoverBootsData()
    data.charge = Settings.get.bufferHoverBoots

    data.createItemStack()
  }

  // ----------------------------------------------------------------------- //

  def init() {
    initMaterials()
    initTools()
    initComponents()
    initCards()
    initUpgrades()
    initStorage()
    initSpecial()

    // Register aliases.
    for ((k, v) <- aliases) {
      descriptors.getOrElseUpdate(k, descriptors(v))
    }
  }

  // Crafting materials.
  private def initMaterials(): Unit = {
    registerItem(new item.CuttingWire(defaultProps), Constants.ItemName.CuttingWire)
    registerItem(new item.Acid(defaultProps), Constants.ItemName.Acid)
    registerItem(new item.RawCircuitBoard(defaultProps), Constants.ItemName.RawCircuitBoard)
    registerItem(new item.CircuitBoard(defaultProps), Constants.ItemName.CircuitBoard)
    registerItem(new item.PrintedCircuitBoard(defaultProps), Constants.ItemName.PrintedCircuitBoard)
    registerItem(new item.CardBase(defaultProps), Constants.ItemName.Card)
    registerItem(new item.Transistor(defaultProps), Constants.ItemName.Transistor)
    registerItem(new item.Microchip(defaultProps, Tier.One), Constants.ItemName.ChipTier1)
    registerItem(new item.Microchip(defaultProps, Tier.Two), Constants.ItemName.ChipTier2)
    registerItem(new item.Microchip(defaultProps, Tier.Three), Constants.ItemName.ChipTier3)
    registerItem(new item.ALU(defaultProps), Constants.ItemName.Alu)
    registerItem(new item.ControlUnit(defaultProps), Constants.ItemName.ControlUnit)
    registerItem(new item.Disk(defaultProps), Constants.ItemName.Disk)
    registerItem(new item.Interweb(defaultProps), Constants.ItemName.Interweb)
    registerItem(new item.ButtonGroup(defaultProps), Constants.ItemName.ButtonGroup)
    registerItem(new item.ArrowKeys(defaultProps), Constants.ItemName.ArrowKeys)
    registerItem(new item.NumPad(defaultProps), Constants.ItemName.NumPad)

    registerItem(new item.TabletCase(defaultProps, Tier.One), Constants.ItemName.TabletCaseTier1)
    registerItem(new item.TabletCase(defaultProps, Tier.Two), Constants.ItemName.TabletCaseTier2)
    registerItem(new item.TabletCase(defaultProps, Tier.Four), Constants.ItemName.TabletCaseCreative)
    registerItem(new item.MicrocontrollerCase(defaultProps, Tier.One), Constants.ItemName.MicrocontrollerCaseTier1)
    registerItem(new item.MicrocontrollerCase(defaultProps, Tier.Two), Constants.ItemName.MicrocontrollerCaseTier2)
    registerItem(new item.MicrocontrollerCase(defaultProps, Tier.Four), Constants.ItemName.MicrocontrollerCaseCreative)
    registerItem(new item.DroneCase(defaultProps, Tier.One), Constants.ItemName.DroneCaseTier1)
    registerItem(new item.DroneCase(defaultProps, Tier.Two), Constants.ItemName.DroneCaseTier2)
    registerItem(new item.DroneCase(defaultProps, Tier.Four), Constants.ItemName.DroneCaseCreative)

    registerItem(new item.InkCartridgeEmpty(defaultProps.stacksTo(1)), Constants.ItemName.InkCartridgeEmpty)
    registerItem(new item.InkCartridge(defaultProps.stacksTo(1).craftRemainder(get(Constants.ItemName.InkCartridgeEmpty).item)), Constants.ItemName.InkCartridge)
    registerItem(new item.Chamelium(defaultProps), Constants.ItemName.Chamelium)

    registerItem(new item.DiamondChip(defaultProps), Constants.ItemName.DiamondChip)
  }

  val WrenchType: ToolType = ToolType.get("wrench")

  // All kinds of tools.
  private def initTools(): Unit = {
    registerItem(new item.Analyzer(defaultProps), Constants.ItemName.Analyzer)
    registerItem(new item.Debugger(defaultProps), Constants.ItemName.Debugger)
    registerItem(new item.Terminal(defaultProps.stacksTo(1)), Constants.ItemName.Terminal)
    registerItem(new item.TexturePicker(defaultProps), Constants.ItemName.TexturePicker)
    registerItem(new item.Manual(defaultProps), Constants.ItemName.Manual)
    registerItem(new item.Wrench(defaultProps.stacksTo(1).addToolType(WrenchType, 1)), Constants.ItemName.Wrench)

    // 1.5.11
    registerItem(new item.HoverBoots(defaultProps.stacksTo(1).setNoRepair), Constants.ItemName.HoverBoots)

    // 1.5.18
    registerItem(new item.Nanomachines(defaultProps), Constants.ItemName.Nanomachines)
  }

  // General purpose components.
  private def initComponents(): Unit = {
    registerItem(new item.CPU(defaultProps, Tier.One), Constants.ItemName.CPUTier1)
    registerItem(new item.CPU(defaultProps, Tier.Two), Constants.ItemName.CPUTier2)
    registerItem(new item.CPU(defaultProps, Tier.Three), Constants.ItemName.CPUTier3)

    registerItem(new item.ComponentBus(defaultProps, Tier.One), Constants.ItemName.ComponentBusTier1)
    registerItem(new item.ComponentBus(defaultProps, Tier.Two), Constants.ItemName.ComponentBusTier2)
    registerItem(new item.ComponentBus(defaultProps, Tier.Three), Constants.ItemName.ComponentBusTier3)

    registerItem(new item.Memory(defaultProps, Tier.One), Constants.ItemName.RAMTier1)
    registerItem(new item.Memory(defaultProps, Tier.Two), Constants.ItemName.RAMTier2)
    registerItem(new item.Memory(defaultProps, Tier.Three), Constants.ItemName.RAMTier3)
    registerItem(new item.Memory(defaultProps, Tier.Four), Constants.ItemName.RAMTier4)
    registerItem(new item.Memory(defaultProps, Tier.Five), Constants.ItemName.RAMTier5)
    registerItem(new item.Memory(defaultProps, Tier.Six), Constants.ItemName.RAMTier6)

    registerItem(new item.Server(defaultProps.stacksTo(1), Tier.Four), Constants.ItemName.ServerCreative)
    registerItem(new item.Server(defaultProps.stacksTo(1), Tier.One), Constants.ItemName.ServerTier1)
    registerItem(new item.Server(defaultProps.stacksTo(1), Tier.Two), Constants.ItemName.ServerTier2)
    registerItem(new item.Server(defaultProps.stacksTo(1), Tier.Three), Constants.ItemName.ServerTier3)

    // 1.5.10
    registerItem(new item.APU(defaultProps, Tier.One), Constants.ItemName.APUTier1)
    registerItem(new item.APU(defaultProps, Tier.Two), Constants.ItemName.APUTier2)

    // 1.5.12
    registerItem(new item.APU(defaultProps, Tier.Three), Constants.ItemName.APUCreative)

    // 1.6
    registerItem(new item.TerminalServer(defaultProps.stacksTo(1)), Constants.ItemName.TerminalServer)
    registerItem(new item.DiskDriveMountable(defaultProps.stacksTo(1)), Constants.ItemName.DiskDriveMountable)
  }

  // Card components.
  private def initCards(): Unit = {
    registerItem(new item.DebugCard(defaultProps), Constants.ItemName.DebugCard)
    registerItem(new item.GraphicsCard(defaultProps, Tier.One), Constants.ItemName.GraphicsCardTier1)
    registerItem(new item.GraphicsCard(defaultProps, Tier.Two), Constants.ItemName.GraphicsCardTier2)
    registerItem(new item.GraphicsCard(defaultProps, Tier.Three), Constants.ItemName.GraphicsCardTier3)
    registerItem(new item.RedstoneCard(defaultProps, Tier.One), Constants.ItemName.RedstoneCardTier1)
    registerItem(new item.RedstoneCard(defaultProps, Tier.Two), Constants.ItemName.RedstoneCardTier2)
    registerItem(new item.NetworkCard(defaultProps), Constants.ItemName.NetworkCard)
    registerItem(new item.WirelessNetworkCard(defaultProps, Tier.Two), Constants.ItemName.WirelessNetworkCardTier2)
    registerItem(new item.InternetCard(defaultProps), Constants.ItemName.InternetCard)
    registerItem(new item.LinkedCard(defaultProps), Constants.ItemName.LinkedCard)

    // 1.5.13
    registerItem(new item.DataCard(defaultProps, Tier.One), Constants.ItemName.DataCardTier1)

    // 1.5.15
    registerItem(new item.DataCard(defaultProps, Tier.Two), Constants.ItemName.DataCardTier2)
    registerItem(new item.DataCard(defaultProps, Tier.Three), Constants.ItemName.DataCardTier3)
  }

  // Upgrade components.
  private def initUpgrades(): Unit = {
    registerItem(new item.UpgradeAngel(defaultProps), Constants.ItemName.AngelUpgrade)
    registerItem(new item.UpgradeBattery(defaultProps, Tier.One), Constants.ItemName.BatteryUpgradeTier1)
    registerItem(new item.UpgradeBattery(defaultProps, Tier.Two), Constants.ItemName.BatteryUpgradeTier2)
    registerItem(new item.UpgradeBattery(defaultProps, Tier.Three), Constants.ItemName.BatteryUpgradeTier3)
    registerItem(new item.UpgradeChunkloader(defaultProps), Constants.ItemName.ChunkloaderUpgrade)
    registerItem(new item.UpgradeContainerCard(defaultProps, Tier.One), Constants.ItemName.CardContainerTier1)
    registerItem(new item.UpgradeContainerCard(defaultProps, Tier.Two), Constants.ItemName.CardContainerTier2)
    registerItem(new item.UpgradeContainerCard(defaultProps, Tier.Three), Constants.ItemName.CardContainerTier3)
    registerItem(new item.UpgradeContainerUpgrade(defaultProps, Tier.One), Constants.ItemName.UpgradeContainerTier1)
    registerItem(new item.UpgradeContainerUpgrade(defaultProps, Tier.Two), Constants.ItemName.UpgradeContainerTier2)
    registerItem(new item.UpgradeContainerUpgrade(defaultProps, Tier.Three), Constants.ItemName.UpgradeContainerTier3)
    registerItem(new item.UpgradeCrafting(defaultProps), Constants.ItemName.CraftingUpgrade)
    registerItem(new item.UpgradeDatabase(defaultProps, Tier.One), Constants.ItemName.DatabaseUpgradeTier1)
    registerItem(new item.UpgradeDatabase(defaultProps, Tier.Two), Constants.ItemName.DatabaseUpgradeTier2)
    registerItem(new item.UpgradeDatabase(defaultProps, Tier.Three), Constants.ItemName.DatabaseUpgradeTier3)
    registerItem(new item.UpgradeExperience(defaultProps), Constants.ItemName.ExperienceUpgrade)
    registerItem(new item.UpgradeGenerator(defaultProps), Constants.ItemName.GeneratorUpgrade)
    registerItem(new item.UpgradeInventory(defaultProps), Constants.ItemName.InventoryUpgrade)
    registerItem(new item.UpgradeInventoryController(defaultProps), Constants.ItemName.InventoryControllerUpgrade)
    registerItem(new item.UpgradeNavigation(defaultProps), Constants.ItemName.NavigationUpgrade)
    registerItem(new item.UpgradePiston(defaultProps), Constants.ItemName.PistonUpgrade)
    registerItem(new item.UpgradeSign(defaultProps), Constants.ItemName.SignUpgrade)
    registerItem(new item.UpgradeSolarGenerator(defaultProps), Constants.ItemName.SolarGeneratorUpgrade)
    registerItem(new item.UpgradeTank(defaultProps), Constants.ItemName.TankUpgrade)
    registerItem(new item.UpgradeTankController(defaultProps), Constants.ItemName.TankControllerUpgrade)
    registerItem(new item.UpgradeTractorBeam(defaultProps), Constants.ItemName.TractorBeamUpgrade)
    registerItem(new item.UpgradeLeash(defaultProps), Constants.ItemName.LeashUpgrade)

    // 1.5.8
    registerItem(new item.UpgradeHover(defaultProps, Tier.One), Constants.ItemName.HoverUpgradeTier1)
    registerItem(new item.UpgradeHover(defaultProps, Tier.Two), Constants.ItemName.HoverUpgradeTier2)

    // 1.6
    registerItem(new item.UpgradeTrading(defaultProps), Constants.ItemName.TradingUpgrade)
    registerItem(new item.UpgradeMF(defaultProps), Constants.ItemName.MFU)

    // 1.7.2
    registerItem(new item.WirelessNetworkCard(defaultProps, Tier.One), Constants.ItemName.WirelessNetworkCardTier1)
    registerItem(new item.ComponentBus(defaultProps, Tier.Four), Constants.ItemName.ComponentBusCreative)

    // 1.8
    registerItem(new item.UpgradeStickyPiston(defaultProps), Constants.ItemName.StickyPistonUpgrade)
  }

  // Storage media of all kinds.
  private def initStorage(): Unit = {
    registerItem(new item.EEPROM(defaultProps), Constants.ItemName.EEPROM)
    registerItem(new item.FloppyDisk(defaultProps), Constants.ItemName.Floppy)
    registerItem(new item.HardDiskDrive(defaultProps, Tier.One), Constants.ItemName.HDDTier1)
    registerItem(new item.HardDiskDrive(defaultProps, Tier.Two), Constants.ItemName.HDDTier2)
    registerItem(new item.HardDiskDrive(defaultProps, Tier.Three), Constants.ItemName.HDDTier3)

    val luaBios = {
      val code = new Array[Byte](4 * 1024)
      val count = OpenComputers.getClass.getResourceAsStream(Settings.scriptPath + "bios.lua").read(code)
      registerEEPROM("EEPROM (Lua BIOS)", code.take(count), null, readonly = false)
    }
    registerStack(luaBios, Constants.ItemName.LuaBios)

  }

  // Special purpose items that don't fit into any other category.
  private def initSpecial(): Unit = {
    registerItem(new item.Tablet(defaultProps.stacksTo(1)), Constants.ItemName.Tablet)
    registerItem(new item.Drone(defaultProps), Constants.ItemName.Drone)
    registerItem(new item.Present(defaultProps), Constants.ItemName.Present)
  }

  def decorateCreativeTab(list: NonNullList[ItemStack]) {
    list.add(Items.createConfiguredDrone())
    list.add(Items.createConfiguredMicrocontroller())
    list.add(Items.createConfiguredRobot())
    list.add(Items.createConfiguredTablet())
    Loot.disksForClient.foreach(list.add)
    registeredItems.foreach(list.add)
  }
}
