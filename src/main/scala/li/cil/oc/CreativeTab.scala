package li.cil.oc

import li.cil.oc.common.init.Items
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList

object CreativeTab extends ItemGroup(OpenComputers.Name) {
  private lazy val stack = api.Items.get(Constants.BlockName.CaseTier1).createItemStack(1)

  override def makeIcon = stack

  override def fillItemList(list: NonNullList[ItemStack]) {
    super.fillItemList(list)
    Items.decorateCreativeTab(list)
  }
}
