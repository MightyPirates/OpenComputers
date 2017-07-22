package li.cil.oc.common.block

import li.cil.oc.util.Color
import net.minecraft.block.material.Material

class ChameliumBlock extends SimpleBlock(Material.rock) {
  override protected def customTextures = Array(
    Some("White"),
    Some("White"),
    Some("White"),
    Some("White"),
    Some("White"),
    Some("White")
  )

  override def getRenderColor(meta: Int): Int = Color.byOreName(Color.dyes(meta max 0 min Color.dyes.length))

  override def damageDropped(meta: Int): Int = meta
}
