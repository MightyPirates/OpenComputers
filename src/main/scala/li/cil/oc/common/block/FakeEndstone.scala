package li.cil.oc.common.block

import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.BlockState
import net.minecraft.block.material.Material

class FakeEndstone extends SimpleBlock(Properties.of(Material.STONE).strength(3, 5)) {
}
