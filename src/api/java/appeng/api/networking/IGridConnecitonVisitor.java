package appeng.api.networking;

public interface IGridConnecitonVisitor extends IGridVisitor
{

	/**
	 * Called for each connection on the network.
	 * 
	 * @param n
	 *            the connection.
	 */
	public void visitConnection(IGridConnection n);

}
