package li.cil.oc.server.network

trait Distributor {
  def globalBuffer: Double

  def globalBuffer_=(value: Double): Unit

  def globalBufferSize: Double

  def globalBufferSize_=(value: Double): Unit

  def addConnector(connector: Connector): Unit

  def removeConnector(connector: Connector): Unit

  def changeBuffer(delta: Double): Double
}
