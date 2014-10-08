package thaumcraft.api.nodes;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;

public interface INode extends IAspectContainer {

	/**
	 * Unique identifier to distinguish nodes. Normal node id's are based on world id and coordinates
	 * @return
	 */
	public String getId();
	
	public AspectList getAspectsBase();
	
	/**
	 * Return the type of node
	 * @return
	 */
	public NodeType getNodeType();

	/**
	 * Set the type of node
	 * @return
	 */
	public void setNodeType(NodeType nodeType);

	/**
	 * Return the node modifier
	 * @return
	 */
	public void setNodeModifier(NodeModifier nodeModifier);
	
	/**
	 * Set the node modifier
	 * @return
	 */
	public NodeModifier getNodeModifier();
		
	/**
	 * Return the maximum capacity of each aspect the node can hold
	 * @return
	 */
	public int getNodeVisBase(Aspect aspect);

	/**
	 * Set the maximum capacity of each aspect the node can hold
	 * @return
	 */
	public void setNodeVisBase(Aspect aspect, short nodeVisBase);
	
}
