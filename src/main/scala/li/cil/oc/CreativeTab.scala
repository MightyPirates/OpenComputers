package li.cil.oc

import li.cil.oc.common.init.Items
import net.minecraft.creativetab.CreativeTabs

object CreativeTab extends CreativeTabs(CreativeTabs.getNextID, "OpenComputers") {
  override def getTabIconItem = Items.multi

  override def getIconItemStack = Items.get("case1").createItemStack(1)

  override def getTranslatedTabLabel = getTabLabel
}