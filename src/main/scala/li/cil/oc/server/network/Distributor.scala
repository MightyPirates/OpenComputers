package li.cil.oc.server.network

trait Distributor {
  def globalBuffer: Double

  def globalBuffer_=(value: Double): Unit

  def globalBufferSize: Double

  def globalBufferSize_=(value: Double): Unit

  def addConnector(connector: PowerNode): Unit

  def removeConnector(connector: PowerNode): Unit

  def changeBuffer(delta: Double): Double
}
