package li.cil.oc.common.tileentity

trait PowerInformation extends TileEntity {
  def globalBuffer: Double

  def globalBuffer_=(value: Double)

  def globalBufferSize: Double

  def globalBufferSize_=(value: Double)
}
