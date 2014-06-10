/**
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2013. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computer.api;

/**
 * The interface that defines a peripheral. This should be implemented by the
 * TileEntity of any block that you wish to be interacted with by
 * computer or turtle.
 */
public interface IPeripheral
{
	/**
	 * Should return a string that uniquely identifies this type of peripheral.
	 * This can be queried from lua by calling peripheral.getType()
	 * @return 	A string identifying the type of peripheral.
	 */
    public String getType();
    
	/**
	 * Should return an array of strings that identify the methods that this 
	 * peripheral exposes to Lua. This will be called once before each attachment,
	 * and should not change when called multiple times.
	 * @return 	An array of strings representing method names.
	 * @see 	#callMethod
	 */
    public String[] getMethodNames();
    
	/**
	 * This is called when a lua program on an attached computer calls peripheral.call() with
	 * one of the methods exposed by getMethodNames().<br>
	 * <br>
	 * Be aware that this will be called from the ComputerCraft Lua thread, and must be thread-safe
	 * when interacting with minecraft objects.
	 * @param 	computer	The interface to the computer that is making the call. Remember that multiple
	 *						computers can be attached to a peripheral at once.
	 * @param	context		The context of the currently running lua thread. This can be used to wait for events
	 *						or otherwise yield.
	 * @param	method		An integer identifying which of the methods from getMethodNames() the computer
	 *						wishes to call. The integer indicates the index into the getMethodNames() table
	 *						that corresponds to the string passed into peripheral.call()
	 * @param	arguments	An array of objects, representing the arguments passed into peripheral.call().<br>
	 *						Lua values of type "string" will be represented by Object type String.<br>
	 *						Lua values of type "number" will be represented by Object type Double.<br>
	 *						Lua values of type "boolean" will be represented by Object type Boolean.<br>
	 *						Lua values of any other type will be represented by a null object.<br>
	 *						This array will be empty if no arguments are passed.
	 * @return 	An array of objects, representing values you wish to return to the lua program.<br>
	 *			Integers, Doubles, Floats, Strings, Booleans and null be converted to their corresponding lua type.<br>
	 *			All other types will be converted to nil.<br>
	 *			You may return null to indicate no values should be returned.
	 * @throws	Exception	If you throw any exception from this function, a lua error will be raised with the
	 *						same message as your exception. Use this to throw appropriate errors if the wrong
	 *						arguments are supplied to your method.
	 * @see 	#getMethodNames
	 */
    public Object[] callMethod( IComputerAccess computer, ILuaContext context, int method, Object[] arguments ) throws Exception;
    
	/**
	 * Is called before the computer attempts to attach to the peripheral, and should return whether to allow
	 * the attachment. Use this to restrict the number of computers that can attach, or to limit attachments to
	 * certain world directions.<br>
	 * If true is returned, attach() will be called shortly afterwards, and the computer will be able to make method calls.
	 * If false is returned, attach() will not be called, and the peripheral will be invisible to the computer.
	 * @param 	side		The world direction (0=bottom, 1=top, etc) that the computer lies relative to the peripheral.
	 * @return	Whether to allow the attachment, as a boolean.
	 * @see 	#attach
	 */
    public boolean canAttachToSide( int side );

	/**
	 * Is called when canAttachToSide has returned true, and a computer is attaching to the peripheral.
	 * This will occur when a peripheral is placed next to an active computer, when a computer is turned on next to a peripheral,
	 * or when a turtle travels into a square next to a peripheral.
	 * Between calls to attach() and detach(), the attached computer can make method calls on the peripheral using peripheral.call().
	 * This method can be used to keep track of which computers are attached to the peripheral, or to take action when attachment
	 * occurs.<br>
	 * <br>
	 * Be aware that this will be called from the ComputerCraft Lua thread, and must be thread-safe
	 * when interacting with minecraft objects.
	 * @param 	computer		The interface to the computer that is being attached. Remember that multiple
	 *							computers can be attached to a peripheral at once.
	 * @see		#canAttachToSide
	 * @see		#detach
	 */
    public void attach( IComputerAccess computer );

	/**
	 * Is called when a computer is detaching from the peripheral.
	 * This will occur when a computer shuts down, when the peripheral is removed while attached to computers,
	 * or when a turtle moves away from a square attached to a peripheral.
	 * This method can be used to keep track of which computers are attached to the peripheral, or to take action when detachment
	 * occurs.<br>
	 * <br>
	 * Be aware that this will be called from the ComputerCraft Lua thread, and must be thread-safe
	 * when interacting with minecraft objects.
	 * @param 	computer		The interface to the computer that is being detached. Remember that multiple
	 *							computers can be attached to a peripheral at once.
	 * @see		#canAttachToSide
	 * @see		#detach
	 */
    public void detach( IComputerAccess computer );
}
