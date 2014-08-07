package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.block._
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.common.{Tier, tileentity}

object Blocks {
  var blockSimple: SimpleDelegator = _
  var blockSimpleWithRedstone: SimpleDelegator = _
  var blockSpecial: SpecialDelegator = _
  var blockSpecialWithRedstone: SpecialDelegator = _

  var robotProxy: RobotProxy = _
  var robotAfterimage: RobotAfterimage = _

  def init() {
    blockSimple = new SimpleDelegator()
    blockSimpleWithRedstone = new SimpleRedstoneDelegator()
    blockSpecial = new SpecialDelegator()
    blockSpecialWithRedstone = new SpecialRedstoneDelegator()

    GameRegistry.registerBlock(blockSimple, classOf[Item], "simple")
    GameRegistry.registerBlock(blockSimpleWithRedstone, classOf[Item], "simple_redstone")
    GameRegistry.registerBlock(blockSpecial, classOf[Item], "special")
    GameRegistry.registerBlock(blockSpecialWithRedstone, classOf[Item], "special_redstone")

    GameRegistry.registerTileEntity(classOf[tileentity.Adapter], Settings.namespace + "adapter")
    GameRegistry.registerTileEntity(classOf[tileentity.Cable], Settings.namespace + "cable")
    GameRegistry.registerTileEntity(classOf[tileentity.Capacitor], Settings.namespace + "capacitor")
    GameRegistry.registerTileEntity(classOf[tileentity.Case], Settings.namespace + "case")
    GameRegistry.registerTileEntity(classOf[tileentity.Charger], Settings.namespace + "charger")
    GameRegistry.registerTileEntity(classOf[tileentity.DiskDrive], Settings.namespace + "disk_drive")
    GameRegistry.registerTileEntity(classOf[tileentity.Disassembler], Settings.namespace + "disassembler")
    GameRegistry.registerTileEntity(classOf[tileentity.Keyboard], Settings.namespace + "keyboard")
    GameRegistry.registerTileEntity(classOf[tileentity.Hologram], Settings.namespace + "hologram")
    GameRegistry.registerTileEntity(classOf[tileentity.Geolyzer], Settings.namespace + "geolyzer")
    GameRegistry.registerTileEntity(classOf[tileentity.MotionSensor], Settings.namespace + "motion_sensor")
    GameRegistry.registerTileEntity(classOf[tileentity.PowerConverter], Settings.namespace + "power_converter")
    GameRegistry.registerTileEntity(classOf[tileentity.PowerDistributor], Settings.namespace + "power_distributor")
    GameRegistry.registerTileEntity(classOf[tileentity.Redstone], Settings.namespace + "redstone")
    GameRegistry.registerTileEntity(classOf[tileentity.RobotProxy], Settings.namespace + "robot")
    GameRegistry.registerTileEntity(classOf[tileentity.RobotAssembler], Settings.namespace + "robotAssembler")
    GameRegistry.registerTileEntityWithAlternatives(classOf[tileentity.Switch], Settings.namespace + "switch", Settings.namespace + "router")
    GameRegistry.registerTileEntity(classOf[tileentity.Screen], Settings.namespace + "screen")
    GameRegistry.registerTileEntity(classOf[tileentity.ServerRack], Settings.namespace + "serverRack")
    GameRegistry.registerTileEntityWithAlternatives(classOf[tileentity.AccessPoint], Settings.namespace + "access_point", Settings.namespace + "wireless_router")

    // IMPORTANT: the multi block must come first, since the sub blocks will
    // try to register with it. Also, the order the sub blocks are created in
    // must not be changed since that order determines their actual IDs.
    Recipes.addBlock(new Adapter(blockSimple), "adapter", "oc:adapter")
    Recipes.addBlock(new Cable(blockSpecial), "cable", "oc:cable")
    Recipes.addBlock(new Capacitor(blockSimple), "capacitor", "oc:capacitor")
    Recipes.addBlock(new Case(blockSimpleWithRedstone, Tier.One), "case1", "oc:case1")
    Recipes.addBlock(new Case(blockSimpleWithRedstone, Tier.Two), "case2", "oc:case2")
    Recipes.addBlock(new Case(blockSimpleWithRedstone, Tier.Three), "case3", "oc:case3")
    Recipes.addBlock(new Charger(blockSimpleWithRedstone), "charger", "oc:charger")
    Recipes.addBlock(new DiskDrive(blockSimple), "diskDrive", "oc:diskDrive")
    new KeyboardDeprecated(blockSpecial)
    Recipes.addBlock(new PowerDistributor(blockSimple), "powerDistributor", "oc:powerDistributor")
    Recipes.addBlock(new PowerConverter(blockSimple), "powerConverter", "oc:powerConverter")
    Recipes.addBlock(new Redstone(blockSimpleWithRedstone), "redstone", "oc:redstone")
    robotAfterimage = new RobotAfterimage(blockSpecial)
    robotProxy = Items.registerBlock(new RobotProxy(blockSpecialWithRedstone), "robot")
    Recipes.addBlock(new Switch(blockSimple), "switch", "oc:switch")

    // Copied to simple block for automatic conversion from old format (when
    // screens did not take redstone inputs) to keep save format compatible.
    blockSimple.subBlocks += Recipes.addBlock(new Screen(blockSimpleWithRedstone, Tier.One), "screen1", "oc:screen1")
    blockSimple.subBlocks += Recipes.addBlock(new Screen(blockSimpleWithRedstone, Tier.Two), "screen2", "oc:screen2")
    blockSimple.subBlocks += Recipes.addBlock(new Screen(blockSimpleWithRedstone, Tier.Three), "screen3", "oc:screen3")

    // v1.2.0
    Recipes.addBlock(new ServerRack(blockSpecialWithRedstone), "rack", "oc:rack")

    // MC 1.7
    Recipes.addNewBlock(new Keyboard(), "keyboard", "oc:keyboard")

    // v1.2.2
    Recipes.addBlock(new Hologram(blockSpecial, Tier.One), "hologram1", "oc:hologram1")
    Recipes.addBlock(new AccessPoint(blockSimple), "accessPoint", "oc:accessPoint")

    // v1.2.6
    Items.registerBlock(new Case(blockSimpleWithRedstone, Tier.Four), "caseCreative")

    // v1.3.0
    Recipes.addBlock(new Hologram(blockSpecial, Tier.Two), "hologram2", "oc:hologram2")
    Recipes.addBlock(new Geolyzer(blockSimple), "geolyzer", "oc:geolyzer")
    Recipes.addBlock(new RobotAssembler(blockSpecial), "robotAssembler", "oc:robotAssembler")
    Recipes.addBlock(new Disassembler(blockSimple), "disassembler", "oc:disassembler")

    // v1.3.2
    Recipes.addBlock(new MotionSensor(blockSimple), "motionSensor", "oc:motionSensor")
  }
}