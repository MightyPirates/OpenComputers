package li.cil.oc.common.init

import java.util.concurrent.Callable

import cpw.mods.fml.common.registry.GameRegistry
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
import li.cil.oc.common.item.UpgradeLeash
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.common.item.data.HoverBootsData
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.common.item.traits.Delegate
import li.cil.oc.common.item.traits.SimpleItem
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.integration.Mods
import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

import scala.collection.mutable

object Items extends ItemAPI {
  val descriptors = mutable.Map.empty[String, ItemInfo]

  val names = mutable.Map.empty[Any, String]

  override def get(name: String): ItemInfo = descriptors.get(name).orNull

  override def get(stack: ItemStack) = names.get(getBlockOrItem(stack)) match {
    case Some(name) => get(name)
    case _ => null
  }

  def registerBlock[T <: Block](instance: T, id: String) = {
    instance match {
      case simple: SimpleBlock =>
        instance.setBlockName("oc." + id)
        GameRegistry.registerBlock(simple, classOf[common.block.Item], id)
      case _ =>
    }
    descriptors += id -> new ItemInfo {
      override def name = id

      override def block = instance

      override def item = null

      override def createItemStack(size: Int) = instance match {
        case simple: SimpleBlock => simple.createItemStack(size)
        case _ => new ItemStack(instance, size)
      }
    }
    names += instance -> id
    instance
  }

  def registerItem[T <: Delegate](delegate: T, id: String) = {
    descriptors += id -> new ItemInfo {
      override def name = id

      override def block = null

      override def item = delegate.parent

      override def createItemStack(size: Int) = delegate.createItemStack(size)
    }
    names += delegate -> id
    delegate
  }

  def registerItem(instance: Item, id: String) = {
    instance match {
      case simple: SimpleItem =>
        simple.setUnlocalizedName("oc." + id)
        GameRegistry.registerItem(simple, id)
      case _ =>
    }
    descriptors += id -> new ItemInfo {
      override def name = id

      override def block = null

      override def item = instance

      override def createItemStack(size: Int) = instance match {
        case simple: SimpleItem => simple.createItemStack(size)
        case _ => new ItemStack(instance, size)
      }
    }
    names += instance -> id
    instance
  }

  def registerStack(stack: ItemStack, id: String) = {
    val immutableStack = stack.copy()
    descriptors += id -> new ItemInfo {
      override def name = id

      override def block = null

      override def createItemStack(size: Int): ItemStack = {
        val copy = immutableStack.copy()
        copy.stackSize = size
        copy
      }

      override def item = immutableStack.getItem
    }
    stack
  }

  private def getBlockOrItem(stack: ItemStack): Any =
    if (stack == null) null
    else Delegator.subItem(stack).getOrElse(stack.getItem match {
      case block: ItemBlock => block.field_150939_a
      case item => item
    })

  // ----------------------------------------------------------------------- //

  val registeredItems = mutable.ArrayBuffer.empty[ItemStack]

