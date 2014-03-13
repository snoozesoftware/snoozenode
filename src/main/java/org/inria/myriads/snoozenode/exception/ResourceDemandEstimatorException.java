package org.inria.myriads.snoozenode.exception;

/**
 * @author msimonin
 *
 */
public class ResourceDemandEstimatorException extends Exception
{

    
    /** Serial Id.*/
    private static final long serialVersionUID = 1L;

    /**
     * Empty Constructor.
     */
    public ResourceDemandEstimatorException()
    {
    }
    
    /**
     * @param message   The message.
     */
    public ResourceDemandEstimatorException(String message)
    {
        super(message);
    }

}
