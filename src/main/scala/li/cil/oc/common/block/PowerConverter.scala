package li.cil.oc.common.block

import java.text.DecimalFormat
import java.util

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.util.Tooltip
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class PowerConverter extends SimpleBlock with traits.PowerAcceptor {
  if (Settings.get.ignorePower) {
    setCreativeTab(null)
    ItemBlacklist.hide(this)
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput: Double = Settings.get.powerConverterRate

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.PowerConverter()
}