  override def registerFloppy(name: String, color: Int, factory: Callable[FileSystem]): ItemStack = {
    val stack = Loot.registerLootDisk(name, color, factory)

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

  // Nobody should use this anyway, since it's internal, but IIRC some people do, so let's be nice...
  // TODO remove in OC 1.6
  /**
   * @deprecated use <tt>api.Items.get("openOS").createItemStack(amount)</tt> instead.
   */
  @Deprecated
  def createOpenOS(amount: Int = 1) = {
    get(Constants.ItemName.OpenOS).createItemStack(amount)
  }

  // Nobody should use this anyway, since it's internal, but IIRC some people do, so let's be nice...
  // TODO remove in OC 1.6
  /**
   * @deprecated use <tt>api.Items.get("luaBios").createItemStack(amount)</tt> instead.
   */
  @Deprecated
  def createLuaBios(amount: Int = 1) = {
    get(Constants.ItemName.LuaBios).createItemStack(amount)
  }

  def createConfiguredDrone() = {
    val data = new DroneData()

    data.name = "Crecopter"
    data.tier = Tier.Four
    data.storedEnergy = Settings.get.bufferDrone.toInt
    data.components = Array(
      get(Constants.ItemName.InventoryUpgrade).createItemStack(1),
      get(Constants.ItemName.InventoryUpgrade).createItemStack(1),
      get(Constants.ItemName.InventoryControllerUpgrade).createItemStack(1),
      get(Constants.ItemName.TankUpgrade).createItemStack(1),
      get(Constants.ItemName.TankControllerUpgrade).createItemStack(1),
      get(Constants.ItemName.LeashUpgrade).createItemStack(1),

      get(Constants.ItemName.WirelessNetworkCard).createItemStack(1),

      get(Constants.ItemName.CPUTier3).createItemStack(1),
      get(Constants.ItemName.RAMTier6).createItemStack(1),
      get(Constants.ItemName.RAMTier6).createItemStack(1)
    )

    data.createItemStack()
  }

  def createConfiguredMicrocontroller() = {
    val data = new MicrocontrollerData()

    data.tier = Tier.Four
    data.storedEnergy = Settings.get.bufferMicrocontroller.toInt
    data.components = Array(
      get(Constants.ItemName.SignUpgrade).createItemStack(1),
      get(Constants.ItemName.PistonUpgrade).createItemStack(1),

      get(Constants.ItemName.RedstoneCardTier2).createItemStack(1),
      get(Constants.ItemName.WirelessNetworkCard).createItemStack(1),

      get(Constants.ItemName.CPUTier3).createItemStack(1),
      get(Constants.ItemName.RAMTier6).createItemStack(1),
      get(Constants.ItemName.RAMTier6).createItemStack(1)
    )

    data.createItemStack()
  }

  def createConfiguredRobot() = {
    val data = new RobotData()

    data.name = "Creatix"
    data.tier = Tier.Four
    data.robotEnergy = Settings.get.bufferRobot.toInt
    data.totalEnergy = data.robotEnergy
    data.components = Array(
      get(Constants.BlockName.ScreenTier1).createItemStack(1),
      get(Constants.BlockName.Keyboard).createItemStack(1),
      get(Constants.ItemName.InventoryUpgrade).createItemStack(1),
      get(Constants.ItemName.InventoryUpgrade).createItemStack(1),
      get(Constants.ItemName.InventoryUpgrade).createItemStack(1),
      get(Constants.ItemName.InventoryUpgrade).createItemStack(1),
      get(Constants.ItemName.InventoryControllerUpgrade).createItemStack(1),
      get(Constants.ItemName.TankUpgrade).createItemStack(1),
      get(Constants.ItemName.TankControllerUpgrade).createItemStack(1),
      get(Constants.ItemName.CraftingUpgrade).createItemStack(1),

      get(Constants.ItemName.GraphicsCardTier3).createItemStack(1),
      get(Constants.ItemName.RedstoneCardTier2).createItemStack(1),
      get(Constants.ItemName.WirelessNetworkCard).createItemStack(1),
      get(Constants.ItemName.InternetCard).createItemStack(1),

      get(Constants.ItemName.CPUTier3).createItemStack(1),
      get(Constants.ItemName.RAMTier6).createItemStack(1),
      get(Constants.ItemName.RAMTier6).createItemStack(1),

      get(Constants.ItemName.LuaBios).createItemStack(1),
      get(Constants.ItemName.OpenOS).createItemStack(1),
      get(Constants.ItemName.HDDTier3).createItemStack(1)
    )
    data.containers = Array(
      get(Constants.ItemName.CardContainerTier3).createItemStack(1),
      get(Constants.ItemName.UpgradeContainerTier3).createItemStack(1),
      get(Constants.BlockName.DiskDrive).createItemStack(1)
    )

    data.createItemStack()
  }

  def createConfiguredTablet() = {
    val data = new TabletData()

    data.tier = Tier.Four
    data.energy = Settings.get.bufferTablet
    data.maxEnergy = data.energy
    data.items = Array(
      Option(get(Constants.BlockName.ScreenTier1).createItemStack(1)),
      Option(get(Constants.BlockName.Keyboard).createItemStack(1)),

      Option(get(Constants.ItemName.SignUpgrade).createItemStack(1)),
      Option(get(Constants.ItemName.PistonUpgrade).createItemStack(1)),
      Option(get(Constants.BlockName.Geolyzer).createItemStack(1)),
      Option(get(Constants.ItemName.NavigationUpgrade).createItemStack(1)),

      Option(get(Constants.ItemName.GraphicsCardTier2).createItemStack(1)),
      Option(get(Constants.ItemName.RedstoneCardTier2).createItemStack(1)),
      Option(get(Constants.ItemName.WirelessNetworkCard).createItemStack(1)),

      Option(get(Constants.ItemName.CPUTier3).createItemStack(1)),
      Option(get(Constants.ItemName.RAMTier6).createItemStack(1)),
      Option(get(Constants.ItemName.RAMTier6).createItemStack(1)),

      Option(get(Constants.ItemName.LuaBios).createItemStack(1)),
      Option(get(Constants.ItemName.HDDTier3).createItemStack(1))
    ).padTo(32, None)
    data.items(31) = Option(get(Constants.ItemName.OpenOS).createItemStack(1))
    data.container = Option(get(Constants.BlockName.DiskDrive).createItemStack(1))

    val stack = get(Constants.ItemName.Tablet).createItemStack(1)
    data.save(stack)

    stack
  }

  def createChargedHoverBoots() = {
    val data = new HoverBootsData()
    data.charge = Settings.get.bufferHoverBoots

    val stack = get(Constants.ItemName.HoverBoots).createItemStack(1)
    data.save(stack)

    stack
  }

  // ----------------------------------------------------------------------- //
  // Crafting

  def init() {
    val multi = new item.Delegator() {
      def additionalItems = Array(
        createConfiguredDrone(),
        createConfiguredMicrocontroller(),
        createConfiguredRobot(),
        createConfiguredTablet(),
        createChargedHoverBoots()
      ) ++ Loot.disksForClient ++ registeredItems

      override def getSubItems(item: Item, tab: CreativeTabs, list: java.util.List[_]) {
        // Workaround for MC's untyped lists...
        def add[T](list: java.util.List[T], value: Any) = list.add(value.asInstanceOf[T])
        super.getSubItems(item, tab, list)
        additionalItems.foreach(add(list, _))
      }
    }

    GameRegistry.registerItem(multi, "item")

    Recipes.addSubItem(new item.Analyzer(multi), Constants.ItemName.Analyzer, "oc:analyzer")

    Recipes.addSubItem(new item.Memory(multi, Tier.One), Constants.ItemName.RAMTier1, "oc:ram1")
    Recipes.addSubItem(new item.Memory(multi, Tier.Three), Constants.ItemName.RAMTier3, "oc:ram3")
    Recipes.addSubItem(new item.Memory(multi, Tier.Four), Constants.ItemName.RAMTier4, "oc:ram4")

    Recipes.addSubItem(new item.FloppyDisk(multi), Constants.ItemName.Floppy, "oc:floppy")
    Recipes.addSubItem(new item.HardDiskDrive(multi, Tier.One), Constants.ItemName.HDDTier1, "oc:hdd1")
    Recipes.addSubItem(new item.HardDiskDrive(multi, Tier.Two), Constants.ItemName.HDDTier2, "oc:hdd2")
    Recipes.addSubItem(new item.HardDiskDrive(multi, Tier.Three), Constants.ItemName.HDDTier3, "oc:hdd3")

    Recipes.addSubItem(new item.GraphicsCard(multi, Tier.One), Constants.ItemName.GraphicsCardTier1, "oc:graphicsCard1")
    Recipes.addSubItem(new item.GraphicsCard(multi, Tier.Two), Constants.ItemName.GraphicsCardTier2, "oc:graphicsCard2")
    Recipes.addSubItem(new item.GraphicsCard(multi, Tier.Three), Constants.ItemName.GraphicsCardTier3, "oc:graphicsCard3")
    Recipes.addSubItem(new item.NetworkCard(multi), Constants.ItemName.NetworkCard, "oc:lanCard")
    Recipes.addSubItem(new item.RedstoneCard(multi, Tier.Two), Constants.ItemName.RedstoneCardTier2, "oc:redstoneCard2")
    Recipes.addSubItem(new item.WirelessNetworkCard(multi), Constants.ItemName.WirelessNetworkCard, "oc:wlanCard")

    Recipes.addSubItem(new item.UpgradeCrafting(multi), Constants.ItemName.CraftingUpgrade, "oc:craftingUpgrade")
    Recipes.addSubItem(new item.UpgradeGenerator(multi), Constants.ItemName.GeneratorUpgrade, "oc:generatorUpgrade")

    registerItem(new item.IronNugget(multi), Constants.ItemName.IronNugget)

    Recipes.addSubItem(new item.CuttingWire(multi), Constants.ItemName.CuttingWire, "oc:materialCuttingWire")
    Recipes.addSubItem(new item.Acid(multi), Constants.ItemName.Acid, "oc:materialAcid")
    Recipes.addSubItem(new item.Disk(multi), Constants.ItemName.Disk, "oc:materialDisk")

    Recipes.addSubItem(new item.ButtonGroup(multi), Constants.ItemName.ButtonGroup, "oc:materialButtonGroup")
    Recipes.addSubItem(new item.ArrowKeys(multi), Constants.ItemName.ArrowKeys, "oc:materialArrowKey")
    Recipes.addSubItem(new item.NumPad(multi), Constants.ItemName.NumPad, "oc:materialNumPad")

    Recipes.addSubItem(new item.Transistor(multi), Constants.ItemName.Transistor, "oc:materialTransistor")
    Recipes.addSubItem(new item.Microchip(multi, Tier.One), Constants.ItemName.ChipTier1, "oc:circuitChip1")
    Recipes.addSubItem(new item.Microchip(multi, Tier.Two), Constants.ItemName.ChipTier2, "oc:circuitChip2")
    Recipes.addSubItem(new item.Microchip(multi, Tier.Three), Constants.ItemName.ChipTier3, "oc:circuitChip3")
    Recipes.addSubItem(new item.ALU(multi), Constants.ItemName.Alu, "oc:materialALU")
    Recipes.addSubItem(new item.ControlUnit(multi), Constants.ItemName.ControlUnit, "oc:materialCU")
    Recipes.addSubItem(new item.CPU(multi, Tier.One), Constants.ItemName.CPUTier1, "oc:cpu1")

    Recipes.addSubItem(new item.RawCircuitBoard(multi), Constants.ItemName.RawCircuitBoard, "oc:materialCircuitBoardRaw")
    Recipes.addSubItem(new item.CircuitBoard(multi), Constants.ItemName.CircuitBoard, "oc:materialCircuitBoard")
    Recipes.addSubItem(new item.PrintedCircuitBoard(multi), Constants.ItemName.PrintedCircuitBoard, "oc:materialCircuitBoardPrinted")
    Recipes.addSubItem(new item.CardBase(multi), Constants.ItemName.Card, "oc:materialCard")

    // v1.1.0
    Recipes.addSubItem(new item.UpgradeSolarGenerator(multi), Constants.ItemName.SolarGeneratorUpgrade, "oc:solarGeneratorUpgrade")
    Recipes.addSubItem(new item.UpgradeSign(multi), Constants.ItemName.SignUpgrade, "oc:signUpgrade")
    Recipes.addSubItem(new item.UpgradeNavigation(multi), Constants.ItemName.NavigationUpgrade, "oc:navigationUpgrade")

    // Always create, to avoid shifting IDs.
    val abstractBus = new item.AbstractBusCard(multi)
    if (Mods.StargateTech2.isAvailable) {
      Recipes.addSubItem(abstractBus, Constants.ItemName.AbstractBusCard, "oc:abstractBusCard")
    }

    Recipes.addSubItem(new item.Memory(multi, Tier.Five), Constants.ItemName.RAMTier5, "oc:ram5")
    Recipes.addSubItem(new item.Memory(multi, Tier.Six), Constants.ItemName.RAMTier6, "oc:ram6")

    // v1.2.0
    Recipes.addSubItem(new item.Server(multi, Tier.Three), Constants.ItemName.ServerTier3, "oc:server3")
    Recipes.addSubItem(new item.Terminal(multi), Constants.ItemName.Terminal, "oc:terminal")
    Recipes.addSubItem(new item.CPU(multi, Tier.Two), Constants.ItemName.CPUTier2, "oc:cpu2")
    Recipes.addSubItem(new item.CPU(multi, Tier.Three), Constants.ItemName.CPUTier3, "oc:cpu3")
    Recipes.addSubItem(new item.InternetCard(multi), Constants.ItemName.InternetCard, "oc:internetCard")
    Recipes.addSubItem(new item.Server(multi, Tier.One), Constants.ItemName.ServerTier1, "oc:server1")
    Recipes.addSubItem(new item.Server(multi, Tier.Two), Constants.ItemName.ServerTier2, "oc:server2")

    // v1.2.3
    registerItem(new item.FloppyDisk(multi) {
      showInItemList = false
    }, Constants.ItemName.LootDisk)

    // v1.2.6
    Recipes.addSubItem(new item.Interweb(multi), Constants.ItemName.Interweb, "oc:materialInterweb")
    Recipes.addSubItem(new item.UpgradeAngel(multi), Constants.ItemName.AngelUpgrade, "oc:angelUpgrade")
    Recipes.addSubItem(new item.Memory(multi, Tier.Two), Constants.ItemName.RAMTier2, "oc:ram2")

    // v1.3.0
    Recipes.addSubItem(new item.LinkedCard(multi), Constants.ItemName.LinkedCard, "oc:linkedCard")
    Recipes.addSubItem(new item.UpgradeExperience(multi), Constants.ItemName.ExperienceUpgrade, "oc:experienceUpgrade")
    Recipes.addSubItem(new item.UpgradeInventory(multi), Constants.ItemName.InventoryUpgrade, "oc:inventoryUpgrade")
    Recipes.addSubItem(new item.UpgradeContainerUpgrade(multi, Tier.One), Constants.ItemName.UpgradeContainerTier1, "oc:upgradeContainer1")
    Recipes.addSubItem(new item.UpgradeContainerUpgrade(multi, Tier.Two), Constants.ItemName.UpgradeContainerTier2, "oc:upgradeContainer2")
    Recipes.addSubItem(new item.UpgradeContainerUpgrade(multi, Tier.Three), Constants.ItemName.UpgradeContainerTier3, "oc:upgradeContainer3")
    Recipes.addSubItem(new item.UpgradeContainerCard(multi, Tier.One), Constants.ItemName.CardContainerTier1, "oc:cardContainer1")
    Recipes.addSubItem(new item.UpgradeContainerCard(multi, Tier.Two), Constants.ItemName.CardContainerTier2, "oc:cardContainer2")
    Recipes.addSubItem(new item.UpgradeContainerCard(multi, Tier.Three), Constants.ItemName.CardContainerTier3, "oc:cardContainer3")

    // Special case loot disk because this one's craftable and having it have
    // the same item damage would confuse NEI and the item costs computation.
    // UPDATE: screw that, keeping it for compatibility for now, but using recipe
    // below now (creating "normal" loot disk).
    new item.FloppyDisk(multi) {
      showInItemList = false

      override def createItemStack(amount: Int) = get(Constants.ItemName.OpenOS).createItemStack(1)

      override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
        if (player.isSneaking) get(Constants.ItemName.Floppy).createItemStack(1)
        else super.onItemRightClick(stack, world, player)
      }
    }

    Recipes.addSubItem(new item.UpgradeInventoryController(multi), Constants.ItemName.InventoryControllerUpgrade, "oc:inventoryControllerUpgrade")
    Recipes.addSubItem(new item.UpgradeChunkloader(multi), Constants.ItemName.ChunkloaderUpgrade, "oc:chunkloaderUpgrade")
    Recipes.addSubItem(new item.UpgradeBattery(multi, Tier.One), Constants.ItemName.BatteryUpgradeTier1, "oc:batteryUpgrade1")
    Recipes.addSubItem(new item.UpgradeBattery(multi, Tier.Two), Constants.ItemName.BatteryUpgradeTier2, "oc:batteryUpgrade2")
    Recipes.addSubItem(new item.UpgradeBattery(multi, Tier.Three), Constants.ItemName.BatteryUpgradeTier3, "oc:batteryUpgrade3")
    Recipes.addSubItem(new item.RedstoneCard(multi, Tier.One), Constants.ItemName.RedstoneCardTier1, "oc:redstoneCard1")

    // 1.3.2
    Recipes.addSubItem(new item.UpgradeTractorBeam(multi), Constants.ItemName.TractorBeamUpgrade, "oc:tractorBeamUpgrade")

    // 1.3.?
    registerItem(new item.Tablet(multi), Constants.ItemName.Tablet)

    // 1.3.2 (cont.)
    registerItem(new item.Server(multi, Tier.Four), Constants.ItemName.ServerCreative)

    // 1.3.3
    Recipes.addSubItem(new item.ComponentBus(multi, Tier.One), Constants.ItemName.ComponentBusTier1, "oc:componentBus1")
    Recipes.addSubItem(new item.ComponentBus(multi, Tier.Two), Constants.ItemName.ComponentBusTier2, "oc:componentBus2")
    Recipes.addSubItem(new item.ComponentBus(multi, Tier.Three), Constants.ItemName.ComponentBusTier3, "oc:componentBus3")
    registerItem(new item.DebugCard(multi), Constants.ItemName.DebugCard)

    // 1.3.5
    Recipes.addSubItem(new item.TabletCase(multi, Tier.One), Constants.ItemName.TabletCaseTier1, "oc:tabletCase1")
    Recipes.addSubItem(new item.UpgradePiston(multi), Constants.ItemName.PistonUpgrade, "oc:pistonUpgrade")
    Recipes.addSubItem(new item.UpgradeTank(multi), Constants.ItemName.TankUpgrade, "oc:tankUpgrade")
    Recipes.addSubItem(new item.UpgradeTankController(multi), Constants.ItemName.TankControllerUpgrade, "oc:tankControllerUpgrade")

    // 1.4.0
    Recipes.addSubItem(new item.UpgradeDatabase(multi, Tier.One), Constants.ItemName.DatabaseUpgradeTier1, "oc:databaseUpgrade1")
    Recipes.addSubItem(new item.UpgradeDatabase(multi, Tier.Two), Constants.ItemName.DatabaseUpgradeTier2, "oc:databaseUpgrade2")
    Recipes.addSubItem(new item.UpgradeDatabase(multi, Tier.Three), Constants.ItemName.DatabaseUpgradeTier3, "oc:databaseUpgrade3")
    registerItem(new item.Debugger(multi), Constants.ItemName.Debugger)

    // 1.4.2
    val eeprom = new item.EEPROM()
    Recipes.addItem(eeprom, Constants.ItemName.EEPROM, "oc:eeprom")
    val luaBios = {
      val code = new Array[Byte](4 * 1024)
      val count = OpenComputers.getClass.getResourceAsStream(Settings.scriptPath + "bios.lua").read(code)
      registerEEPROM("EEPROM (Lua BIOS)", code.take(count), null, readonly = false)
    }
    Recipes.addStack(luaBios, Constants.ItemName.LuaBios)

    Recipes.addSubItem(new item.MicrocontrollerCase(multi, Tier.One), Constants.ItemName.MicrocontrollerCaseTier1, "oc:microcontrollerCase1")

    // 1.4.3
    Recipes.addSubItem(new item.DroneCase(multi, Tier.One), Constants.ItemName.DroneCaseTier1, "oc:droneCase1")
    registerItem(new item.Drone(multi), Constants.ItemName.Drone)
    Recipes.addSubItem(new UpgradeLeash(multi), Constants.ItemName.LeashUpgrade, "oc:leashUpgrade")
    Recipes.addSubItem(new item.MicrocontrollerCase(multi, Tier.Two), Constants.ItemName.MicrocontrollerCaseTier2, "oc:microcontrollerCase2")
    Recipes.addSubItem(new item.DroneCase(multi, Tier.Two), Constants.ItemName.DroneCaseTier2, "oc:droneCase2")
    registerItem(new item.Present(multi), Constants.ItemName.Present)

    // Always create, to avoid shifting IDs.
    val worldSensorCard = new item.WorldSensorCard(multi)
    if (Mods.Galacticraft.isAvailable) {
      Recipes.addSubItem(worldSensorCard, Constants.ItemName.WorldSensorCard, "oc:worldSensorCard")
    }

    // 1.4.4
    registerItem(new item.MicrocontrollerCase(multi, Tier.Four), Constants.ItemName.MicrocontrollerCaseCreative)
    registerItem(new item.DroneCase(multi, Tier.Four), Constants.ItemName.DroneCaseCreative)

    // 1.4.7
    Recipes.addSubItem(new item.TabletCase(multi, Tier.Two), Constants.ItemName.TabletCaseTier2, "oc:tabletCase2")
    registerItem(new item.TabletCase(multi, Tier.Four), Constants.ItemName.TabletCaseCreative)

    // 1.5.4
    Recipes.addSubItem(new item.InkCartridgeEmpty(multi), Constants.ItemName.InkCartridgeEmpty, "oc:inkCartridgeEmpty")
    Recipes.addSubItem(new item.InkCartridge(multi), Constants.ItemName.InkCartridge, "oc:inkCartridge")
    Recipes.addSubItem(new item.Chamelium(multi), Constants.ItemName.Chamelium, "oc:chamelium")
    Recipes.addSubItem(new item.TexturePicker(multi), Constants.ItemName.TexturePicker, "oc:texturePicker")

    // 1.5.7
    Recipes.addSubItem(new item.Manual(multi), Constants.ItemName.Manual, "oc:manual", "craftingBook")
    Recipes.addItem(new item.Wrench(), Constants.ItemName.Wrench, "oc:wrench")

    // 1.5.8
    Recipes.addSubItem(new item.UpgradeHover(multi, Tier.One), Constants.ItemName.HoverUpgradeTier1, "oc:hoverUpgrade1")
    Recipes.addSubItem(new item.UpgradeHover(multi, Tier.Two), Constants.ItemName.HoverUpgradeTier2, "oc:hoverUpgrade2")

    // 1.5.10
    Recipes.addSubItem(new item.APU(multi, Tier.One), Constants.ItemName.APUTier1, "oc:apu1")
    Recipes.addSubItem(new item.APU(multi, Tier.Two), Constants.ItemName.APUTier2, "oc:apu2")

    // 1.5.11
    Recipes.addItem(new item.HoverBoots(), Constants.ItemName.HoverBoots, "oc:hoverBoots")

    // 1.5.12
    registerItem(new item.APU(multi, Tier.Three), Constants.ItemName.APUCreative)

    // 1.5.13
    Recipes.addSubItem(new item.DataCard(multi), Constants.ItemName.DataCard, "oc:dataCard")
  }
}
