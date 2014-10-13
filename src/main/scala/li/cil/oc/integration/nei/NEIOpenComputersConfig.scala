package li.cil.oc.integration.nei

import codechicken.nei.api.API
import codechicken.nei.api.IConfigureNEI
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.integration.util.NEI
import net.minecraft.item.ItemStack

@SideOnly(Side.CLIENT)
class NEIOpenComputersConfig extends IConfigureNEI {
  override def getName = "OpenComputers"

  override def getVersion = "1.0.0"

  override def loadConfig() {
    API.registerUsageHandler(new DocumentationHandler())
    for (block <- NEI.hiddenBlocks) {
      API.hideItem(new ItemStack(block))
    }
  }
}
