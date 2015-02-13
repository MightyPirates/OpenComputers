package li.cil.oc.common.init

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.detail.ItemAPI
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.common
import li.cil.oc.common.Loot
import li.cil.oc.common.Tier
import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.common.item
import li.cil.oc.common.item.SimpleItem
import li.cil.oc.common.item.UpgradeLeash
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.integration.Mods
import li.cil.oc.util.Color
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
  private val descriptors = mutable.Map.empty[String, ItemInfo]

  private val names = mutable.Map.empty[Any, String]

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

  def registerItem[T <: common.item.Delegate](delegate: T, id: String) = {
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

  private def getBlockOrItem(stack: ItemStack): Any = if (stack == null) null
  else {
    multi.subItem(stack).getOrElse(stack.getItem match {
      case block: ItemBlock => block.field_150939_a
      case item => item
    })
  }

  // ----------------------------------------------------------------------- //

  def createOpenOS(amount: Int = 1) = {
    val data = new NBTTagCompound()
    data.setString(Settings.namespace + "fs.label", "openos")

    val nbt = new NBTTagCompound()
    nbt.setTag(Settings.namespace + "data", data)
    nbt.setString(Settings.namespace + "lootPath", "OpenOS")
    nbt.setInteger(Settings.namespace + "color", Color.dyes.indexOf("dyeGreen"))

    val stack = get("lootDisk").createItemStack(amount)
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
    val data = new DroneData()

    data.name = "Crecopter"
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
      get("diskDrive").createItemStack(1)
    )

    val stack = get("robot").createItemStack(1)
    data.save(stack)

    stack
  }

  def createConfiguredTablet() = {
    val data = new TabletData()

    data.tier = Tier.Four
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
    data.container = Option(get("diskDrive").createItemStack(1))

    val stack = get("tablet").createItemStack(1)
    data.save(stack)

    stack
  }

  // ----------------------------------------------------------------------- //

  var multi: item.Delegator = _

  // ----------------------------------------------------------------------- //
  // Crafting
  var ironNugget: item.IronNugget = _

  def init() {
    multi = new item.Delegator() {
      lazy val configuredItems = Array(
        createOpenOS(),
        createLuaBios(),
        createConfiguredDrone(),
        createConfiguredMicrocontroller(),
        createConfiguredRobot(),
        createConfiguredTablet()
      )

      override def getSubItems(item: Item, tab: CreativeTabs, list: java.util.List[_]) {
        // Workaround for MC's untyped lists...
        def add[T](list: java.util.List[T], value: Any) = list.add(value.asInstanceOf[T])
        super.getSubItems(item, tab, list)
        Loot.worldDisks.values.foreach(entry => add(list, entry._1))
        configuredItems.foreach(add(list, _))
      }
    }

    GameRegistry.registerItem(multi, "item")

    Recipes.addMultiItem(new item.Analyzer(multi), "analyzer", "oc:analyzer")

    Recipes.addMultiItem(new item.Memory(multi, Tier.One), "ram1", "oc:ram1")
    Recipes.addMultiItem(new item.Memory(multi, Tier.Three), "ram3", "oc:ram3")
    Recipes.addMultiItem(new item.Memory(multi, Tier.Four), "ram4", "oc:ram4")

    Recipes.addMultiItem(new item.FloppyDisk(multi), "floppy", "oc:floppy")
    Recipes.addMultiItem(new item.HardDiskDrive(multi, Tier.One), "hdd1", "oc:hdd1")
    Recipes.addMultiItem(new item.HardDiskDrive(multi, Tier.Two), "hdd2", "oc:hdd2")
    Recipes.addMultiItem(new item.HardDiskDrive(multi, Tier.Three), "hdd3", "oc:hdd3")

    Recipes.addMultiItem(new item.GraphicsCard(multi, Tier.One), "graphicsCard1", "oc:graphicsCard1")
    Recipes.addMultiItem(new item.GraphicsCard(multi, Tier.Two), "graphicsCard2", "oc:graphicsCard2")
    Recipes.addMultiItem(new item.GraphicsCard(multi, Tier.Three), "graphicsCard3", "oc:graphicsCard3")
    Recipes.addMultiItem(new item.NetworkCard(multi), "lanCard", "oc:lanCard")
    Recipes.addMultiItem(new item.RedstoneCard(multi, Tier.Two), "redstoneCard2", "oc:redstoneCard2")
    Recipes.addMultiItem(new item.WirelessNetworkCard(multi), "wlanCard", "oc:wlanCard")

    Recipes.addMultiItem(new item.UpgradeCrafting(multi), "craftingUpgrade", "oc:craftingUpgrade")
    Recipes.addMultiItem(new item.UpgradeGenerator(multi), "generatorUpgrade", "oc:generatorUpgrade")

    ironNugget = new item.IronNugget(multi)

    Recipes.addMultiItem(new item.CuttingWire(multi), "cuttingWire", "oc:materialCuttingWire")
    Recipes.addMultiItem(new item.Acid(multi), "acid", "oc:materialAcid")
    Recipes.addMultiItem(new item.Disk(multi), "disk", "oc:materialDisk")

    Recipes.addMultiItem(new item.ButtonGroup(multi), "buttonGroup", "oc:materialButtonGroup")
    Recipes.addMultiItem(new item.ArrowKeys(multi), "arrowKeys", "oc:materialArrowKey")
    Recipes.addMultiItem(new item.NumPad(multi), "numPad", "oc:materialNumPad")

    Recipes.addMultiItem(new item.Transistor(multi), "transistor", "oc:materialTransistor")
    Recipes.addMultiItem(new item.Microchip(multi, Tier.One), "chip1", "oc:circuitChip1")
    Recipes.addMultiItem(new item.Microchip(multi, Tier.Two), "chip2", "oc:circuitChip2")
    Recipes.addMultiItem(new item.Microchip(multi, Tier.Three), "chip3", "oc:circuitChip3")
    Recipes.addMultiItem(new item.ALU(multi), "alu", "oc:materialALU")
    Recipes.addMultiItem(new item.ControlUnit(multi), "cu", "oc:materialCU")
    Recipes.addMultiItem(new item.CPU(multi, Tier.One), "cpu1", "oc:cpu1")

    Recipes.addMultiItem(new item.RawCircuitBoard(multi), "rawCircuitBoard", "oc:materialCircuitBoardRaw")
    Recipes.addMultiItem(new item.CircuitBoard(multi), "circuitBoard", "oc:materialCircuitBoard")
    Recipes.addMultiItem(new item.PrintedCircuitBoard(multi), "printedCircuitBoard", "oc:materialCircuitBoardPrinted")
    Recipes.addMultiItem(new item.CardBase(multi), "card", "oc:materialCard")

    // v1.1.0
    Recipes.addMultiItem(new item.UpgradeSolarGenerator(multi), "solarGeneratorUpgrade", "oc:solarGeneratorUpgrade")
    Recipes.addMultiItem(new item.UpgradeSign(multi), "signUpgrade", "oc:signUpgrade")
    Recipes.addMultiItem(new item.UpgradeNavigation(multi), "navigationUpgrade", "oc:navigationUpgrade")

    // Always create, to avoid shifting IDs.
    val abstractBus = new item.AbstractBusCard(multi)
    if (Mods.StargateTech2.isAvailable) {
      Recipes.addMultiItem(abstractBus, "abstractBusCard", "oc:abstractBusCard")
    }

    Recipes.addMultiItem(new item.Memory(multi, Tier.Five), "ram5", "oc:ram5")
    Recipes.addMultiItem(new item.Memory(multi, Tier.Six), "ram6", "oc:ram6")

    // v1.2.0
    Recipes.addMultiItem(new item.Server(multi, Tier.Three), "server3", "oc:server3")
    Recipes.addMultiItem(new item.Terminal(multi), "terminal", "oc:terminal")
    Recipes.addMultiItem(new item.CPU(multi, Tier.Two), "cpu2", "oc:cpu2")
    Recipes.addMultiItem(new item.CPU(multi, Tier.Three), "cpu3", "oc:cpu3")
    Recipes.addMultiItem(new item.InternetCard(multi), "internetCard", "oc:internetCard")
    Recipes.addMultiItem(new item.Server(multi, Tier.One), "server1", "oc:server1")
    Recipes.addMultiItem(new item.Server(multi, Tier.Two), "server2", "oc:server2")

    // v1.2.3
    registerItem(new item.FloppyDisk(multi) {
      showInItemList = false

      override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
        if (player.isSneaking) get("floppy").createItemStack(1)
        else super.onItemRightClick(stack, world, player)
      }
    }, "lootDisk")

    // v1.2.6
    Recipes.addMultiItem(new item.Interweb(multi), "interweb", "oc:materialInterweb")
    Recipes.addMultiItem(new item.UpgradeAngel(multi), "angelUpgrade", "oc:angelUpgrade")
    Recipes.addMultiItem(new item.Memory(multi, Tier.Two), "ram2", "oc:ram2")

    // v1.3.0
    Recipes.addMultiItem(new item.LinkedCard(multi), "linkedCard", "oc:linkedCard")
    Recipes.addMultiItem(new item.UpgradeExperience(multi), "experienceUpgrade", "oc:experienceUpgrade")
    Recipes.addMultiItem(new item.UpgradeInventory(multi), "inventoryUpgrade", "oc:inventoryUpgrade")
    Recipes.addMultiItem(new item.UpgradeContainerUpgrade(multi, Tier.One), "upgradeContainer1", "oc:upgradeContainer1")
    Recipes.addMultiItem(new item.UpgradeContainerUpgrade(multi, Tier.Two), "upgradeContainer2", "oc:upgradeContainer2")
    Recipes.addMultiItem(new item.UpgradeContainerUpgrade(multi, Tier.Three), "upgradeContainer3", "oc:upgradeContainer3")
    Recipes.addMultiItem(new item.UpgradeContainerCard(multi, Tier.One), "cardContainer1", "oc:cardContainer1")
    Recipes.addMultiItem(new item.UpgradeContainerCard(multi, Tier.Two), "cardContainer2", "oc:cardContainer2")
    Recipes.addMultiItem(new item.UpgradeContainerCard(multi, Tier.Three), "cardContainer3", "oc:cardContainer3")

    // Special case loot disk because this one's craftable and having it have
    // the same item damage would confuse NEI and the item costs computation.
    // UPDATE: screw that, keeping it for compatibility for now, but using recipe
    // below now (creating "normal" loot disk).
    registerItem(new item.FloppyDisk(multi) {
      showInItemList = false

      override def createItemStack(amount: Int) = createOpenOS(amount)

      override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
        if (player.isSneaking) get("floppy").createItemStack(1)
        else super.onItemRightClick(stack, world, player)
      }
    }, "openOS")
    Recipes.addRecipe(createOpenOS(), "openOS")

    Recipes.addMultiItem(new item.UpgradeInventoryController(multi), "inventoryControllerUpgrade", "oc:inventoryControllerUpgrade")
    Recipes.addMultiItem(new item.UpgradeChunkloader(multi), "chunkloaderUpgrade", "oc:chunkloaderUpgrade")
    Recipes.addMultiItem(new item.UpgradeBattery(multi, Tier.One), "batteryUpgrade1", "oc:batteryUpgrade1")
    Recipes.addMultiItem(new item.UpgradeBattery(multi, Tier.Two), "batteryUpgrade2", "oc:batteryUpgrade2")
    Recipes.addMultiItem(new item.UpgradeBattery(multi, Tier.Three), "batteryUpgrade3", "oc:batteryUpgrade3")
    Recipes.addMultiItem(new item.RedstoneCard(multi, Tier.One), "redstoneCard1", "oc:redstoneCard1")

    // 1.3.2
    Recipes.addMultiItem(new item.UpgradeTractorBeam(multi), "tractorBeamUpgrade", "oc:tractorBeamUpgrade")

    // 1.3.?
    registerItem(new item.Tablet(multi), "tablet")

    // 1.3.2 (cont.)
    registerItem(new item.Server(multi, Tier.Four), "serverCreative")

    // 1.3.3
    Recipes.addMultiItem(new item.ComponentBus(multi, Tier.One), "componentBus1", "oc:componentBus1")
    Recipes.addMultiItem(new item.ComponentBus(multi, Tier.Two), "componentBus2", "oc:componentBus2")
    Recipes.addMultiItem(new item.ComponentBus(multi, Tier.Three), "componentBus3", "oc:componentBus3")
    registerItem(new item.DebugCard(multi), "debugCard")

    // 1.3.5
    Recipes.addMultiItem(new item.TabletCase(multi, Tier.One), "tabletCase1", "oc:tabletCase1")
    Recipes.addMultiItem(new item.UpgradePiston(multi), "pistonUpgrade", "oc:pistonUpgrade")
    Recipes.addMultiItem(new item.UpgradeTank(multi), "tankUpgrade", "oc:tankUpgrade")
    Recipes.addMultiItem(new item.UpgradeTankController(multi), "tankControllerUpgrade", "oc:tankControllerUpgrade")

    // 1.4.0
    Recipes.addMultiItem(new item.UpgradeDatabase(multi, Tier.One), "databaseUpgrade1", "oc:databaseUpgrade1")
    Recipes.addMultiItem(new item.UpgradeDatabase(multi, Tier.Two), "databaseUpgrade2", "oc:databaseUpgrade2")
    Recipes.addMultiItem(new item.UpgradeDatabase(multi, Tier.Three), "databaseUpgrade3", "oc:databaseUpgrade3")
    registerItem(new item.Debugger(multi), "debugger")

    // 1.4.2
    val eeprom = new item.EEPROM()
    Recipes.addItem(eeprom, "eeprom", "oc:eeprom")
    Recipes.addRecipe(createLuaBios(), "luaBios")
    Recipes.addMultiItem(new item.MicrocontrollerCase(multi, Tier.One), "microcontrollerCase1", "oc:microcontrollerCase1")

    // 1.4.3
    Recipes.addMultiItem(new item.DroneCase(multi, Tier.One), "droneCase1", "oc:droneCase1")
    registerItem(new item.Drone(multi), "drone")
    Recipes.addMultiItem(new UpgradeLeash(multi), "leashUpgrade", "oc:leashUpgrade")
    Recipes.addMultiItem(new item.MicrocontrollerCase(multi, Tier.Two), "microcontrollerCase2", "oc:microcontrollerCase2")
    Recipes.addMultiItem(new item.DroneCase(multi, Tier.Two), "droneCase2", "oc:droneCase2")
    registerItem(new item.Present(multi), "present")

    // Always create, to avoid shifting IDs.
    val worldSensorCard = new item.WorldSensorCard(multi)
    if (Mods.Galacticraft.isAvailable) {
      Recipes.addMultiItem(worldSensorCard, "worldSensorCard", "oc:worldSensorCard")
    }

    // 1.4.4
    registerItem(new item.MicrocontrollerCase(multi, Tier.Four), "microcontrollerCaseCreative")
    registerItem(new item.DroneCase(multi, Tier.Four), "droneCaseCreative")

    // 1.4.7
    Recipes.addMultiItem(new item.TabletCase(multi, Tier.Two), "tabletCase2", "oc:tabletCase2")
    registerItem(new item.TabletCase(multi, Tier.Four), "tabletCaseCreative")
  }
}
