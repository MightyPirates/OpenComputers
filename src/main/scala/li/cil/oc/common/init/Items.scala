package li.cil.oc.common.init

import java.util

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.detail.ItemAPI
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.common
import li.cil.oc.common.Loot
import li.cil.oc.common.Tier
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.common.item
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.SimpleItem
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.integration.Mods
import li.cil.oc.util.Color
import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.registry.GameRegistry

import scala.collection.mutable

object Items extends ItemAPI {
  private val descriptors = mutable.Map.empty[String, ItemInfo]

  private val names = mutable.Map.empty[Any, String]

  override def get(name: String): ItemInfo = descriptors.get(name).orNull

  override def get(stack: ItemStack) = names.get(getBlockOrItem(stack)) match {
    case Some(name) => get(name)
    case _ => null
  }

  def registerBlock[T <: Block](instance: T, id: String) = {
    if (!descriptors.contains(id)) {
      instance match {
        case simple: SimpleBlock =>
          instance.setUnlocalizedName("oc." + id)
          GameRegistry.registerBlock(simple, classOf[common.block.Item], id)
          OpenComputers.proxy.registerModel(instance, id)
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
    }
    instance
  }

  def registerItem[T <: common.item.Delegate](delegate: T, id: String) = {
    if (!descriptors.contains(id)) {
      OpenComputers.proxy.registerModel(delegate, id)
      descriptors += id -> new ItemInfo {
        override def name = id

        override def block = null

        override def item = delegate.parent

        override def createItemStack(size: Int) = delegate.createItemStack(size)
      }
      names += delegate -> id
    }
    delegate
  }

  def registerItem(instance: Item, id: String) = {
    if (!descriptors.contains(id)) {
      instance match {
        case simple: SimpleItem =>
          simple.setUnlocalizedName("oc." + id)
          GameRegistry.registerItem(simple, id)
          OpenComputers.proxy.registerModel(instance, id)
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
    }
    instance
  }

  private def getBlockOrItem(stack: ItemStack): Any =
    if (stack == null) null
    else Delegator.subItem(stack).getOrElse(stack.getItem match {
      case block: ItemBlock => block.getBlock
      case item => item
    })

  // ----------------------------------------------------------------------- //

  def createOpenOS(amount: Int = 1) = {
    val data = new NBTTagCompound()
    data.setString(Settings.namespace + "fs.label", "openos")

    val nbt = new NBTTagCompound()
    nbt.setTag(Settings.namespace + "data", data)
    nbt.setString(Settings.namespace + "lootPath", "OpenOS")
    nbt.setInteger(Settings.namespace + "color", Color.dyes.indexOf("dyeGreen"))

    val stack = get("floppy").createItemStack(amount)
    stack.setTagCompound(nbt)

    stack
  }

  def createLuaBios(amount: Int = 1) = {
    val data = new NBTTagCompound()
    val code = new Array[Byte](4 * 1024)
    val count = OpenComputers.getClass.getResourceAsStream(Settings.scriptPath + "bios.lua").read(code)
    data.setByteArray(Settings.namespace + "eeprom", code.take(count))
    data.setString(Settings.namespace + "label", "Lua BIOS")

    val nbt = new NBTTagCompound()
    nbt.setTag(Settings.namespace + "data", data)

    val stack = get("eeprom").createItemStack(amount)
    stack.setTagCompound(nbt)

    stack
  }

  def createConfiguredDrone() = {
    val data = new MicrocontrollerData()

    data.tier = Tier.Four
    data.storedEnergy = Settings.get.bufferDrone.toInt
    data.components = Array(
      get("inventoryUpgrade").createItemStack(1),
      get("inventoryUpgrade").createItemStack(1),
      get("inventoryControllerUpgrade").createItemStack(1),
      get("tankUpgrade").createItemStack(1),
      get("tankControllerUpgrade").createItemStack(1),
      get("leashUpgrade").createItemStack(1),

      get("wlanCard").createItemStack(1),

      get("cpu3").createItemStack(1),
      get("ram6").createItemStack(1),
      get("ram6").createItemStack(1)
    )

    val stack = get("drone").createItemStack(1)
    data.save(stack)

    stack
  }

  def createConfiguredMicrocontroller() = {
    val data = new MicrocontrollerData()

    data.tier = Tier.Four
    data.storedEnergy = Settings.get.bufferMicrocontroller.toInt
    data.components = Array(
      get("signUpgrade").createItemStack(1),
      get("pistonUpgrade").createItemStack(1),

      get("redstoneCard2").createItemStack(1),
      get("wlanCard").createItemStack(1),

      get("cpu3").createItemStack(1),
      get("ram6").createItemStack(1),
      get("ram6").createItemStack(1)
    )

    val stack = get("microcontroller").createItemStack(1)
    data.save(stack)

    stack
  }

  def createConfiguredRobot() = {
    val data = new RobotData()

    data.name = "Creatix"
    data.tier = Tier.Four
    data.robotEnergy = Settings.get.bufferRobot.toInt
    data.totalEnergy = data.robotEnergy
    data.components = Array(
      get("screen1").createItemStack(1),
      get("keyboard").createItemStack(1),
      get("inventoryUpgrade").createItemStack(1),
      get("inventoryUpgrade").createItemStack(1),
      get("inventoryUpgrade").createItemStack(1),
      get("inventoryUpgrade").createItemStack(1),
      get("inventoryControllerUpgrade").createItemStack(1),
      get("tankUpgrade").createItemStack(1),
      get("tankControllerUpgrade").createItemStack(1),
      get("craftingUpgrade").createItemStack(1),

      get("graphicsCard3").createItemStack(1),
      get("redstoneCard2").createItemStack(1),
      get("wlanCard").createItemStack(1),
      get("internetCard").createItemStack(1),

      get("cpu3").createItemStack(1),
      get("ram6").createItemStack(1),
      get("ram6").createItemStack(1),

      createLuaBios(),
      createOpenOS(),
      get("hdd3").createItemStack(1)
    )
    data.containers = Array(
      get("cardContainer3").createItemStack(1),
      get("upgradeContainer3").createItemStack(1),
      get("upgradeContainer3").createItemStack(1)
    )

    val stack = get("robot").createItemStack(1)
    data.save(stack)

    stack
  }

  def createConfiguredTablet() = {
    val data = new TabletData()

    data.energy = Settings.get.bufferTablet
    data.maxEnergy = data.energy
    data.items = Array(
      Option(get("screen1").createItemStack(1)),
      Option(get("keyboard").createItemStack(1)),

      Option(get("signUpgrade").createItemStack(1)),
      Option(get("pistonUpgrade").createItemStack(1)),

      Option(get("graphicsCard2").createItemStack(1)),
      Option(get("redstoneCard2").createItemStack(1)),
      Option(get("wlanCard").createItemStack(1)),

      Option(get("cpu3").createItemStack(1)),
      Option(get("ram6").createItemStack(1)),
      Option(get("ram6").createItemStack(1)),

      Option(createLuaBios()),
      Option(createOpenOS()),
      Option(get("hdd3").createItemStack(1))
    )

    val stack = get("tablet").createItemStack(1)
    data.save(stack)

    stack
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
    initIntegration()
  }

  // Crafting materials.
  private def initMaterials(): Unit = {
    val materials = newItem(new item.Delegator(), "material")

    registerItem(new item.IronNugget(materials), "ironNugget")
    Recipes.addSubItem(new item.CuttingWire(materials), "cuttingWire", "oc:materialCuttingWire")
    Recipes.addSubItem(new item.Acid(materials), "acid", "oc:materialAcid")
    Recipes.addSubItem(new item.RawCircuitBoard(materials), "rawCircuitBoard", "oc:materialCircuitBoardRaw")
    Recipes.addSubItem(new item.CircuitBoard(materials), "circuitBoard", "oc:materialCircuitBoard")
    Recipes.addSubItem(new item.PrintedCircuitBoard(materials), "printedCircuitBoard", "oc:materialCircuitBoardPrinted")
    Recipes.addSubItem(new item.CardBase(materials), "card", "oc:materialCard")
    Recipes.addSubItem(new item.Transistor(materials), "transistor", "oc:materialTransistor")
    Recipes.addSubItem(new item.Microchip(materials, Tier.One), "chip1", "oc:circuitChip1")
    Recipes.addSubItem(new item.Microchip(materials, Tier.Two), "chip2", "oc:circuitChip2")
    Recipes.addSubItem(new item.Microchip(materials, Tier.Three), "chip3", "oc:circuitChip3")
    Recipes.addSubItem(new item.ALU(materials), "alu", "oc:materialALU")
    Recipes.addSubItem(new item.ControlUnit(materials), "cu", "oc:materialCU")
    Recipes.addSubItem(new item.Disk(materials), "disk", "oc:materialDisk")
    Recipes.addSubItem(new item.Interweb(materials), "interweb", "oc:materialInterweb")
    Recipes.addSubItem(new item.ButtonGroup(materials), "buttonGroup", "oc:materialButtonGroup")
    Recipes.addSubItem(new item.ArrowKeys(materials), "arrowKeys", "oc:materialArrowKey")
    Recipes.addSubItem(new item.NumPad(materials), "numPad", "oc:materialNumPad")

    Recipes.addSubItem(new item.TabletCase(materials), "tabletCase", "oc:tabletCase")
    Recipes.addSubItem(new item.MicrocontrollerCase(materials, Tier.One), "microcontrollerCase1", "oc:microcontrollerCase1")
    Recipes.addSubItem(new item.MicrocontrollerCase(materials, Tier.Two), "microcontrollerCase2", "oc:microcontrollerCase2")
    registerItem(new item.MicrocontrollerCase(materials, Tier.Four), "microcontrollerCaseCreative")
    Recipes.addSubItem(new item.DroneCase(materials, Tier.One), "droneCase1", "oc:droneCase1")
    Recipes.addSubItem(new item.DroneCase(materials, Tier.Two), "droneCase2", "oc:droneCase2")
    registerItem(new item.DroneCase(materials, Tier.Four), "droneCaseCreative")
  }

  // All kinds of tools.
  private def initTools(): Unit = {
    val tools = newItem(new item.Delegator(), "tool")

    Recipes.addSubItem(new item.Analyzer(tools), "analyzer", "oc:analyzer")
    registerItem(new item.Debugger(tools), "debugger")
    Recipes.addSubItem(new item.Terminal(tools), "terminal", "oc:terminal")
  }

  // General purpose components.
  private def initComponents(): Unit = {
    val components = newItem(new item.Delegator(), "component")

    Recipes.addSubItem(new item.CPU(components, Tier.One), "cpu1", "oc:cpu1")
    Recipes.addSubItem(new item.CPU(components, Tier.Two), "cpu2", "oc:cpu2")
    Recipes.addSubItem(new item.CPU(components, Tier.Three), "cpu3", "oc:cpu3")

    Recipes.addSubItem(new item.ComponentBus(components, Tier.One), "componentBus1", "oc:componentBus1")
    Recipes.addSubItem(new item.ComponentBus(components, Tier.Two), "componentBus2", "oc:componentBus2")
    Recipes.addSubItem(new item.ComponentBus(components, Tier.Three), "componentBus3", "oc:componentBus3")

    Recipes.addSubItem(new item.Memory(components, Tier.One), "ram1", "oc:ram1")
    Recipes.addSubItem(new item.Memory(components, Tier.Two), "ram2", "oc:ram2")
    Recipes.addSubItem(new item.Memory(components, Tier.Three), "ram3", "oc:ram3")
    Recipes.addSubItem(new item.Memory(components, Tier.Four), "ram4", "oc:ram4")
    Recipes.addSubItem(new item.Memory(components, Tier.Five), "ram5", "oc:ram5")
    Recipes.addSubItem(new item.Memory(components, Tier.Six), "ram6", "oc:ram6")

    registerItem(new item.Server(components, Tier.Four), "serverCreative")
    Recipes.addSubItem(new item.Server(components, Tier.One), "server1", "oc:server1")
    Recipes.addSubItem(new item.Server(components, Tier.Two), "server2", "oc:server2")
    Recipes.addSubItem(new item.Server(components, Tier.Three), "server3", "oc:server3")
  }

  // Card components.
  private def initCards(): Unit = {
    val cards = newItem(new item.Delegator(), "card")

    registerItem(new item.DebugCard(cards), "debugCard")
    Recipes.addSubItem(new item.GraphicsCard(cards, Tier.One), "graphicsCard1", "oc:graphicsCard1")
    Recipes.addSubItem(new item.GraphicsCard(cards, Tier.Two), "graphicsCard2", "oc:graphicsCard2")
    Recipes.addSubItem(new item.GraphicsCard(cards, Tier.Three), "graphicsCard3", "oc:graphicsCard3")
    Recipes.addSubItem(new item.RedstoneCard(cards, Tier.One), "redstoneCard1", "oc:redstoneCard1")
    Recipes.addSubItem(new item.RedstoneCard(cards, Tier.Two), "redstoneCard2", "oc:redstoneCard2")
    Recipes.addSubItem(new item.NetworkCard(cards), "lanCard", "oc:lanCard")
    Recipes.addSubItem(new item.WirelessNetworkCard(cards), "wlanCard", "oc:wlanCard")
    Recipes.addSubItem(new item.InternetCard(cards), "internetCard", "oc:internetCard")
    Recipes.addSubItem(new item.LinkedCard(cards), "linkedCard", "oc:linkedCard")
  }

  // Upgrade components.
  private def initUpgrades(): Unit = {
    val upgrades = newItem(new item.Delegator(), "upgrade")

    Recipes.addSubItem(new item.UpgradeAngel(upgrades), "angelUpgrade", "oc:angelUpgrade")
    Recipes.addSubItem(new item.UpgradeBattery(upgrades, Tier.One), "batteryUpgrade1", "oc:batteryUpgrade1")
    Recipes.addSubItem(new item.UpgradeBattery(upgrades, Tier.Two), "batteryUpgrade2", "oc:batteryUpgrade2")
    Recipes.addSubItem(new item.UpgradeBattery(upgrades, Tier.Three), "batteryUpgrade3", "oc:batteryUpgrade3")
    Recipes.addSubItem(new item.UpgradeChunkloader(upgrades), "chunkloaderUpgrade", "oc:chunkloaderUpgrade")
    Recipes.addSubItem(new item.UpgradeContainerCard(upgrades, Tier.One), "cardContainer1", "oc:cardContainer1")
    Recipes.addSubItem(new item.UpgradeContainerCard(upgrades, Tier.Two), "cardContainer2", "oc:cardContainer2")
    Recipes.addSubItem(new item.UpgradeContainerCard(upgrades, Tier.Three), "cardContainer3", "oc:cardContainer3")
    Recipes.addSubItem(new item.UpgradeContainerUpgrade(upgrades, Tier.One), "upgradeContainer1", "oc:upgradeContainer1")
    Recipes.addSubItem(new item.UpgradeContainerUpgrade(upgrades, Tier.Two), "upgradeContainer2", "oc:upgradeContainer2")
    Recipes.addSubItem(new item.UpgradeContainerUpgrade(upgrades, Tier.Three), "upgradeContainer3", "oc:upgradeContainer3")
    Recipes.addSubItem(new item.UpgradeCrafting(upgrades), "craftingUpgrade", "oc:craftingUpgrade")
    Recipes.addSubItem(new item.UpgradeDatabase(upgrades, Tier.One), "databaseUpgrade1", "oc:databaseUpgrade1")
    Recipes.addSubItem(new item.UpgradeDatabase(upgrades, Tier.Two), "databaseUpgrade2", "oc:databaseUpgrade2")
    Recipes.addSubItem(new item.UpgradeDatabase(upgrades, Tier.Three), "databaseUpgrade3", "oc:databaseUpgrade3")
    Recipes.addSubItem(new item.UpgradeExperience(upgrades), "experienceUpgrade", "oc:experienceUpgrade")
    Recipes.addSubItem(new item.UpgradeGenerator(upgrades), "generatorUpgrade", "oc:generatorUpgrade")
    Recipes.addSubItem(new item.UpgradeInventory(upgrades), "inventoryUpgrade", "oc:inventoryUpgrade")
    Recipes.addSubItem(new item.UpgradeInventoryController(upgrades), "inventoryControllerUpgrade", "oc:inventoryControllerUpgrade")
    Recipes.addSubItem(new item.UpgradeNavigation(upgrades), "navigationUpgrade", "oc:navigationUpgrade")
    Recipes.addSubItem(new item.UpgradePiston(upgrades), "pistonUpgrade", "oc:pistonUpgrade")
    Recipes.addSubItem(new item.UpgradeSign(upgrades), "signUpgrade", "oc:signUpgrade")
    Recipes.addSubItem(new item.UpgradeSolarGenerator(upgrades), "solarGeneratorUpgrade", "oc:solarGeneratorUpgrade")
    Recipes.addSubItem(new item.UpgradeTank(upgrades), "tankUpgrade", "oc:tankUpgrade")
    Recipes.addSubItem(new item.UpgradeTankController(upgrades), "tankControllerUpgrade", "oc:tankControllerUpgrade")
    Recipes.addSubItem(new item.UpgradeTractorBeam(upgrades), "tractorBeamUpgrade", "oc:tractorBeamUpgrade")
    Recipes.addSubItem(new item.UpgradeLeash(upgrades), "leashUpgrade", "oc:leashUpgrade")
  }

  // Storage media of all kinds.
  private def initStorage(): Unit = {
    val storage = newItem(new item.Delegator() {
      // Override to inject loot disks.
      override def getSubItems(item: Item, tab: CreativeTabs, list: java.util.List[_]) {
        super.getSubItems(item, tab, list)
        Items.add(list, createLuaBios())
        Loot.worldDisks.values.foreach(entry => Items.add(list, entry._1))
      }
    }, "storage")

    Recipes.addSubItem(new item.EEPROM(storage), "eeprom", "oc:eeprom")
    Recipes.addSubItem(new item.FloppyDisk(storage), "floppy", "oc:floppy")
    Recipes.addSubItem(new item.HardDiskDrive(storage, Tier.One), "hdd1", "oc:hdd1")
    Recipes.addSubItem(new item.HardDiskDrive(storage, Tier.Two), "hdd2", "oc:hdd2")
    Recipes.addSubItem(new item.HardDiskDrive(storage, Tier.Three), "hdd3", "oc:hdd3")

    Recipes.addRecipe(createLuaBios(), "luaBios")
    Recipes.addRecipe(createOpenOS(), "openOS")
  }

  // Special purpose items that don't fit into any other category.
  private def initSpecial(): Unit = {
    val misc = newItem(new item.Delegator() {
      private lazy val configuredItems = Array(
        Items.createOpenOS(),
        Items.createLuaBios(),
        Items.createConfiguredDrone(),
        Items.createConfiguredMicrocontroller(),
        Items.createConfiguredRobot(),
        Items.createConfiguredTablet()
      )

      override def getSubItems(item: Item, tab: CreativeTabs, list: util.List[_]): Unit = {
        super.getSubItems(item, tab, list)
        configuredItems.foreach(Items.add(list, _))
      }
    }, "misc")

    registerItem(new item.Tablet(misc), "tablet")
    registerItem(new item.Drone(misc), "drone")
    registerItem(new item.Present(misc), "present")
  }

  // Items used for integration with other mods.
  private def initIntegration(): Unit = {
    val integration = newItem(new item.Delegator(), "integration")

    // Always create, to avoid shifting IDs.
    val abstractBus = new item.AbstractBusCard(integration)
    val worldSensorCard = new item.WorldSensorCard(integration)

    // Only register recipes if the related mods are present.
    if (Mods.StargateTech2.isAvailable) {
      Recipes.addSubItem(abstractBus, "abstractBusCard", "oc:abstractBusCard")
    }
    if (Mods.Galacticraft.isAvailable) {
      Recipes.addSubItem(worldSensorCard, "worldSensorCard", "oc:worldSensorCard")
    }
  }

  private def newItem[T <: Item](item: T, name: String): T = {
    item.setUnlocalizedName("oc." + name)
    GameRegistry.registerItem(item, name)
    item
  }

  // Workaround for MC's untyped lists...
  private final def add[T](list: java.util.List[T], value: Any) = list.add(value.asInstanceOf[T])
}
