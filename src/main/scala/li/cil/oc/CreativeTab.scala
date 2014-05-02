package li.cil.oc

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.common.Loot
import net.minecraft.creativetab.CreativeTabs

object CreativeTab extends CreativeTabs(CreativeTabs.getNextID, "OpenComputers") {
  @SideOnly(Side.CLIENT)
  override def getTabIconItemIndex = Settings.get.blockId2

  override def displayAllReleventItems(list: java.util.List[_]) = {
    def add[T](list: java.util.List[T], value: Any) = list.add(value.asInstanceOf[T])
    super.displayAllReleventItems(list)
    Loot.disks.foreach(add(list, _))
  }

  override def getTranslatedTabLabel = getTabLabel
}