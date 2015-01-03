package li.cil.oc.common.init

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

  // ----------------------------------------------------------------------- //

  def init() {
    val multi = new item.Delegator()
    val materials = new item.Delegator()
    val components = new item.Delegator()
    val cards = new item.Delegator()
    val upgrades = new item.Delegator()
    val storage = new item.Delegator() {
      override def getSubItems(item: Item, tab: CreativeTabs, list: java.util.List[_]) {
        // Workaround for MC's untyped lists...
        def add[T](list: java.util.List[T], value: Any) = list.add(value.asInstanceOf[T])
        super.getSubItems(item, tab, list)
        Loot.worldDisks.values.foreach(entry => add(list, entry._1))
        add(list, createOpenOS())
        add(list, createLuaBios())
      }
    }

    GameRegistry.registerItem(multi, "item")
    GameRegistry.registerItem(materials, "material")
    GameRegistry.registerItem(components, "component")
    GameRegistry.registerItem(cards, "card")
    GameRegistry.registerItem(upgrades, "upgrade")
    GameRegistry.registerItem(storage, "storage")

    registerItem(new item.IronNugget(materials), "ironNugget")
    Recipes.addMultiItem(new item.CuttingWire(materials), "cuttingWire", "oc:materialCuttingWire")
    Recipes.addMultiItem(new item.Acid(materials), "acid", "oc:materialAcid")
    Recipes.addMultiItem(new item.RawCircuitBoard(materials), "rawCircuitBoard", "oc:materialCircuitBoardRaw")
    Recipes.addMultiItem(new item.CircuitBoard(materials), "circuitBoard", "oc:materialCircuitBoard")
    Recipes.addMultiItem(new item.PrintedCircuitBoard(materials), "printedCircuitBoard", "oc:materialCircuitBoardPrinted")
    Recipes.addMultiItem(new item.CardBase(materials), "card", "oc:materialCard")
    Recipes.addMultiItem(new item.Transistor(materials), "transistor", "oc:materialTransistor")
    Recipes.addMultiItem(new item.Microchip(materials, Tier.One), "chip1", "oc:circuitChip1")
    Recipes.addMultiItem(new item.Microchip(materials, Tier.Two), "chip2", "oc:circuitChip2")
    Recipes.addMultiItem(new item.Microchip(materials, Tier.Three), "chip3", "oc:circuitChip3")
    Recipes.addMultiItem(new item.ALU(materials), "alu", "oc:materialALU")
    Recipes.addMultiItem(new item.ControlUnit(materials), "cu", "oc:materialCU")
    Recipes.addMultiItem(new item.Disk(materials), "disk", "oc:materialDisk")
    Recipes.addMultiItem(new item.Interweb(materials), "interweb", "oc:materialInterweb")
    Recipes.addMultiItem(new item.ButtonGroup(materials), "buttonGroup", "oc:materialButtonGroup")
    Recipes.addMultiItem(new item.ArrowKeys(materials), "arrowKeys", "oc:materialArrowKey")
    Recipes.addMultiItem(new item.NumPad(materials), "numPad", "oc:materialNumPad")

    Recipes.addMultiItem(new item.TabletCase(materials), "tabletCase", "oc:tabletCase")
    Recipes.addMultiItem(new item.MicrocontrollerCase(materials, Tier.One), "microcontrollerCase1", "oc:microcontrollerCase1")
    Recipes.addMultiItem(new item.MicrocontrollerCase(materials, Tier.Two), "microcontrollerCase2", "oc:microcontrollerCase2")
    Recipes.addMultiItem(new item.DroneCase(materials, Tier.One), "droneCase1", "oc:droneCase1")
    Recipes.addMultiItem(new item.DroneCase(materials, Tier.Two), "droneCase2", "oc:droneCase2")

    // Always create, to avoid shifting IDs.
    val abstractBus = new item.AbstractBusCard(cards)
    if (Mods.StargateTech2.isAvailable) {
      Recipes.addMultiItem(abstractBus, "abstractBusCard", "oc:abstractBusCard")
    }

    Recipes.addMultiItem(new item.CPU(components, Tier.One), "cpu1", "oc:cpu1")
    Recipes.addMultiItem(new item.CPU(components, Tier.Two), "cpu2", "oc:cpu2")
    Recipes.addMultiItem(new item.CPU(components, Tier.Three), "cpu3", "oc:cpu3")

    Recipes.addMultiItem(new item.ComponentBus(components, Tier.One), "componentBus1", "oc:componentBus1")
    Recipes.addMultiItem(new item.ComponentBus(components, Tier.Two), "componentBus2", "oc:componentBus2")
    Recipes.addMultiItem(new item.ComponentBus(components, Tier.Three), "componentBus3", "oc:componentBus3")

    Recipes.addMultiItem(new item.Memory(components, Tier.One), "ram1", "oc:ram1")
    Recipes.addMultiItem(new item.Memory(components, Tier.Two), "ram2", "oc:ram2")
    Recipes.addMultiItem(new item.Memory(components, Tier.Three), "ram3", "oc:ram3")
    Recipes.addMultiItem(new item.Memory(components, Tier.Four), "ram4", "oc:ram4")
    Recipes.addMultiItem(new item.Memory(components, Tier.Five), "ram5", "oc:ram5")
    Recipes.addMultiItem(new item.Memory(components, Tier.Six), "ram6", "oc:ram6")

    registerItem(new item.Server(components, Tier.Four), "serverCreative")
    Recipes.addMultiItem(new item.Server(components, Tier.One), "server1", "oc:server1")
    Recipes.addMultiItem(new item.Server(components, Tier.Two), "server2", "oc:server2")
    Recipes.addMultiItem(new item.Server(components, Tier.Three), "server3", "oc:server3")

    registerItem(new item.DebugCard(cards), "debugCard")
    Recipes.addMultiItem(new item.GraphicsCard(cards, Tier.One), "graphicsCard1", "oc:graphicsCard1")
    Recipes.addMultiItem(new item.GraphicsCard(cards, Tier.Two), "graphicsCard2", "oc:graphicsCard2")
    Recipes.addMultiItem(new item.GraphicsCard(cards, Tier.Three), "graphicsCard3", "oc:graphicsCard3")
    Recipes.addMultiItem(new item.RedstoneCard(cards, Tier.One), "redstoneCard1", "oc:redstoneCard1")
    Recipes.addMultiItem(new item.RedstoneCard(cards, Tier.Two), "redstoneCard2", "oc:redstoneCard2")
    Recipes.addMultiItem(new item.NetworkCard(cards), "lanCard", "oc:lanCard")
    Recipes.addMultiItem(new item.WirelessNetworkCard(cards), "wlanCard", "oc:wlanCard")
    Recipes.addMultiItem(new item.InternetCard(cards), "internetCard", "oc:internetCard")
    Recipes.addMultiItem(new item.LinkedCard(cards), "linkedCard", "oc:linkedCard")

    Recipes.addMultiItem(new item.UpgradeAngel(upgrades), "angelUpgrade", "oc:angelUpgrade")
    Recipes.addMultiItem(new item.UpgradeBattery(upgrades, Tier.One), "batteryUpgrade1", "oc:batteryUpgrade1")
    Recipes.addMultiItem(new item.UpgradeBattery(upgrades, Tier.Three), "batteryUpgrade3", "oc:batteryUpgrade3")
    Recipes.addMultiItem(new item.UpgradeBattery(upgrades, Tier.Two), "batteryUpgrade2", "oc:batteryUpgrade2")
    Recipes.addMultiItem(new item.UpgradeChunkloader(upgrades), "chunkloaderUpgrade", "oc:chunkloaderUpgrade")
    Recipes.addMultiItem(new item.UpgradeContainerCard(upgrades, Tier.One), "cardContainer1", "oc:cardContainer1")
    Recipes.addMultiItem(new item.UpgradeContainerCard(upgrades, Tier.Three), "cardContainer3", "oc:cardContainer3")
    Recipes.addMultiItem(new item.UpgradeContainerCard(upgrades, Tier.Two), "cardContainer2", "oc:cardContainer2")
    Recipes.addMultiItem(new item.UpgradeContainerUpgrade(upgrades, Tier.One), "upgradeContainer1", "oc:upgradeContainer1")
    Recipes.addMultiItem(new item.UpgradeContainerUpgrade(upgrades, Tier.Three), "upgradeContainer3", "oc:upgradeContainer3")
    Recipes.addMultiItem(new item.UpgradeContainerUpgrade(upgrades, Tier.Two), "upgradeContainer2", "oc:upgradeContainer2")
    Recipes.addMultiItem(new item.UpgradeCrafting(upgrades), "craftingUpgrade", "oc:craftingUpgrade")
    Recipes.addMultiItem(new item.UpgradeDatabase(upgrades, Tier.One), "databaseUpgrade1", "oc:databaseUpgrade1")
    Recipes.addMultiItem(new item.UpgradeDatabase(upgrades, Tier.Three), "databaseUpgrade3", "oc:databaseUpgrade3")
    Recipes.addMultiItem(new item.UpgradeDatabase(upgrades, Tier.Two), "databaseUpgrade2", "oc:databaseUpgrade2")
    Recipes.addMultiItem(new item.UpgradeExperience(upgrades), "experienceUpgrade", "oc:experienceUpgrade")
    Recipes.addMultiItem(new item.UpgradeGenerator(upgrades), "generatorUpgrade", "oc:generatorUpgrade")
    Recipes.addMultiItem(new item.UpgradeInventory(upgrades), "inventoryUpgrade", "oc:inventoryUpgrade")
    Recipes.addMultiItem(new item.UpgradeInventoryController(upgrades), "inventoryControllerUpgrade", "oc:inventoryControllerUpgrade")
    Recipes.addMultiItem(new item.UpgradeNavigation(upgrades), "navigationUpgrade", "oc:navigationUpgrade")
    Recipes.addMultiItem(new item.UpgradePiston(upgrades), "pistonUpgrade", "oc:pistonUpgrade")
    Recipes.addMultiItem(new item.UpgradeSign(upgrades), "signUpgrade", "oc:signUpgrade")
    Recipes.addMultiItem(new item.UpgradeSolarGenerator(upgrades), "solarGeneratorUpgrade", "oc:solarGeneratorUpgrade")
    Recipes.addMultiItem(new item.UpgradeTank(upgrades), "tankUpgrade", "oc:tankUpgrade")
    Recipes.addMultiItem(new item.UpgradeTankController(upgrades), "tankControllerUpgrade", "oc:tankControllerUpgrade")
    Recipes.addMultiItem(new item.UpgradeTractorBeam(upgrades), "tractorBeamUpgrade", "oc:tractorBeamUpgrade")
    Recipes.addMultiItem(new item.UpgradeLeash(upgrades), "leashUpgrade", "oc:leashUpgrade")

    Recipes.addItem(new item.EEPROM(), "eeprom", "oc:eeprom")
    Recipes.addMultiItem(new item.HardDiskDrive(storage, Tier.One), "hdd1", "oc:hdd1")
    Recipes.addMultiItem(new item.HardDiskDrive(storage, Tier.Two), "hdd2", "oc:hdd2")
    Recipes.addMultiItem(new item.HardDiskDrive(storage, Tier.Three), "hdd3", "oc:hdd3")
    Recipes.addMultiItem(new item.FloppyDisk(storage), "floppy", "oc:floppy")
    registerItem(new item.FloppyDisk(storage) {
      showInItemList = false

      override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
        if (player.isSneaking) get("floppy").createItemStack(1)
        else super.onItemRightClick(stack, world, player)
      }
    }, "lootDisk")

    Recipes.addRecipe(createLuaBios(), "luaBios")
    Recipes.addRecipe(createOpenOS(), "openOS")

    Recipes.addMultiItem(new item.Analyzer(components), "analyzer", "oc:analyzer")
    registerItem(new item.Debugger(components), "debugger")

    registerItem(new item.Tablet(multi), "tablet")
    registerItem(new item.Drone(multi), "drone")

    registerItem(new item.Present(multi), "present")
  }
}
