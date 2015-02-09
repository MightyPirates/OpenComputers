package li.cil.oc

import net.minecraft.creativetab.CreativeTabs

object CreativeTab extends CreativeTabs(CreativeTabs.getNextID, "OpenComputers") {
  private lazy val stack = api.Items.get("case1").createItemStack(1)

  override def getTabIconItem = stack.getItem

  override def getIconItemDamage = stack.getItemDamage

  override def getTranslatedTabLabel = getTabLabel
}
