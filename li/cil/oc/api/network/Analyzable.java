package li.cil.oc.api.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Allows defining a callback for when a block is right-clicked with an
 * analyzer.
 * <p/>
 * This has to be implemented by a {@link net.minecraft.tileentity.TileEntity},
 * else it will have no effect.
 */
public interface Analyzable {
    /**
     * Called when a player uses the analyzer tool on the tile entity's block.
     * <p/>
     * This can be used to display additional block specific information in the
     * player's chat when the analyzer is used (or do whatever) and may also be
     * used to redirect the query to some other environment by returning some
     * other environment than <tt>this</tt>. The latter is used by multi-block
     * screens, for example, to always show information of the primary screen.
     * <p/>
     * Return <tt>null</tt> to suppress any further node information being
     * displayed.
     * <p/>
     * To display additional stats, add them to the stats compound. Each key
     * is localized and displayed together with the associated value in the
     * player's chat. For example:
     * <pre>
     * # Localization file:
     * mod:text.Analyzer.SpecialResult=Custom Title
     * </pre>
     * <pre>
     * // In onAnalyze implementation:
     * stats.setString("mod:text.Analyzer.SpecialResult", "my special info");
     * </pre>
     * will result in the following chat message on the client:
     * <pre>
     * Custom Title: my special info
     * </pre>
     *
     * @param stats  the compound in which to write stats to display.
     * @param player the player that used the analyzer.
     * @param side   the side of the block the player clicked.
     * @param hitX   the relative X coordinate the player clicked.
     * @param hitY   the relative Y coordinate the player clicked.
     * @param hitZ   the relative Z coordinate the player clicked.
     * @return the environment to display node information for, usually the
     *         environment itself (i.e. just return <tt>this</tt>).
     */
    Node onAnalyze(NBTTagCompound stats, EntityPlayer player, int side, float hitX, float hitY, float hitZ);
}
