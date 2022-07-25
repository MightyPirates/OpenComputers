package li.cil.oc.common.init

import java.util.concurrent.Callable

import li.cil.oc.Constants
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
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.common.item.data.HoverBootsData
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.common.item.traits.Delegate
import li.cil.oc.common.item.traits.SimpleItem
import li.cil.oc.server.machine.luac.LuaStateFactory
import net.minecraft.block.Block
import net.minecraft.item.ItemGroup
import net.minecraft.item.DyeColor
import net.minecraft.item.Item
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.{GameData}

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

  def registerBlock(instance: Block, id: String): Block = {
    if (!descriptors.contains(id)) {
      instance match {
        case simple: SimpleBlock =>
          simple.setUnlocalizedName("oc." + id)
          simple.setRegistryName(OpenComputers.ID, id)
          GameData.register_impl[Block](simple)
          OpenComputers.proxy.registerModel(simple, id)

          val item : Item = new common.block.Item(simple)
          item.setRegistryName(OpenComputers.ID, id)
          GameData.register_impl(item)
          OpenComputers.proxy.registerModel(item, id)
        case _ =>
      }
      descriptors += id -> new ItemInfo {
        override def name: String = id

        override def block = instance

        override def item = null

        override def createItemStack(size: Int): ItemStack = instance match {
          case simple: SimpleBlock => simple.createItemStack(size)
          case _ => new ItemStack(instance, size)
        }
      }
      names += instance -> id
    }
    instance
  }

  def registerItem[T <: Delegate](delegate: T, id: String): T = {
    if (!descriptors.contains(id)) {
      OpenComputers.proxy.registerModel(delegate, id)
      descriptors += id -> new ItemInfo {
        override def name: String = id

        override def block = null

        override def item: Delegator = delegate.parent

        override def createItemStack(size: Int): ItemStack = delegate.createItemStack(size)
      }
      names += delegate -> id
    }
    delegate
  }

  def registerItem(instance: Item, id: String): Item = {
    if (!descriptors.contains(id)) {
      instance match {
        case simple: SimpleItem =>
          simple.setUnlocalizedName("oc." + id)
          GameData.register_impl(simple.setRegistryName(new ResourceLocation(Settings.resourceDomain, id)))
          OpenComputers.proxy.registerModel(instance, id)
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
    else Delegator.subItem(stack).getOrElse(stack.getItem match {
      case block: BlockItem => block.getBlock
      case item => item
    })

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
    val materials = newItem(new item.Delegator(), "material")

    registerItem(new item.CuttingWire(materials), Constants.ItemName.CuttingWire)
    registerItem(new item.Acid(materials), Constants.ItemName.Acid)
    registerItem(new item.RawCircuitBoard(materials), Constants.ItemName.RawCircuitBoard)
    registerItem(new item.CircuitBoard(materials), Constants.ItemName.CircuitBoard)
    registerItem(new item.PrintedCircuitBoard(materials), Constants.ItemName.PrintedCircuitBoard)
    registerItem(new item.CardBase(materials), Constants.ItemName.Card)
    registerItem(new item.Transistor(materials), Constants.ItemName.Transistor)
    registerItem(new item.Microchip(materials, Tier.One), Constants.ItemName.ChipTier1)
    registerItem(new item.Microchip(materials, Tier.Two), Constants.ItemName.ChipTier2)
    registerItem(new item.Microchip(materials, Tier.Three), Constants.ItemName.ChipTier3)
    registerItem(new item.ALU(materials), Constants.ItemName.Alu)
    registerItem(new item.ControlUnit(materials), Constants.ItemName.ControlUnit)
    registerItem(new item.Disk(materials), Constants.ItemName.Disk)
    registerItem(new item.Interweb(materials), Constants.ItemName.Interweb)
    registerItem(new item.ButtonGroup(materials), Constants.ItemName.ButtonGroup)
    registerItem(new item.ArrowKeys(materials), Constants.ItemName.ArrowKeys)
    registerItem(new item.NumPad(materials), Constants.ItemName.NumPad)

    registerItem(new item.TabletCase(materials, Tier.One), Constants.ItemName.TabletCaseTier1)
    registerItem(new item.TabletCase(materials, Tier.Two), Constants.ItemName.TabletCaseTier2)
    registerItem(new item.TabletCase(materials, Tier.Four), Constants.ItemName.TabletCaseCreative)
    registerItem(new item.MicrocontrollerCase(materials, Tier.One), Constants.ItemName.MicrocontrollerCaseTier1)
    registerItem(new item.MicrocontrollerCase(materials, Tier.Two), Constants.ItemName.MicrocontrollerCaseTier2)
    registerItem(new item.MicrocontrollerCase(materials, Tier.Four), Constants.ItemName.MicrocontrollerCaseCreative)
    registerItem(new item.DroneCase(materials, Tier.One), Constants.ItemName.DroneCaseTier1)
    registerItem(new item.DroneCase(materials, Tier.Two), Constants.ItemName.DroneCaseTier2)
    registerItem(new item.DroneCase(materials, Tier.Four), Constants.ItemName.DroneCaseCreative)

    registerItem(new item.InkCartridgeEmpty(materials), Constants.ItemName.InkCartridgeEmpty)
    registerItem(new item.InkCartridge(materials), Constants.ItemName.InkCartridge)
    registerItem(new item.Chamelium(materials), Constants.ItemName.Chamelium)

    registerItem(new item.DiamondChip(materials), Constants.ItemName.DiamondChip)
  }

  // All kinds of tools.
  private def initTools(): Unit = {
    val tools = newItem(new item.Delegator(), "tool")

    registerItem(new item.Analyzer(tools), Constants.ItemName.Analyzer)
    registerItem(new item.Debugger(tools), Constants.ItemName.Debugger)
    registerItem(new item.Terminal(tools), Constants.ItemName.Terminal)
    registerItem(new item.TexturePicker(tools), Constants.ItemName.TexturePicker)
    registerItem(new item.Manual(tools), Constants.ItemName.Manual)
    registerItem(new item.Wrench(), Constants.ItemName.Wrench)

    // 1.5.11
    registerItem(new item.HoverBoots(), Constants.ItemName.HoverBoots)

    // 1.5.18
    registerItem(new item.Nanomachines(tools), Constants.ItemName.Nanomachines)
  }

  // General purpose components.
  private def initComponents(): Unit = {
    val components = newItem(new item.Delegator(), "component")

    registerItem(new item.CPU(components, Tier.One), Constants.ItemName.CPUTier1)
    registerItem(new item.CPU(components, Tier.Two), Constants.ItemName.CPUTier2)
    registerItem(new item.CPU(components, Tier.Three), Constants.ItemName.CPUTier3)

    registerItem(new item.ComponentBus(components, Tier.One), Constants.ItemName.ComponentBusTier1)
    registerItem(new item.ComponentBus(components, Tier.Two), Constants.ItemName.ComponentBusTier2)
    registerItem(new item.ComponentBus(components, Tier.Three), Constants.ItemName.ComponentBusTier3)

    registerItem(new item.Memory(components, Tier.One), Constants.ItemName.RAMTier1)
    registerItem(new item.Memory(components, Tier.Two), Constants.ItemName.RAMTier2)
    registerItem(new item.Memory(components, Tier.Three), Constants.ItemName.RAMTier3)
    registerItem(new item.Memory(components, Tier.Four), Constants.ItemName.RAMTier4)
    registerItem(new item.Memory(components, Tier.Five), Constants.ItemName.RAMTier5)
    registerItem(new item.Memory(components, Tier.Six), Constants.ItemName.RAMTier6)

    registerItem(new item.Server(components, Tier.Four), Constants.ItemName.ServerCreative)
    registerItem(new item.Server(components, Tier.One), Constants.ItemName.ServerTier1)
    registerItem(new item.Server(components, Tier.Two), Constants.ItemName.ServerTier2)
    registerItem(new item.Server(components, Tier.Three), Constants.ItemName.ServerTier3)

    // 1.5.10
    registerItem(new item.APU(components, Tier.One), Constants.ItemName.APUTier1)
    registerItem(new item.APU(components, Tier.Two), Constants.ItemName.APUTier2)

    // 1.5.12
    registerItem(new item.APU(components, Tier.Three), Constants.ItemName.APUCreative)

    // 1.6
    registerItem(new item.TerminalServer(components), Constants.ItemName.TerminalServer)
    registerItem(new item.DiskDriveMountable(components), Constants.ItemName.DiskDriveMountable)
  }

  // Card components.
  private def initCards(): Unit = {
    val cards = newItem(new item.Delegator(), "card")

    registerItem(new item.DebugCard(cards), Constants.ItemName.DebugCard)
    registerItem(new item.GraphicsCard(cards, Tier.One), Constants.ItemName.GraphicsCardTier1)
    registerItem(new item.GraphicsCard(cards, Tier.Two), Constants.ItemName.GraphicsCardTier2)
    registerItem(new item.GraphicsCard(cards, Tier.Three), Constants.ItemName.GraphicsCardTier3)
    registerItem(new item.RedstoneCard(cards, Tier.One), Constants.ItemName.RedstoneCardTier1)
    registerItem(new item.RedstoneCard(cards, Tier.Two), Constants.ItemName.RedstoneCardTier2)
    registerItem(new item.NetworkCard(cards), Constants.ItemName.NetworkCard)
    registerItem(new item.WirelessNetworkCard(cards, Tier.Two), Constants.ItemName.WirelessNetworkCardTier2)
    registerItem(new item.InternetCard(cards), Constants.ItemName.InternetCard)
    registerItem(new item.LinkedCard(cards), Constants.ItemName.LinkedCard)

    // 1.5.13
    registerItem(new item.DataCard(cards, Tier.One), Constants.ItemName.DataCardTier1)

    // 1.5.15
    registerItem(new item.DataCard(cards, Tier.Two), Constants.ItemName.DataCardTier2)
    registerItem(new item.DataCard(cards, Tier.Three), Constants.ItemName.DataCardTier3)
  }

  // Upgrade components.
  private def initUpgrades(): Unit = {
    val upgrades = newItem(new item.Delegator(), "upgrade")

    registerItem(new item.UpgradeAngel(upgrades), Constants.ItemName.AngelUpgrade)
    registerItem(new item.UpgradeBattery(upgrades, Tier.One), Constants.ItemName.BatteryUpgradeTier1)
    registerItem(new item.UpgradeBattery(upgrades, Tier.Two), Constants.ItemName.BatteryUpgradeTier2)
    registerItem(new item.UpgradeBattery(upgrades, Tier.Three), Constants.ItemName.BatteryUpgradeTier3)
    registerItem(new item.UpgradeChunkloader(upgrades), Constants.ItemName.ChunkloaderUpgrade)
    registerItem(new item.UpgradeContainerCard(upgrades, Tier.One), Constants.ItemName.CardContainerTier1)
    registerItem(new item.UpgradeContainerCard(upgrades, Tier.Two), Constants.ItemName.CardContainerTier2)
    registerItem(new item.UpgradeContainerCard(upgrades, Tier.Three), Constants.ItemName.CardContainerTier3)
    registerItem(new item.UpgradeContainerUpgrade(upgrades, Tier.One), Constants.ItemName.UpgradeContainerTier1)
    registerItem(new item.UpgradeContainerUpgrade(upgrades, Tier.Two), Constants.ItemName.UpgradeContainerTier2)
    registerItem(new item.UpgradeContainerUpgrade(upgrades, Tier.Three), Constants.ItemName.UpgradeContainerTier3)
    registerItem(new item.UpgradeCrafting(upgrades), Constants.ItemName.CraftingUpgrade)
    registerItem(new item.UpgradeDatabase(upgrades, Tier.One), Constants.ItemName.DatabaseUpgradeTier1)
    registerItem(new item.UpgradeDatabase(upgrades, Tier.Two), Constants.ItemName.DatabaseUpgradeTier2)
    registerItem(new item.UpgradeDatabase(upgrades, Tier.Three), Constants.ItemName.DatabaseUpgradeTier3)
    registerItem(new item.UpgradeExperience(upgrades), Constants.ItemName.ExperienceUpgrade)
    registerItem(new item.UpgradeGenerator(upgrades), Constants.ItemName.GeneratorUpgrade)
    registerItem(new item.UpgradeInventory(upgrades), Constants.ItemName.InventoryUpgrade)
    registerItem(new item.UpgradeInventoryController(upgrades), Constants.ItemName.InventoryControllerUpgrade)
    registerItem(new item.UpgradeNavigation(upgrades), Constants.ItemName.NavigationUpgrade)
    registerItem(new item.UpgradePiston(upgrades), Constants.ItemName.PistonUpgrade)
    registerItem(new item.UpgradeSign(upgrades), Constants.ItemName.SignUpgrade)
    registerItem(new item.UpgradeSolarGenerator(upgrades), Constants.ItemName.SolarGeneratorUpgrade)
    registerItem(new item.UpgradeTank(upgrades), Constants.ItemName.TankUpgrade)
    registerItem(new item.UpgradeTankController(upgrades), Constants.ItemName.TankControllerUpgrade)
    registerItem(new item.UpgradeTractorBeam(upgrades), Constants.ItemName.TractorBeamUpgrade)
    registerItem(new item.UpgradeLeash(upgrades), Constants.ItemName.LeashUpgrade)

    // 1.5.8
    registerItem(new item.UpgradeHover(upgrades, Tier.One), Constants.ItemName.HoverUpgradeTier1)
    registerItem(new item.UpgradeHover(upgrades, Tier.Two), Constants.ItemName.HoverUpgradeTier2)

    // 1.6
    registerItem(new item.UpgradeTrading(upgrades), Constants.ItemName.TradingUpgrade)
    registerItem(new item.UpgradeMF(upgrades), Constants.ItemName.MFU)

    // 1.7.2
    registerItem(new item.WirelessNetworkCard(upgrades, Tier.One), Constants.ItemName.WirelessNetworkCardTier1)
    registerItem(new item.ComponentBus(upgrades, Tier.Four), Constants.ItemName.ComponentBusCreative)

    // 1.8
    registerItem(new item.UpgradeStickyPiston(upgrades), Constants.ItemName.StickyPistonUpgrade)
  }

  // Storage media of all kinds.
  private def initStorage(): Unit = {
    val storage = newItem(new item.Delegator(), "storage")

    registerItem(new item.EEPROM(storage), Constants.ItemName.EEPROM)
    registerItem(new item.FloppyDisk(storage), Constants.ItemName.Floppy)
    registerItem(new item.HardDiskDrive(storage, Tier.One), Constants.ItemName.HDDTier1)
    registerItem(new item.HardDiskDrive(storage, Tier.Two), Constants.ItemName.HDDTier2)
    registerItem(new item.HardDiskDrive(storage, Tier.Three), Constants.ItemName.HDDTier3)

    val luaBios = {
      val code = new Array[Byte](4 * 1024)
      val count = OpenComputers.getClass.getResourceAsStream(Settings.scriptPath + "bios.lua").read(code)
      registerEEPROM("EEPROM (Lua BIOS)", code.take(count), null, readonly = false)
    }
    registerStack(luaBios, Constants.ItemName.LuaBios)

  }

  // Special purpose items that don't fit into any other category.
  private def initSpecial(): Unit = {
    val misc = newItem(new item.Delegator() {
      private def configuredItems = Array(
        Items.createConfiguredDrone(),
        Items.createConfiguredMicrocontroller(),
        Items.createConfiguredRobot(),
        Items.createConfiguredTablet(),
        Items.createChargedHoverBoots()
      ) ++ Loot.disksForClient ++ registeredItems

      override def fillItemCategory(tab: ItemGroup, list: NonNullList[ItemStack]): Unit = {
        super.fillItemCategory(tab, list)
        if(this.allowdedIn(tab)){
          configuredItems.foreach(list.add)
        }
      }
    }, "misc")

    registerItem(new item.Tablet(misc), Constants.ItemName.Tablet)
    registerItem(new item.Drone(misc), Constants.ItemName.Drone)
    registerItem(new item.Present(misc), Constants.ItemName.Present)
  }

  private def newItem[T <: item.Delegator](delegator: T, name: String): T = {
    delegator.setUnlocalizedName("oc." + name)
    GameData.register_impl(delegator.setRegistryName(new ResourceLocation(Settings.resourceDomain, name)))
    delegator
  }
}
