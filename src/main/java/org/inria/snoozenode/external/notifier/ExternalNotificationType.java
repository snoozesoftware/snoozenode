package org.inria.snoozenode.external.notifier;

/**
 * 
 * Externale notification type.
 * 
 * @author msimonin
 *
 */
public enum ExternalNotificationType
{
    /** management messages (vm life cycle ...).*/
    MANAGEMENT("management"),
    
    /** system message. */
    SYSTEM("system"),
    
    /** monitoring message. */
    MONITORING("monitoring");
    
    /** The text.*/
    private final String text_;
    
    /**
     * @param text  The text.
     */
    private ExternalNotificationType(final String text) 
    {
        this.text_ = text;
    }

    @Override
    public String toString() 
    {
        return text_;
    }
}
