package li.cil.oc

import net.minecraft.creativetab.CreativeTabs

object CreativeTab extends CreativeTabs(CreativeTabs.getNextID, "OpenComputers") {
  override def getTabIconItem = Items.multi

  override def getIconItemStack = Blocks.case1.createItemStack()

  override def getTranslatedTabLabel = getTabLabel
}