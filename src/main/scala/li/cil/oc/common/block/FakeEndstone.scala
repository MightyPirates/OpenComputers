package li.cil.oc.common.block

import net.minecraft.block.material.Material

class FakeEndstone extends SimpleBlock(Material.rock) {
  setHardness(3)
  setResistance(15)

  override protected def customTextures = Array(
    Some("minecraft:end_stone"),
    Some("minecraft:end_stone"),
    Some("minecraft:end_stone"),
    Some("minecraft:end_stone"),
    Some("minecraft:end_stone"),
    Some("minecraft:end_stone")
  )
}
