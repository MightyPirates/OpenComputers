package forestry.api.genetics;
 
/*
 * Interface to be implemented by the enums representing the various chromosomes
 */
public interface IChromosomeType {
       
        /*
         * Get class which all alleles on this chromosome must interface
         */
        Class<? extends IAllele> getAlleleClass();
       
        String getName();
       
        ISpeciesRoot getSpeciesRoot();
       
        int ordinal();
 
}