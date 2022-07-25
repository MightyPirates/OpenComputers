package li.cil.oc

import net.minecraft.item.ItemGroup

object CreativeTab extends ItemGroup(-1, OpenComputers.Name) {
  private lazy val stack = api.Items.get(Constants.BlockName.CaseTier1).createItemStack(1)

  override def makeIcon = stack
}
