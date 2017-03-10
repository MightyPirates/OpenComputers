package li.cil.oc.common.init

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.common.block._
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.{TileEntityMotionSensor, TileEntityCable, TileEntityCapacitor, TileEntityGeolyzer, TileEntityKeyboard, TileEntityPowerConverter, TileEntityRedstoneIO, TileEntityWaypoint}
import net.minecraftforge.fml.common.registry.GameRegistry

object Blocks {
  def init() {
    GameRegistry.registerTileEntity(classOf[tileentity.AccessPoint], Settings.namespace + "accessPoint")
    GameRegistry.registerTileEntity(classOf[tileentity.TileEntityAdapter], Settings.namespace + "adapter")
    GameRegistry.registerTileEntity(classOf[tileentity.Assembler], Settings.namespace + "assembler")
    GameRegistry.registerTileEntity(classOf[TileEntityCable], Settings.namespace + "cable")
    GameRegistry.registerTileEntity(classOf[TileEntityCapacitor], Settings.namespace + "capacitor")
    GameRegistry.registerTileEntity(classOf[tileentity.Case], Settings.namespace + "case")
    GameRegistry.registerTileEntity(classOf[tileentity.Charger], Settings.namespace + "charger")
    GameRegistry.registerTileEntity(classOf[tileentity.TileEntityDiskDrive], Settings.namespace + "diskDrive")
    GameRegistry.registerTileEntity(classOf[tileentity.Disassembler], Settings.namespace + "disassembler")
    GameRegistry.registerTileEntity(classOf[TileEntityKeyboard], Settings.namespace + "keyboard")
    GameRegistry.registerTileEntity(classOf[tileentity.Hologram], Settings.namespace + "hologram")
    GameRegistry.registerTileEntity(classOf[TileEntityGeolyzer], Settings.namespace + "geolyzer")
    GameRegistry.registerTileEntity(classOf[tileentity.Microcontroller], Settings.namespace + "microcontroller")
    GameRegistry.registerTileEntity(classOf[TileEntityMotionSensor], Settings.namespace + "motionSensor")
    GameRegistry.registerTileEntity(classOf[tileentity.TileEntityNetSplitter], Settings.namespace + "netSplitter")
    GameRegistry.registerTileEntity(classOf[TileEntityPowerConverter], Settings.namespace + "powerConverter")
    GameRegistry.registerTileEntity(classOf[tileentity.TileEntityPowerDistributor], Settings.namespace + "powerDistributor")
    GameRegistry.registerTileEntity(classOf[tileentity.Print], Settings.namespace + "print")
    GameRegistry.registerTileEntity(classOf[tileentity.Printer], Settings.namespace + "printer")
    GameRegistry.registerTileEntity(classOf[tileentity.Raid], Settings.namespace + "raid")
    GameRegistry.registerTileEntity(classOf[TileEntityRedstoneIO], Settings.namespace + "redstone")
    GameRegistry.registerTileEntity(classOf[tileentity.Relay], Settings.namespace + "relay")
    GameRegistry.registerTileEntity(classOf[tileentity.RobotProxy], Settings.namespace + "robot")
    GameRegistry.registerTileEntity(classOf[tileentity.Switch], Settings.namespace + "switch")
    GameRegistry.registerTileEntity(classOf[tileentity.Screen], Settings.namespace + "screen")
    GameRegistry.registerTileEntityWithAlternatives(classOf[tileentity.Rack], Settings.namespace + "rack", Settings.namespace + "serverRack")
    GameRegistry.registerTileEntity(classOf[tileentity.TileEntityTransposer], Settings.namespace + "transposer")
    GameRegistry.registerTileEntity(classOf[TileEntityWaypoint], Settings.namespace + "waypoint")

    Items.registerBlock(new AccessPoint(), Constants.BlockName.AccessPoint)
    Recipes.addBlock(new BlockAdapter(), Constants.BlockName.Adapter, "oc:adapter")
    Recipes.addBlock(new Assembler(), Constants.BlockName.Assembler, "oc:assembler")
    Recipes.addBlock(new BlockCable(), Constants.BlockName.Cable, "oc:cable")
    Recipes.addBlock(new BlockCapacitor(), Constants.BlockName.Capacitor, "oc:capacitor")
    Recipes.addBlock(new Case(Tier.One), Constants.BlockName.CaseTier1, "oc:case1")
    Recipes.addBlock(new Case(Tier.Three), Constants.BlockName.CaseTier3, "oc:case3")
    Recipes.addBlock(new Case(Tier.Two), Constants.BlockName.CaseTier2, "oc:case2")
    Recipes.addBlock(new ChameliumBlock(), Constants.BlockName.ChameliumBlock, "oc:chameliumBlock")
    Recipes.addBlock(new Charger(), Constants.BlockName.Charger, "oc:charger")
    Recipes.addBlock(new Disassembler(), Constants.BlockName.Disassembler, "oc:disassembler")
    Recipes.addBlock(new BlockDiskDrive(), Constants.BlockName.DiskDrive, "oc:diskDrive")
    Recipes.addBlock(new BlockGeolyzer(), Constants.BlockName.Geolyzer, "oc:geolyzer")
    Recipes.addBlock(new Hologram(Tier.One), Constants.BlockName.HologramTier1, "oc:hologram1")
    Recipes.addBlock(new Hologram(Tier.Two), Constants.BlockName.HologramTier2, "oc:hologram2")
    Recipes.addBlock(new Keyboard(), Constants.BlockName.Keyboard, "oc:keyboard")
    Recipes.addBlock(new BlockMotionSensor(), Constants.BlockName.MotionSensor, "oc:motionSensor")
    Recipes.addBlock(new PowerConverter(), Constants.BlockName.PowerConverter, "oc:powerConverter")
    Recipes.addBlock(new BlockPowerDistributor(), Constants.BlockName.PowerDistributor, "oc:powerDistributor")
    Recipes.addBlock(new Printer(), Constants.BlockName.Printer, "oc:printer")
    Recipes.addBlock(new Raid(), Constants.BlockName.Raid, "oc:raid")
    Recipes.addBlock(new BlockRedstoneIO(), Constants.BlockName.Redstone, "oc:redstone")
    Recipes.addBlock(new Relay(), Constants.BlockName.Relay, "oc:relay")
    Recipes.addBlock(new Screen(Tier.One), Constants.BlockName.ScreenTier1, "oc:screen1")
    Recipes.addBlock(new Screen(Tier.Three), Constants.BlockName.ScreenTier3, "oc:screen3")
    Recipes.addBlock(new Screen(Tier.Two), Constants.BlockName.ScreenTier2, "oc:screen2")
    Recipes.addBlock(new Rack(), Constants.BlockName.Rack, "oc:rack", "oc:rack")
    Items.registerBlock(new Switch(), Constants.BlockName.Switch)
    Recipes.addBlock(new Waypoint(), Constants.BlockName.Waypoint, "oc:waypoint")

    Items.registerBlock(new Case(Tier.Four), Constants.BlockName.CaseCreative)
    Items.registerBlock(new Microcontroller(), Constants.BlockName.Microcontroller)
    Items.registerBlock(new Print(), Constants.BlockName.Print)
    Items.registerBlock(new RobotAfterimage(), Constants.BlockName.RobotAfterimage)
    Items.registerBlock(new RobotProxy(), Constants.BlockName.Robot)

    // v1.5.10
    Recipes.addBlock(new FakeEndstone(), Constants.BlockName.Endstone, "oc:stoneEndstone")

    // v1.5.14
    Recipes.addBlock(new BlockNetSplitter(), Constants.BlockName.NetSplitter, "oc:netSplitter")

    // v1.5.16
    Recipes.addBlock(new BlockTransposer(), Constants.BlockName.Transposer, "oc:transposer")
  }
}
