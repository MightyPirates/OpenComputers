package li.cil.oc.common.init

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.common.block._
import li.cil.oc.common.tileentity
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fml.common.registry.GameRegistry

object Blocks {
  def init() {
    Items.registerBlock(new Adapter(), Constants.BlockName.Adapter)
    Items.registerBlock(new Assembler(), Constants.BlockName.Assembler)
    Items.registerBlock(new Cable(), Constants.BlockName.Cable)
    Items.registerBlock(new Capacitor(), Constants.BlockName.Capacitor)
    Items.registerBlock(new Case(Tier.One), Constants.BlockName.CaseTier1)
    Items.registerBlock(new Case(Tier.Three), Constants.BlockName.CaseTier3)
    Items.registerBlock(new Case(Tier.Two), Constants.BlockName.CaseTier2)
    Items.registerBlock(new ChameliumBlock(), Constants.BlockName.ChameliumBlock)
    Items.registerBlock(new Charger(), Constants.BlockName.Charger)
    Items.registerBlock(new Disassembler(), Constants.BlockName.Disassembler)
    Items.registerBlock(new DiskDrive(), Constants.BlockName.DiskDrive)
    Items.registerBlock(new Geolyzer(), Constants.BlockName.Geolyzer)
    Items.registerBlock(new Hologram(Tier.One), Constants.BlockName.HologramTier1)
    Items.registerBlock(new Hologram(Tier.Two), Constants.BlockName.HologramTier2)
    Items.registerBlock(new Keyboard(), Constants.BlockName.Keyboard)
    Items.registerBlock(new MotionSensor(), Constants.BlockName.MotionSensor)
    Items.registerBlock(new PowerConverter(), Constants.BlockName.PowerConverter)
    Items.registerBlock(new PowerDistributor(), Constants.BlockName.PowerDistributor)
    Items.registerBlock(new Printer(), Constants.BlockName.Printer)
    Items.registerBlock(new Raid(), Constants.BlockName.Raid)
    Items.registerBlock(new Redstone(), Constants.BlockName.Redstone)
    Items.registerBlock(new Relay(), Constants.BlockName.Relay)
    Items.registerBlock(new Screen(Tier.One), Constants.BlockName.ScreenTier1)
    Items.registerBlock(new Screen(Tier.Three), Constants.BlockName.ScreenTier3)
    Items.registerBlock(new Screen(Tier.Two), Constants.BlockName.ScreenTier2)
    Items.registerBlock(new Rack(), Constants.BlockName.Rack)
    Items.registerBlock(new Waypoint(), Constants.BlockName.Waypoint)

    Items.registerBlock(new Case(Tier.Four), Constants.BlockName.CaseCreative)
    Items.registerBlock(new Microcontroller(), Constants.BlockName.Microcontroller)
    Items.registerBlock(new Print(), Constants.BlockName.Print)
    Items.registerBlock(new RobotAfterimage(), Constants.BlockName.RobotAfterimage)
    Items.registerBlock(new RobotProxy(), Constants.BlockName.Robot)

    // v1.5.10
    Items.registerBlock(new FakeEndstone(), Constants.BlockName.Endstone)

    // v1.5.14
    Items.registerBlock(new NetSplitter(), Constants.BlockName.NetSplitter)

    // v1.5.16
    Items.registerBlock(new Transposer(), Constants.BlockName.Transposer)

    // v1.7.2
    Items.registerBlock(new CarpetedCapacitor(), Constants.BlockName.CarpetedCapacitor)
  }
}
