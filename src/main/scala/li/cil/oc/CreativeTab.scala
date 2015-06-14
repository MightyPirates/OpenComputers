package li.cil.oc

import net.minecraft.creativetab.CreativeTabs

object CreativeTab extends CreativeTabs(CreativeTabs.getNextID, OpenComputers.Name) {
  private lazy val stack = api.Items.get(Constants.BlockName.CaseTier1).createItemStack(1)

  override def getTabIconItem = stack.getItem

  override def getIconItemStack = stack

  override def getTranslatedTabLabel = getTabLabel
}
