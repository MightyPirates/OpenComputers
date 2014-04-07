package li.cil.oc

import net.minecraft.creativetab.CreativeTabs

object CreativeTab extends CreativeTabs(CreativeTabs.getNextID, "OpenComputers") {
  override def getTabIconItem = Items.multi

  override def getIconItemStack = Blocks.case1.createItemStack()

  override def displayAllReleventItems(list: java.util.List[_]) = {
    def add[T](list: java.util.List[T], value: Any) = list.add(value.asInstanceOf[T])
    super.displayAllReleventItems(list)
    Loot.disks.foreach(add(list, _))
  }

  override def getTranslatedTabLabel = getTabLabel
}