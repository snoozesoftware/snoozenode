package org.inria.snoozenode.external.notifier;

public enum ExternalNotificationType
{
    /** management messages (vm life cycle ...)*/
    MANAGEMENT("management"),
    
    /** system message. */
    SYSTEM("system"),
    
    /** monitoring message. */
    MONITORING("monitoring");
    
    private final String text;
    /**
     * @param text
     */
    private ExternalNotificationType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
