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
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.server.machine.luac.LuaStateFactory
import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
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
          instance.setUnlocalizedName("oc." + id)
          instance.setRegistryName(id)
          GameData.register_impl(instance)
          OpenComputers.proxy.registerModel(instance, id)

          val item : Item = new common.block.Item(instance)
          item.setUnlocalizedName("oc." + id)
          item.setRegistryName(id)
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
      case block: ItemBlock => block.getBlock
      case item => item
    })

  // ----------------------------------------------------------------------- //

  val registeredItems: ArrayBuffer[ItemStack] = mutable.ArrayBuffer.empty[ItemStack]

  override def registerFloppy(name: String, color: EnumDyeColor, factory: Callable[FileSystem], doRecipeCycling: Boolean): ItemStack = {
    val stack = Loot.registerLootDisk(name, color, factory, doRecipeCycling)

    registeredItems += stack

    stack.copy()
  }

  override def registerEEPROM(name: String, code: Array[Byte], data: Array[Byte], readonly: Boolean): ItemStack = {
    val nbt = new NBTTagCompound()
    if (name != null) {
      nbt.setString(Settings.namespace + "label", name.trim.take(24))
    }
    if (code != null) {
      nbt.setByteArray(Settings.namespace + "eeprom", code.take(Settings.get.eepromSize))
    }
    if (data != null) {
      nbt.setByteArray(Settings.namespace + "userdata", data.take(Settings.get.eepromDataSize))
    }
    nbt.setBoolean(Settings.namespace + "readonly", readonly)

    val stackNbt = new NBTTagCompound()
    stackNbt.setTag(Settings.namespace + "data", nbt)

    val stack = get(Constants.ItemName.EEPROM).createItemStack(1)
    stack.setTagCompound(stackNbt)

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

    Recipes.addSubItem(new item.CuttingWire(materials), Constants.ItemName.CuttingWire, "oc:materialCuttingWire")
    Recipes.addSubItem(new item.Acid(materials), Constants.ItemName.Acid, "oc:materialAcid")
    Recipes.addSubItem(new item.RawCircuitBoard(materials), Constants.ItemName.RawCircuitBoard, "oc:materialCircuitBoardRaw")
    Recipes.addSubItem(new item.CircuitBoard(materials), Constants.ItemName.CircuitBoard, "oc:materialCircuitBoard")
    Recipes.addSubItem(new item.PrintedCircuitBoard(materials), Constants.ItemName.PrintedCircuitBoard, "oc:materialCircuitBoardPrinted")
    Recipes.addSubItem(new item.CardBase(materials), Constants.ItemName.Card, "oc:materialCard")
    Recipes.addSubItem(new item.Transistor(materials), Constants.ItemName.Transistor, "oc:materialTransistor")
    Recipes.addSubItem(new item.Microchip(materials, Tier.One), Constants.ItemName.ChipTier1, "oc:circuitChip1")
    Recipes.addSubItem(new item.Microchip(materials, Tier.Two), Constants.ItemName.ChipTier2, "oc:circuitChip2")
    Recipes.addSubItem(new item.Microchip(materials, Tier.Three), Constants.ItemName.ChipTier3, "oc:circuitChip3")
    Recipes.addSubItem(new item.ALU(materials), Constants.ItemName.Alu, "oc:materialALU")
    Recipes.addSubItem(new item.ControlUnit(materials), Constants.ItemName.ControlUnit, "oc:materialCU")
    Recipes.addSubItem(new item.Disk(materials), Constants.ItemName.Disk, "oc:materialDisk")
    Recipes.addSubItem(new item.Interweb(materials), Constants.ItemName.Interweb, "oc:materialInterweb")
    Recipes.addSubItem(new item.ButtonGroup(materials), Constants.ItemName.ButtonGroup, "oc:materialButtonGroup")
    Recipes.addSubItem(new item.ArrowKeys(materials), Constants.ItemName.ArrowKeys, "oc:materialArrowKey")
    Recipes.addSubItem(new item.NumPad(materials), Constants.ItemName.NumPad, "oc:materialNumPad")

    Recipes.addSubItem(new item.TabletCase(materials, Tier.One), Constants.ItemName.TabletCaseTier1, "oc:tabletCase1")
    Recipes.addSubItem(new item.TabletCase(materials, Tier.Two), Constants.ItemName.TabletCaseTier2, "oc:tabletCase2")
    registerItem(new item.TabletCase(materials, Tier.Four), Constants.ItemName.TabletCaseCreative)
    Recipes.addSubItem(new item.MicrocontrollerCase(materials, Tier.One), Constants.ItemName.MicrocontrollerCaseTier1, "oc:microcontrollerCase1")
    Recipes.addSubItem(new item.MicrocontrollerCase(materials, Tier.Two), Constants.ItemName.MicrocontrollerCaseTier2, "oc:microcontrollerCase2")
    registerItem(new item.MicrocontrollerCase(materials, Tier.Four), Constants.ItemName.MicrocontrollerCaseCreative)
    Recipes.addSubItem(new item.DroneCase(materials, Tier.One), Constants.ItemName.DroneCaseTier1, "oc:droneCase1")
    Recipes.addSubItem(new item.DroneCase(materials, Tier.Two), Constants.ItemName.DroneCaseTier2, "oc:droneCase2")
    registerItem(new item.DroneCase(materials, Tier.Four), Constants.ItemName.DroneCaseCreative)

    Recipes.addSubItem(new item.InkCartridgeEmpty(materials), Constants.ItemName.InkCartridgeEmpty, "oc:inkCartridgeEmpty")
    Recipes.addSubItem(new item.InkCartridge(materials), Constants.ItemName.InkCartridge, "oc:inkCartridge")
    Recipes.addSubItem(new item.Chamelium(materials), Constants.ItemName.Chamelium, "oc:chamelium")

    registerItem(new item.DiamondChip(materials), Constants.ItemName.DiamondChip)
  }

  // All kinds of tools.
  private def initTools(): Unit = {
    val tools = newItem(new item.Delegator(), "tool")

    Recipes.addSubItem(new item.Analyzer(tools), Constants.ItemName.Analyzer, "oc:analyzer")
    registerItem(new item.Debugger(tools), Constants.ItemName.Debugger)
    Recipes.addSubItem(new item.Terminal(tools), Constants.ItemName.Terminal, "oc:terminal")
    Recipes.addSubItem(new item.TexturePicker(tools), Constants.ItemName.TexturePicker, "oc:texturePicker")
    Recipes.addSubItem(new item.Manual(tools), Constants.ItemName.Manual, "oc:manual")
    Recipes.addItem(new item.Wrench(), Constants.ItemName.Wrench, "oc:wrench")

    // 1.5.11
    Recipes.addItem(new item.HoverBoots(), Constants.ItemName.HoverBoots, "oc:hoverBoots")

    // 1.5.18
    Recipes.addSubItem(new item.Nanomachines(tools), Constants.ItemName.Nanomachines, "oc:nanomachines")
  }

  // General purpose components.
  private def initComponents(): Unit = {
    val components = newItem(new item.Delegator(), "component")

    Recipes.addSubItem(new item.CPU(components, Tier.One), Constants.ItemName.CPUTier1, "oc:cpu1")
    Recipes.addSubItem(new item.CPU(components, Tier.Two), Constants.ItemName.CPUTier2, "oc:cpu2")
    Recipes.addSubItem(new item.CPU(components, Tier.Three), Constants.ItemName.CPUTier3, "oc:cpu3")

    Recipes.addSubItem(new item.ComponentBus(components, Tier.One), Constants.ItemName.ComponentBusTier1, "oc:componentBus1")
    Recipes.addSubItem(new item.ComponentBus(components, Tier.Two), Constants.ItemName.ComponentBusTier2, "oc:componentBus2")
    Recipes.addSubItem(new item.ComponentBus(components, Tier.Three), Constants.ItemName.ComponentBusTier3, "oc:componentBus3")

    Recipes.addSubItem(new item.Memory(components, Tier.One), Constants.ItemName.RAMTier1, "oc:ram1")
    Recipes.addSubItem(new item.Memory(components, Tier.Two), Constants.ItemName.RAMTier2, "oc:ram2")
    Recipes.addSubItem(new item.Memory(components, Tier.Three), Constants.ItemName.RAMTier3, "oc:ram3")
    Recipes.addSubItem(new item.Memory(components, Tier.Four), Constants.ItemName.RAMTier4, "oc:ram4")
    Recipes.addSubItem(new item.Memory(components, Tier.Five), Constants.ItemName.RAMTier5, "oc:ram5")
    Recipes.addSubItem(new item.Memory(components, Tier.Six), Constants.ItemName.RAMTier6, "oc:ram6")

    registerItem(new item.Server(components, Tier.Four), Constants.ItemName.ServerCreative)
    Recipes.addSubItem(new item.Server(components, Tier.One), Constants.ItemName.ServerTier1, "oc:server1")
    Recipes.addSubItem(new item.Server(components, Tier.Two), Constants.ItemName.ServerTier2, "oc:server2")
    Recipes.addSubItem(new item.Server(components, Tier.Three), Constants.ItemName.ServerTier3, "oc:server3")

    // 1.5.10
    Recipes.addSubItem(new item.APU(components, Tier.One), Constants.ItemName.APUTier1, "oc:apu1")
    Recipes.addSubItem(new item.APU(components, Tier.Two), Constants.ItemName.APUTier2, "oc:apu2")

    // 1.5.12
    registerItem(new item.APU(components, Tier.Three), Constants.ItemName.APUCreative)

    // 1.6
    Recipes.addSubItem(new item.TerminalServer(components), Constants.ItemName.TerminalServer, "oc:terminalServer")
    Recipes.addSubItem(new item.DiskDriveMountable(components), Constants.ItemName.DiskDriveMountable, "oc:diskDriveMountable")
  }

  // Card components.
  private def initCards(): Unit = {
    val cards = newItem(new item.Delegator(), "card")

    registerItem(new item.DebugCard(cards), Constants.ItemName.DebugCard)
    Recipes.addSubItem(new item.GraphicsCard(cards, Tier.One), Constants.ItemName.GraphicsCardTier1, "oc:graphicsCard1")
    Recipes.addSubItem(new item.GraphicsCard(cards, Tier.Two), Constants.ItemName.GraphicsCardTier2, "oc:graphicsCard2")
    Recipes.addSubItem(new item.GraphicsCard(cards, Tier.Three), Constants.ItemName.GraphicsCardTier3, "oc:graphicsCard3")
    Recipes.addSubItem(new item.RedstoneCard(cards, Tier.One), Constants.ItemName.RedstoneCardTier1, "oc:redstoneCard1")
    Recipes.addSubItem(new item.RedstoneCard(cards, Tier.Two), Constants.ItemName.RedstoneCardTier2, "oc:redstoneCard2")
    Recipes.addSubItem(new item.NetworkCard(cards), Constants.ItemName.NetworkCard, "oc:lanCard")
    Recipes.addSubItem(new item.WirelessNetworkCard(cards, Tier.Two), Constants.ItemName.WirelessNetworkCardTier2, "oc:wlanCard2")
    Recipes.addSubItem(new item.InternetCard(cards), Constants.ItemName.InternetCard, "oc:internetCard")
    Recipes.addSubItem(new item.LinkedCard(cards), Constants.ItemName.LinkedCard, "oc:linkedCard")

    // 1.5.13
    Recipes.addSubItem(new item.DataCard(cards, Tier.One), Constants.ItemName.DataCardTier1, "oc:dataCard1")

    // 1.5.15
    Recipes.addSubItem(new item.DataCard(cards, Tier.Two), Constants.ItemName.DataCardTier2, "oc:dataCard2")
    Recipes.addSubItem(new item.DataCard(cards, Tier.Three), Constants.ItemName.DataCardTier3, "oc:dataCard3")
  }

  // Upgrade components.
  private def initUpgrades(): Unit = {
    val upgrades = newItem(new item.Delegator(), "upgrade")

    Recipes.addSubItem(new item.UpgradeAngel(upgrades), Constants.ItemName.AngelUpgrade, "oc:angelUpgrade")
    Recipes.addSubItem(new item.UpgradeBattery(upgrades, Tier.One), Constants.ItemName.BatteryUpgradeTier1, "oc:batteryUpgrade1")
    Recipes.addSubItem(new item.UpgradeBattery(upgrades, Tier.Two), Constants.ItemName.BatteryUpgradeTier2, "oc:batteryUpgrade2")
    Recipes.addSubItem(new item.UpgradeBattery(upgrades, Tier.Three), Constants.ItemName.BatteryUpgradeTier3, "oc:batteryUpgrade3")
    Recipes.addSubItem(new item.UpgradeChunkloader(upgrades), Constants.ItemName.ChunkloaderUpgrade, "oc:chunkloaderUpgrade")
    Recipes.addSubItem(new item.UpgradeContainerCard(upgrades, Tier.One), Constants.ItemName.CardContainerTier1, "oc:cardContainer1")
    Recipes.addSubItem(new item.UpgradeContainerCard(upgrades, Tier.Two), Constants.ItemName.CardContainerTier2, "oc:cardContainer2")
    Recipes.addSubItem(new item.UpgradeContainerCard(upgrades, Tier.Three), Constants.ItemName.CardContainerTier3, "oc:cardContainer3")
    Recipes.addSubItem(new item.UpgradeContainerUpgrade(upgrades, Tier.One), Constants.ItemName.UpgradeContainerTier1, "oc:upgradeContainer1")
    Recipes.addSubItem(new item.UpgradeContainerUpgrade(upgrades, Tier.Two), Constants.ItemName.UpgradeContainerTier2, "oc:upgradeContainer2")
    Recipes.addSubItem(new item.UpgradeContainerUpgrade(upgrades, Tier.Three), Constants.ItemName.UpgradeContainerTier3, "oc:upgradeContainer3")
    Recipes.addSubItem(new item.UpgradeCrafting(upgrades), Constants.ItemName.CraftingUpgrade, "oc:craftingUpgrade")
    Recipes.addSubItem(new item.UpgradeDatabase(upgrades, Tier.One), Constants.ItemName.DatabaseUpgradeTier1, "oc:databaseUpgrade1")
    Recipes.addSubItem(new item.UpgradeDatabase(upgrades, Tier.Two), Constants.ItemName.DatabaseUpgradeTier2, "oc:databaseUpgrade2")
    Recipes.addSubItem(new item.UpgradeDatabase(upgrades, Tier.Three), Constants.ItemName.DatabaseUpgradeTier3, "oc:databaseUpgrade3")
    Recipes.addSubItem(new item.UpgradeExperience(upgrades), Constants.ItemName.ExperienceUpgrade, "oc:experienceUpgrade")
    Recipes.addSubItem(new item.UpgradeGenerator(upgrades), Constants.ItemName.GeneratorUpgrade, "oc:generatorUpgrade")
    Recipes.addSubItem(new item.UpgradeInventory(upgrades), Constants.ItemName.InventoryUpgrade, "oc:inventoryUpgrade")
    Recipes.addSubItem(new item.UpgradeInventoryController(upgrades), Constants.ItemName.InventoryControllerUpgrade, "oc:inventoryControllerUpgrade")
    Recipes.addSubItem(new item.UpgradeNavigation(upgrades), Constants.ItemName.NavigationUpgrade, "oc:navigationUpgrade")
    Recipes.addSubItem(new item.UpgradePiston(upgrades), Constants.ItemName.PistonUpgrade, "oc:pistonUpgrade")
    Recipes.addSubItem(new item.UpgradeSign(upgrades), Constants.ItemName.SignUpgrade, "oc:signUpgrade")
    Recipes.addSubItem(new item.UpgradeSolarGenerator(upgrades), Constants.ItemName.SolarGeneratorUpgrade, "oc:solarGeneratorUpgrade")
    Recipes.addSubItem(new item.UpgradeTank(upgrades), Constants.ItemName.TankUpgrade, "oc:tankUpgrade")
    Recipes.addSubItem(new item.UpgradeTankController(upgrades), Constants.ItemName.TankControllerUpgrade, "oc:tankControllerUpgrade")
    Recipes.addSubItem(new item.UpgradeTractorBeam(upgrades), Constants.ItemName.TractorBeamUpgrade, "oc:tractorBeamUpgrade")
    Recipes.addSubItem(new item.UpgradeLeash(upgrades), Constants.ItemName.LeashUpgrade, "oc:leashUpgrade")

    // 1.5.8
    Recipes.addSubItem(new item.UpgradeHover(upgrades, Tier.One), Constants.ItemName.HoverUpgradeTier1, "oc:hoverUpgrade1")
    Recipes.addSubItem(new item.UpgradeHover(upgrades, Tier.Two), Constants.ItemName.HoverUpgradeTier2, "oc:hoverUpgrade2")

    // 1.6
    Recipes.addSubItem(new item.UpgradeTrading(upgrades), Constants.ItemName.TradingUpgrade, "oc:tradingUpgrade")
    Recipes.addSubItem(new item.UpgradeMF(upgrades), Constants.ItemName.MFU, "oc:mfu")

    // 1.7.2
    Recipes.addSubItem(new item.WirelessNetworkCard(upgrades, Tier.One), Constants.ItemName.WirelessNetworkCardTier1, "oc:wlanCard1")
    registerItem(new item.ComponentBus(upgrades, Tier.Four), Constants.ItemName.ComponentBusCreative)

    // 1.8
    Recipes.addSubItem(new item.UpgradeStickyPiston(upgrades), Constants.ItemName.StickyPistonUpgrade, "oc:stickyPistonUpgrade")
  }

  // Storage media of all kinds.
  private def initStorage(): Unit = {
    val storage = newItem(new item.Delegator(), "storage")

    Recipes.addSubItem(new item.EEPROM(storage), Constants.ItemName.EEPROM, "oc:eeprom")
    Recipes.addSubItem(new item.FloppyDisk(storage), Constants.ItemName.Floppy, "oc:floppy")
    Recipes.addSubItem(new item.HardDiskDrive(storage, Tier.One), Constants.ItemName.HDDTier1, "oc:hdd1")
    Recipes.addSubItem(new item.HardDiskDrive(storage, Tier.Two), Constants.ItemName.HDDTier2, "oc:hdd2")
    Recipes.addSubItem(new item.HardDiskDrive(storage, Tier.Three), Constants.ItemName.HDDTier3, "oc:hdd3")

    val luaBios = {
      val code = new Array[Byte](4 * 1024)
      val count = OpenComputers.getClass.getResourceAsStream(Settings.scriptPath + "bios.lua").read(code)
      registerEEPROM("EEPROM (Lua BIOS)", code.take(count), null, readonly = false)
    }
    Recipes.addStack(luaBios, Constants.ItemName.LuaBios)

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

      override def getSubItems(tab: CreativeTabs, list: NonNullList[ItemStack]): Unit = {
        super.getSubItems(tab, list)
        if(isInCreativeTab(tab)){
          configuredItems.foreach(list.add)
        }
      }
    }, "misc")

    registerItem(new item.Tablet(misc), Constants.ItemName.Tablet)
    registerItem(new item.Drone(misc), Constants.ItemName.Drone)
    registerItem(new item.Present(misc), Constants.ItemName.Present)
  }

  private def newItem[T <: Item](item: T, name: String): T = {
    item.setUnlocalizedName("oc." + name)
    GameData.register_impl(item.setRegistryName(new ResourceLocation(Settings.resourceDomain, name)))
    item
  }
}
