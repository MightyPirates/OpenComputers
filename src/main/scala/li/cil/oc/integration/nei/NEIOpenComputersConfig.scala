package li.cil.oc.integration.nei

import codechicken.nei.NEIClientConfig
import codechicken.nei.api.API
import codechicken.nei.api.IConfigureNEI
import codechicken.nei.config.OptionToggleButton
import codechicken.nei.guihook.GuiContainerManager
import li.cil.oc.OpenComputers
import li.cil.oc.integration.util.ItemBlacklist
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
class NEIOpenComputersConfig extends IConfigureNEI {
  override def getName = OpenComputers.Name

  override def getVersion = OpenComputers.Version

  override def loadConfig() {
    // Non-alphabetic order haunts my OCD, but I want the "Manual" to show up
    // before the API doc.
    API.registerUsageHandler(new ManualUsageHandler())
    API.registerUsageHandler(new CallbackDocHandler())

    // Add option to show items' ore dictionary name in tooltips.
    NEIClientConfig.global.config.getTag("inventory.oredict").getBooleanValue(false)
    val oreDictOption = new OptionToggleButton("inventory.oredict", true)
    GuiContainerManager.addTooltipHandler(new OredictTooltipHandler())
    API.addOption(oreDictOption)

    for (stack <- ItemBlacklist.hiddenItems) {
      API.hideItem(stack())
    }
  }
}
