package li.cil.oc

import net.minecraft.creativetab.CreativeTabs

object CreativeTab extends CreativeTabs(CreativeTabs.getNextID, "OpenComputers") {
  override def getTabIconItem = api.Items.get("case1").item()

  override def getTranslatedTabLabel = getTabLabel
}