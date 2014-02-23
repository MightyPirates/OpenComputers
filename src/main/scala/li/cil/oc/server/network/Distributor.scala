package li.cil.oc.server.network

trait Distributor {
  def globalBuffer: Double

  def globalBuffer_=(value: Double)

  def globalBufferSize: Double

  def globalBufferSize_=(value: Double)

  def addConnector(connector: Connector)

  def removeConnector(connector: Connector)

  def changeBuffer(delta: Double): Double
}
