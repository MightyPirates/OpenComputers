package li.cil.oc.common.init

import li.cil.oc.Constants
import li.cil.oc.CreativeTab
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.common.block._
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.material.Material
import net.minecraft.item.Rarity
import net.minecraft.item.Item

object Blocks {
  def init() {
    def defaultProps = Properties.of(Material.METAL).strength(2, 5)
    def defaultItemProps = new Item.Properties().tab(CreativeTab)
    Items.registerBlock(new Adapter(defaultProps), Constants.BlockName.Adapter, defaultItemProps)
    Items.registerBlock(new Assembler(defaultProps), Constants.BlockName.Assembler, defaultItemProps)
    Items.registerBlock(new Cable(defaultProps), Constants.BlockName.Cable, defaultItemProps)
    Items.registerBlock(new Capacitor(defaultProps), Constants.BlockName.Capacitor, defaultItemProps)
    Items.registerBlock(new Case(defaultProps, Tier.One), Constants.BlockName.CaseTier1, defaultItemProps)
    Items.registerBlock(new Case(defaultProps, Tier.Three), Constants.BlockName.CaseTier3, defaultItemProps.rarity(Rarity.RARE))
    Items.registerBlock(new Case(defaultProps, Tier.Two), Constants.BlockName.CaseTier2, defaultItemProps.rarity(Rarity.UNCOMMON))
    Items.registerBlock(new ChameliumBlock(Properties.of(Material.STONE).strength(2, 5)), Constants.BlockName.ChameliumBlock, defaultItemProps)
    Items.registerBlock(new Charger(defaultProps), Constants.BlockName.Charger, defaultItemProps)
    Items.registerBlock(new Disassembler(defaultProps), Constants.BlockName.Disassembler, defaultItemProps)
    Items.registerBlock(new DiskDrive(defaultProps), Constants.BlockName.DiskDrive, defaultItemProps)
    Items.registerBlock(new Geolyzer(defaultProps), Constants.BlockName.Geolyzer, defaultItemProps)
    Items.registerBlock(new Hologram(defaultProps, Tier.One), Constants.BlockName.HologramTier1, defaultItemProps)
    Items.registerBlock(new Hologram(defaultProps, Tier.Two), Constants.BlockName.HologramTier2, defaultItemProps.rarity(Rarity.UNCOMMON))
    Items.registerBlock(new Keyboard(Properties.of(Material.STONE).strength(2, 5).noOcclusion), Constants.BlockName.Keyboard, defaultItemProps)
    Items.registerBlock(new MotionSensor(defaultProps), Constants.BlockName.MotionSensor, defaultItemProps)
    Items.registerBlock(new PowerConverter(defaultProps), Constants.BlockName.PowerConverter,
      new Item.Properties().tab(if (!Settings.get.ignorePower) CreativeTab else null))
    Items.registerBlock(new PowerDistributor(defaultProps), Constants.BlockName.PowerDistributor, defaultItemProps)
    Items.registerBlock(new Printer(defaultProps), Constants.BlockName.Printer, defaultItemProps)
    Items.registerBlock(new Raid(defaultProps), Constants.BlockName.Raid, defaultItemProps)
    Items.registerBlock(new Redstone(defaultProps), Constants.BlockName.Redstone, defaultItemProps)
    Items.registerBlock(new Relay(defaultProps), Constants.BlockName.Relay, defaultItemProps)
    Items.registerBlock(new Screen(defaultProps, Tier.One), Constants.BlockName.ScreenTier1, defaultItemProps)
    Items.registerBlock(new Screen(defaultProps, Tier.Three), Constants.BlockName.ScreenTier3, defaultItemProps.rarity(Rarity.RARE))
    Items.registerBlock(new Screen(defaultProps, Tier.Two), Constants.BlockName.ScreenTier2, defaultItemProps.rarity(Rarity.UNCOMMON))
    Items.registerBlock(new Rack(defaultProps), Constants.BlockName.Rack, defaultItemProps)
    Items.registerBlock(new Waypoint(defaultProps), Constants.BlockName.Waypoint, defaultItemProps)

    Items.registerBlock(new Case(defaultProps, Tier.Four), Constants.BlockName.CaseCreative, defaultItemProps.rarity(Rarity.EPIC))
    Items.registerBlock(new Microcontroller(defaultProps), Constants.BlockName.Microcontroller, new Item.Properties())
    Items.registerBlock(new Print(Properties.of(Material.METAL).strength(1, 5).noOcclusion.dynamicShape), Constants.BlockName.Print, new Item.Properties())
    Items.registerBlockOnly(new RobotAfterimage(Properties.of(Material.AIR).instabreak.noOcclusion.dynamicShape.air), Constants.BlockName.RobotAfterimage)
    Items.registerBlock(new RobotProxy(defaultProps.noOcclusion.dynamicShape), Constants.BlockName.Robot, new Item.Properties())

    // v1.5.10
    Items.registerBlock(new FakeEndstone(Properties.of(Material.STONE).strength(3, 15)), Constants.BlockName.Endstone, defaultItemProps)

    // v1.5.14
    Items.registerBlock(new NetSplitter(defaultProps), Constants.BlockName.NetSplitter, defaultItemProps)

    // v1.5.16
    Items.registerBlock(new Transposer(defaultProps), Constants.BlockName.Transposer, defaultItemProps)

    // v1.7.2
    Items.registerBlock(new CarpetedCapacitor(defaultProps), Constants.BlockName.CarpetedCapacitor, defaultItemProps)
  }
}
