package mrtjp.projectred.api;

/**
 * Implemented by entities that can emit bundled cable signals. If you are a
 * tile entity, see {@link IBundledTile}
 */
public interface IBundledEmitter
{
    /**
     * Returns the current emitted bundled cable strength for each colour. The
     * bytes are treated as having unsigned values from 0 to 255 - when
     * extracting a value from the array, you need to convert it (value & 255).
     * 
     * May return null, which is equivalent to returning an array with all
     * values 0.
     * 
     * Array indices are the same as the corresponding wool damage values.
     * 
     * For face parts, side is a rotation. For center parts or tile entities, it
     * is a forge direction.
     * 
     * The return value will be used immediately, so the returned array may be
     * overwritten by the next call to getBundledSignal.
     */
    public byte[] getBundledSignal(int side);
}
