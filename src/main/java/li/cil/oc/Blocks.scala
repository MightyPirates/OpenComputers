package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.block._
import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.oredict.OreDictionary

object Blocks {
  var blockSimple: SimpleDelegator = _
  var blockSimpleWithRedstone: SimpleDelegator = _
  var blockSpecial: SpecialDelegator = _
  var blockSpecialWithRedstone: SpecialDelegator = _

  var adapter: Adapter = _
  var cable: Cable = _
  var capacitor: Capacitor = _
  var charger: Charger = _
  var case1, case2, case3: Case = _
  var diskDrive: DiskDrive = _
  var keyboard: Keyboard = _
  var hologram: Hologram = _
  var powerConverter: PowerConverter = _
  var powerDistributor: PowerDistributor = _
  var redstone: Redstone = _
  var robotProxy: RobotProxy = _
  var robotAfterimage: RobotAfterimage = _
  var router: Router = _
  var screen1, screen2, screen3: Screen = _
  var serverRack: Rack = _
  var wirelessRouter: WirelessRouter = _

  def init() {
    blockSimple = new SimpleDelegator(Settings.get.blockId1)
    blockSimpleWithRedstone = new SimpleRedstoneDelegator(Settings.get.blockId2)
    blockSpecial = new SpecialDelegator(Settings.get.blockId3)
    blockSpecialWithRedstone = new SpecialRedstoneDelegator(Settings.get.blockId4) {
      override def subBlockItemStacks() = super.subBlockItemStacks() ++ Iterable({
        val stack = new ItemStack(this, 1, robotProxy.blockId)
        stack.setTagCompound(new NBTTagCompound("tag"))
        stack.getTagCompound.setDouble(Settings.namespace + "xp", Settings.get.baseXpToLevel + math.pow(30.0001 * Settings.get.constantXpGrowth, Settings.get.exponentialXpGrowth))
        stack.getTagCompound.setInteger(Settings.namespace + "storedEnergy", (Settings.get.bufferRobot + Settings.get.bufferPerLevel * 30).toInt)
        stack
      })
    }

    GameRegistry.registerBlock(blockSimple, classOf[Item], Settings.namespace + "simple")
    GameRegistry.registerBlock(blockSimpleWithRedstone, classOf[Item], Settings.namespace + "simple_redstone")
    GameRegistry.registerBlock(blockSpecial, classOf[Item], Settings.namespace + "special")
    GameRegistry.registerBlock(blockSpecialWithRedstone, classOf[Item], Settings.namespace + "special_redstone")

    GameRegistry.registerTileEntity(classOf[tileentity.Adapter], Settings.namespace + "adapter")
    GameRegistry.registerTileEntity(classOf[tileentity.Cable], Settings.namespace + "cable")
    GameRegistry.registerTileEntity(classOf[tileentity.Capacitor], Settings.namespace + "capacitor")
    GameRegistry.registerTileEntity(classOf[tileentity.Case], Settings.namespace + "case")
    GameRegistry.registerTileEntity(classOf[tileentity.Charger], Settings.namespace + "charger")
    GameRegistry.registerTileEntity(classOf[tileentity.DiskDrive], Settings.namespace + "disk_drive")
    GameRegistry.registerTileEntity(classOf[tileentity.Keyboard], Settings.namespace + "keyboard")
    GameRegistry.registerTileEntity(classOf[tileentity.Hologram], Settings.namespace + "hologram")
    GameRegistry.registerTileEntity(classOf[tileentity.PowerConverter], Settings.namespace + "power_converter")
    GameRegistry.registerTileEntity(classOf[tileentity.PowerDistributor], Settings.namespace + "power_distributor")
    GameRegistry.registerTileEntity(classOf[tileentity.Redstone], Settings.namespace + "redstone")
    GameRegistry.registerTileEntity(classOf[tileentity.RobotProxy], Settings.namespace + "robot")
    GameRegistry.registerTileEntity(classOf[tileentity.Router], Settings.namespace + "router")
    GameRegistry.registerTileEntity(classOf[tileentity.Screen], Settings.namespace + "screen")
    GameRegistry.registerTileEntity(classOf[tileentity.Rack], Settings.namespace + "serverRack")
    GameRegistry.registerTileEntity(classOf[tileentity.WirelessRouter], Settings.namespace + "wireless_router")

    // IMPORTANT: the multi block must come first, since the sub blocks will
    // try to register with it. Also, the order the sub blocks are created in
    // must not be changed since that order determines their actual IDs.
    adapter = Recipes.addBlockDelegate(new Adapter(blockSimple), "adapter")
    cable = Recipes.addBlockDelegate(new Cable(blockSpecial), "cable")
    capacitor = Recipes.addBlockDelegate(new Capacitor(blockSimple), "capacitor")
    case1 = Recipes.addBlockDelegate(new Case.Tier1(blockSimpleWithRedstone), "case1")
    case2 = Recipes.addBlockDelegate(new Case.Tier2(blockSimpleWithRedstone), "case2")
    case3 = Recipes.addBlockDelegate(new Case.Tier3(blockSimpleWithRedstone), "case3")
    charger = Recipes.addBlockDelegate(new Charger(blockSimpleWithRedstone), "charger")
    diskDrive = Recipes.addBlockDelegate(new DiskDrive(blockSimple), "diskDrive")
    keyboard = Recipes.addBlockDelegate(new Keyboard(blockSpecial), "keyboard")
    powerDistributor = Recipes.addBlockDelegate(new PowerDistributor(blockSimple), "powerDistributor")
    powerConverter = Recipes.addBlockDelegate(new PowerConverter(blockSimple), "powerConverter")
    redstone = Recipes.addBlockDelegate(new Redstone(blockSimpleWithRedstone), "redstone")
    robotAfterimage = new RobotAfterimage(blockSpecial)
    robotProxy = Recipes.addBlockDelegate(new RobotProxy(blockSpecialWithRedstone), "robot")
    router = Recipes.addBlockDelegate(new Router(blockSimple), "router")
    screen1 = Recipes.addBlockDelegate(new Screen.Tier1(blockSimpleWithRedstone), "screen1")
    screen2 = Recipes.addBlockDelegate(new Screen.Tier2(blockSimpleWithRedstone), "screen2")
    screen3 = Recipes.addBlockDelegate(new Screen.Tier3(blockSimpleWithRedstone), "screen3")

    // For automatic conversion from old format (when screens did not take
    // redstone inputs) to keep save format compatible.
    blockSimple.subBlocks += screen1
    blockSimple.subBlocks += screen2
    blockSimple.subBlocks += screen3

    // v1.2.0
    serverRack = Recipes.addBlockDelegate(new Rack(blockSpecialWithRedstone), "rack")

    // v1.2.2
    hologram = Recipes.addBlockDelegate(new Hologram(blockSpecial), "hologram")
    wirelessRouter = Recipes.addBlockDelegate(new WirelessRouter(blockSimple), "wirelessRouter")

    // Initialize API.
    api.Blocks.AccessPoint = wirelessRouter.createItemStack()
    api.Blocks.Adapter = adapter.createItemStack()
    api.Blocks.Cable = cable.createItemStack()
    api.Blocks.Capacitor = capacitor.createItemStack()
    api.Blocks.Charger = charger.createItemStack()
    api.Blocks.CaseTier1 = case1.createItemStack()
    api.Blocks.CaseTier2 = case2.createItemStack()
    api.Blocks.CaseTier3 = case3.createItemStack()
    api.Blocks.DiskDrive = diskDrive.createItemStack()
    api.Blocks.Keyboard = keyboard.createItemStack()
    api.Blocks.HologramProjector = hologram.createItemStack()
    api.Blocks.PowerConverter = powerConverter.createItemStack()
    api.Blocks.PowerDistributor = powerDistributor.createItemStack()
    api.Blocks.RedstoneIO = redstone.createItemStack()
    api.Blocks.Robot = robotProxy.createItemStack()
    api.Blocks.Switch = router.createItemStack()
    api.Blocks.ScreenTier1 = screen1.createItemStack()
    api.Blocks.ScreenTier2 = screen2.createItemStack()
    api.Blocks.ScreenTier3 = screen3.createItemStack()
    api.Blocks.ServerRack = serverRack.createItemStack()

    // ----------------------------------------------------------------------- //

    register("oc:craftingCable", cable.createItemStack())
    register("oc:craftingCapacitor", capacitor.createItemStack())
    register("oc:craftingCaseTier1", case1.createItemStack())
    register("oc:craftingCaseTier2", case2.createItemStack())
    register("oc:craftingCaseTier3", case3.createItemStack())
    register("oc:craftingDiskDrive", diskDrive.createItemStack())
    register("oc:craftingKeyboard", keyboard.createItemStack())
    register("oc:craftingPowerDistributor", powerDistributor.createItemStack())
    register("oc:craftingRouter", router.createItemStack())
    register("oc:craftingScreenTier1", screen1.createItemStack())
    register("oc:craftingScreenTier2", screen2.createItemStack())
    register("oc:craftingScreenTier3", screen3.createItemStack())
    register("torchRedstoneActive", new ItemStack(Block.torchRedstoneActive, 1, 0))
  }

  private def register(name: String, item: ItemStack) {
    if (!OreDictionary.getOres(name).contains(item)) {
      OreDictionary.registerOre(name, item)
    }
  }
}