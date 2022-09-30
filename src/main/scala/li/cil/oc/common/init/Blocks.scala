package li.cil.oc.common.init

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.common.block._
import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.material.Material
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fml.common.registry.GameRegistry

object Blocks {
  def init() {
    def defaultProps = Properties.of(Material.METAL).strength(2, 5)
    Items.registerBlock(new Adapter(defaultProps), Constants.BlockName.Adapter)
    Items.registerBlock(new Assembler(defaultProps), Constants.BlockName.Assembler)
    Items.registerBlock(new Cable(defaultProps), Constants.BlockName.Cable)
    Items.registerBlock(new Capacitor(defaultProps), Constants.BlockName.Capacitor)
    Items.registerBlock(new Case(defaultProps, Tier.One), Constants.BlockName.CaseTier1)
    Items.registerBlock(new Case(defaultProps, Tier.Three), Constants.BlockName.CaseTier3)
    Items.registerBlock(new Case(defaultProps, Tier.Two), Constants.BlockName.CaseTier2)
    Items.registerBlock(new ChameliumBlock(Properties.of(Material.STONE).strength(2, 5)), Constants.BlockName.ChameliumBlock)
    Items.registerBlock(new Charger(defaultProps), Constants.BlockName.Charger)
    Items.registerBlock(new Disassembler(defaultProps), Constants.BlockName.Disassembler)
    Items.registerBlock(new DiskDrive(defaultProps), Constants.BlockName.DiskDrive)
    Items.registerBlock(new Geolyzer(defaultProps), Constants.BlockName.Geolyzer)
    Items.registerBlock(new Hologram(defaultProps, Tier.One), Constants.BlockName.HologramTier1)
    Items.registerBlock(new Hologram(defaultProps, Tier.Two), Constants.BlockName.HologramTier2)
    Items.registerBlock(new Keyboard(Properties.of(Material.STONE).strength(2, 5).noOcclusion), Constants.BlockName.Keyboard)
    Items.registerBlock(new MotionSensor(defaultProps), Constants.BlockName.MotionSensor)
    Items.registerBlock(new PowerConverter(defaultProps), Constants.BlockName.PowerConverter)
    Items.registerBlock(new PowerDistributor(defaultProps), Constants.BlockName.PowerDistributor)
    Items.registerBlock(new Printer(defaultProps), Constants.BlockName.Printer)
    Items.registerBlock(new Raid(defaultProps), Constants.BlockName.Raid)
    Items.registerBlock(new Redstone(defaultProps), Constants.BlockName.Redstone)
    Items.registerBlock(new Relay(defaultProps), Constants.BlockName.Relay)
    Items.registerBlock(new Screen(defaultProps, Tier.One), Constants.BlockName.ScreenTier1)
    Items.registerBlock(new Screen(defaultProps, Tier.Three), Constants.BlockName.ScreenTier3)
    Items.registerBlock(new Screen(defaultProps, Tier.Two), Constants.BlockName.ScreenTier2)
    Items.registerBlock(new Rack(defaultProps), Constants.BlockName.Rack)
    Items.registerBlock(new Waypoint(defaultProps), Constants.BlockName.Waypoint)

    Items.registerBlock(new Case(defaultProps, Tier.Four), Constants.BlockName.CaseCreative)
    Items.registerBlock(new Microcontroller(defaultProps), Constants.BlockName.Microcontroller)
    Items.registerBlock(new Print(Properties.of(Material.METAL).strength(1, 5).noOcclusion.dynamicShape), Constants.BlockName.Print)
    Items.registerBlock(new RobotAfterimage(Properties.of(Material.AIR).instabreak.noOcclusion.dynamicShape.air), Constants.BlockName.RobotAfterimage)
    Items.registerBlock(new RobotProxy(defaultProps.noOcclusion.dynamicShape), Constants.BlockName.Robot)

    // v1.5.10
    Items.registerBlock(new FakeEndstone(Properties.of(Material.STONE).strength(3, 15)), Constants.BlockName.Endstone)

    // v1.5.14
    Items.registerBlock(new NetSplitter(defaultProps), Constants.BlockName.NetSplitter)

    // v1.5.16
    Items.registerBlock(new Transposer(defaultProps), Constants.BlockName.Transposer)

    // v1.7.2
    Items.registerBlock(new CarpetedCapacitor(defaultProps), Constants.BlockName.CarpetedCapacitor)
  }
}
