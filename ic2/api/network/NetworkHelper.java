package ic2.api.network;

import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Provides methods to initiate events and synchronize tile entity fields in SMP.
 *
 * The methods are transparent between singleplayer and multiplayer - if a method is called in
 * singleplayer, the associated callback will be locally executed. The implementation is different
 * between the client and server versions of IC2.
 *
 * You'll usually want to use the server->client methods defined here to synchronize information
 * which is needed by the clients outside the GUI, such as rendering the block, playing sounds or
 * producing effects. Anything which is only visible inside the GUI should be synchronized through
 * the Container class associated to the GUI in Container.updateProgressBar().
 */
public final class NetworkHelper {
	// server -> client


	/**
	 * Schedule a TileEntity's field to be updated to the clients in range.
	 *
	 * The updater will query the field's value during the next update, updates happen usually
	 * every 2 ticks. If low latency is important use initiateTileEntityEvent instead.
	 *
	 * IC2's network updates have to get triggered every time, it doesn't continuously poll/send
	 * the field value. Just call updateTileEntityField after every change to a field which needs
	 * network synchronization.
	 *
	 * The following field data types are currently supported:
	 *  - int, int[], short, short[], byte, byte[], long, long[]
	 *  - float, float[], double, double[]
	 *  - boolean, boolean[]
	 *  - String, String[]
	 *  - ItemStack
	 *  - NBTBase (includes NBTTagCompound)
	 *  - Block, Item, Achievement, Potion, Enchantment
	 *  - ChunkCoordinates, ChunkCoordIntPair
	 *  - TileEntity (does not sync the actual tile entity, instead looks up the tile entity by its position in the client world)
	 *  - World (does not sync the actual world, instead looks up the world by its dimension ID)
	 *
	 * Once the update has been processed by the client, it'll call onNetworkUpdate on the client-
	 * side TileEntity if it implements INetworkUpdateListener.
	 *
	 * If this method is being executed on the client (i.e. Singleplayer), it'll just call
	 * INetworkUpdateListener.onNetworkUpdate (if implemented by the te).
	 *
	 * @param te TileEntity to update
	 * @param field Name of the field to update
	 */
	public static void updateTileEntityField(TileEntity te, String field) {
		try {
			if (NetworkManager_updateTileEntityField == null) NetworkManager_updateTileEntityField = Class.forName(getPackage() + ".core.network.NetworkManager").getMethod("updateTileEntityField", TileEntity.class, String.class);
			if (instance == null) instance = getInstance();

			NetworkManager_updateTileEntityField.invoke(instance, te, field);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Immediately send an event for the specified TileEntity to the clients in range.
	 *
	 * If this method is being executed on the client (i.e. Singleplayer), it'll just call
	 * INetworkTileEntityEventListener.onNetworkEvent (if implemented by the te).
	 *
	 * @param te TileEntity to notify, should implement INetworkTileEntityEventListener
	 * @param event Arbitrary integer to represent the event, choosing the values is up to you
	 * @param limitRange Limit the notification range to (currently) 20 blocks instead of the
	 *        tracking distance if true
	 */
	public static void initiateTileEntityEvent(TileEntity te, int event, boolean limitRange) {
		try {
			if (NetworkManager_initiateTileEntityEvent == null) NetworkManager_initiateTileEntityEvent = Class.forName(getPackage() + ".core.network.NetworkManager").getMethod("initiateTileEntityEvent", TileEntity.class, Integer.TYPE, Boolean.TYPE);
			if (instance == null) instance = getInstance();

			NetworkManager_initiateTileEntityEvent.invoke(instance, te, event, limitRange);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Immediately send an event for the specified Item to the clients in range.
	 *
	 * The item should implement INetworkItemEventListener to receive the event.
	 *
	 * If this method is being executed on the client (i.e. Singleplayer), it'll just call
	 * INetworkItemEventListener.onNetworkEvent (if implemented by the item).
	 *
	 * @param player EntityPlayer holding the item
	 * @param itemStack ItemStack containing the item
	 * @param event Arbitrary integer to represent the event, choosing the values is up to you
	 * @param limitRange Limit the notification range to (currently) 20 blocks instead of the
	 *        tracking distance if true
	 */
	public static void initiateItemEvent(EntityPlayer player, ItemStack itemStack, int event, boolean limitRange) {
		try {
			if (NetworkManager_initiateItemEvent == null) NetworkManager_initiateItemEvent = Class.forName(getPackage() + ".core.network.NetworkManager").getMethod("initiateItemEvent", EntityPlayer.class, ItemStack.class, Integer.TYPE, Boolean.TYPE);
			if (instance == null) instance = getInstance();

			NetworkManager_initiateItemEvent.invoke(instance, player, itemStack, event, limitRange);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Schedule a block update (re-render) on the clients in range.
	 *
	 * If this method is being executed on the client (i.e. Singleplayer), it'll just trigger the
	 * block update locally.
	 *
	 * @param world World containing the block
	 * @param x The block's x coordinate
	 * @param y The block's y coordinate
	 * @param z The block's z coordinate
	 */
	public static void announceBlockUpdate(World world, int x, int y, int z) {
		try {
			if (NetworkManager_announceBlockUpdate == null) NetworkManager_announceBlockUpdate = Class.forName(getPackage() + ".core.network.NetworkManager").getMethod("announceBlockUpdate", World.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
			if (instance == null) instance = getInstance();

			NetworkManager_announceBlockUpdate.invoke(instance, world, x, y, z);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	// client -> server


	/**
	 * Ask the server to send the values of the fields specified.
	 *
	 * See updateTileEntityField for the supported field types.
	 *
	 * The implementation is currently limited to TileEntitys as data providers. The tile entity
	 * has to be fully initialized when executing this method (i.e. valid worldObj+coords).
	 *
	 * This method doesn't do anything if executed on the server.
	 *
	 * @param dataProvider Object implementing the INetworkDataProvider interface
	 * 
	 * @deprecated no need to call this anymore, IC2 initiates it automatically
	 */
	@Deprecated
	public static void requestInitialData(INetworkDataProvider dataProvider) {
	}

	/**
	 * Immediately send an event for the specified TileEntity to the server.
	 *
	 * This method doesn't do anything if executed on the server.
	 *
	 * @param te TileEntity to notify, should implement INetworkClientTileEntityEventListener
	 * @param event Arbitrary integer to represent the event, choosing the values is up to you
	 */
	public static void initiateClientTileEntityEvent(TileEntity te, int event) {
		try {
			if (NetworkManager_initiateClientTileEntityEvent == null) NetworkManager_initiateClientTileEntityEvent = Class.forName(getPackage() + ".core.network.NetworkManager").getMethod("initiateClientTileEntityEvent", TileEntity.class, Integer.TYPE);
			if (instance == null) instance = getInstance();

			NetworkManager_initiateClientTileEntityEvent.invoke(instance, te, event);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Immediately send an event for the specified Item to the clients in range.
	 *
	 * The item should implement INetworkItemEventListener to receive the event.
	 *
	 * This method doesn't do anything if executed on the server.
	 *
	 * @param itemStack ItemStack containing the item
	 * @param event Arbitrary integer to represent the event, choosing the values is up to you
	 */
	public static void initiateClientItemEvent(ItemStack itemStack, int event) {
		try {
			if (NetworkManager_initiateClientItemEvent == null) NetworkManager_initiateClientItemEvent = Class.forName(getPackage() + ".core.network.NetworkManager").getMethod("initiateClientItemEvent", ItemStack.class, Integer.TYPE);
			if (instance == null) instance = getInstance();

			NetworkManager_initiateClientItemEvent.invoke(instance, itemStack, event);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the base IC2 package name, used internally.
	 *
	 * @return IC2 package name, if unable to be determined defaults to ic2
	 */
	private static String getPackage() {
		Package pkg = NetworkHelper.class.getPackage();

		if (pkg != null) {
			String packageName = pkg.getName();

			return packageName.substring(0, packageName.length() - ".api.network".length());
		}

		return "ic2";
	}

	/**
	 * Get the NetworkManager instance, used internally.
	 *
	 * @return NetworkManager instance
	 */
	private static Object getInstance() {
		try {
			return Class.forName(getPackage() + ".core.IC2").getDeclaredField("network").get(null);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static Object instance;
	private static Method NetworkManager_updateTileEntityField;
	private static Method NetworkManager_initiateTileEntityEvent;
	private static Method NetworkManager_initiateItemEvent;
	private static Method NetworkManager_announceBlockUpdate;
	private static Method NetworkManager_initiateClientTileEntityEvent;
	private static Method NetworkManager_initiateClientItemEvent;
}

