package li.cil.oc.common.init

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.common.block._
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.common.tileentity

object Blocks {
  def init() {
    GameRegistry.registerTileEntity(classOf[tileentity.AccessPoint], Settings.namespace + "access_point")
    GameRegistry.registerTileEntity(classOf[tileentity.Adapter], Settings.namespace + "adapter")
    GameRegistry.registerTileEntityWithAlternatives(classOf[tileentity.Assembler], Settings.namespace + "assembler", Settings.namespace + "robotAssembler")
    GameRegistry.registerTileEntity(classOf[tileentity.Cable], Settings.namespace + "cable")
    GameRegistry.registerTileEntity(classOf[tileentity.Capacitor], Settings.namespace + "capacitor")
    GameRegistry.registerTileEntity(classOf[tileentity.Case], Settings.namespace + "case")
    GameRegistry.registerTileEntity(classOf[tileentity.Charger], Settings.namespace + "charger")
    GameRegistry.registerTileEntity(classOf[tileentity.DiskDrive], Settings.namespace + "disk_drive")
    GameRegistry.registerTileEntity(classOf[tileentity.Disassembler], Settings.namespace + "disassembler")
    GameRegistry.registerTileEntity(classOf[tileentity.Keyboard], Settings.namespace + "keyboard")
    GameRegistry.registerTileEntity(classOf[tileentity.Hologram], Settings.namespace + "hologram")
    GameRegistry.registerTileEntity(classOf[tileentity.Geolyzer], Settings.namespace + "geolyzer")
    GameRegistry.registerTileEntity(classOf[tileentity.Microcontroller], Settings.namespace + "microcontroller")
    GameRegistry.registerTileEntity(classOf[tileentity.MotionSensor], Settings.namespace + "motion_sensor")
    GameRegistry.registerTileEntity(classOf[tileentity.PowerConverter], Settings.namespace + "power_converter")
    GameRegistry.registerTileEntity(classOf[tileentity.PowerDistributor], Settings.namespace + "power_distributor")
    GameRegistry.registerTileEntity(classOf[tileentity.Print], Settings.namespace + "print")
    GameRegistry.registerTileEntity(classOf[tileentity.Printer], Settings.namespace + "printer")
    GameRegistry.registerTileEntity(classOf[tileentity.Raid], Settings.namespace + "raid")
    GameRegistry.registerTileEntity(classOf[tileentity.Redstone], Settings.namespace + "redstone")
    GameRegistry.registerTileEntity(classOf[tileentity.RobotProxy], Settings.namespace + "robot")
    GameRegistry.registerTileEntity(classOf[tileentity.Switch], Settings.namespace + "switch")
    GameRegistry.registerTileEntity(classOf[tileentity.Screen], Settings.namespace + "screen")
    GameRegistry.registerTileEntity(classOf[tileentity.ServerRack], Settings.namespace + "serverRack")

    // IMPORTANT: the multi block must come first, since the sub blocks will
    // try to register with it. Also, the order the sub blocks are created in
    // must not be changed since that order determines their actual IDs.
    Recipes.addBlock(new Adapter(), "adapter", "oc:adapter")
    Recipes.addBlock(new Cable(), "cable", "oc:cable")
    Recipes.addBlock(new Capacitor(), "capacitor", "oc:capacitor")
    Recipes.addBlock(new Case(Tier.One), "case1", "oc:case1")
    Recipes.addBlock(new Case(Tier.Two), "case2", "oc:case2")
    Recipes.addBlock(new Case(Tier.Three), "case3", "oc:case3")
    Recipes.addBlock(new Charger(), "charger", "oc:charger")
    Recipes.addBlock(new DiskDrive(), "diskDrive", "oc:diskDrive")
    Recipes.addBlock(new PowerDistributor(), "powerDistributor", "oc:powerDistributor")
    Recipes.addBlock(new PowerConverter(), "powerConverter", "oc:powerConverter")
    Recipes.addBlock(new Redstone(), "redstone", "oc:redstone")
    Items.registerBlock(new RobotAfterimage(), "robotAfterimage")
    Items.registerBlock(new RobotProxy(), "robot")
    Recipes.addBlock(new Switch(), "switch", "oc:switch")

    // Copied to simple block for automatic conversion from old format (when
    // screens did not take redstone inputs) to keep save format compatible.
    Recipes.addBlock(new Screen(Tier.One), "screen1", "oc:screen1")
    Recipes.addBlock(new Screen(Tier.Two), "screen2", "oc:screen2")
    Recipes.addBlock(new Screen(Tier.Three), "screen3", "oc:screen3")

    // v1.2.0
    Recipes.addBlock(new ServerRack(), "serverRack", "oc:serverRack")

    // MC 1.7
    Recipes.addBlock(new Keyboard(), "keyboard", "oc:keyboard")

    // v1.2.2
    Recipes.addBlock(new Hologram(Tier.One), "hologram1", "oc:hologram1")
    Recipes.addBlock(new AccessPoint(), "accessPoint", "oc:accessPoint")

    // v1.2.6
    Items.registerBlock(new Case(Tier.Four), "caseCreative")

    // v1.3.0
    Recipes.addBlock(new Hologram(Tier.Two), "hologram2", "oc:hologram2")
    Recipes.addBlock(new Geolyzer(), "geolyzer", "oc:geolyzer")
    Recipes.addBlock(new Assembler(), "assembler", "oc:assembler")
    Recipes.addBlock(new Disassembler(), "disassembler", "oc:disassembler")

    // v1.3.2
    Recipes.addBlock(new MotionSensor(), "motionSensor", "oc:motionSensor")

    // v1.4.2
    Recipes.addBlock(new Raid(), "raid", "oc:raid")
    Items.registerBlock(new Microcontroller(), "microcontroller")

    // v1.5.4
    Items.registerBlock(new Print(), "print")
    Recipes.addBlock(new Printer(), "printer", "oc:printer")
  }
}
