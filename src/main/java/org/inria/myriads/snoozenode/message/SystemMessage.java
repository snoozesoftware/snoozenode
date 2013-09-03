package org.inria.myriads.snoozenode.message;

/**
 * 
 * System message class.
 * 
 * @author msimonin
 *
 */
public class SystemMessage
{
    /** event type.*/
    private SystemMessageType eventType_;
    
    /** message. */
    private Object message_;

    /**
     * @param eventType     The event type.
     * @param message       The message.
     */
    public SystemMessage(SystemMessageType eventType, Object message)
    {
        eventType_ = eventType;
        this.message_ = message;
    }

    /**
     * @return the eventType
     */
    public SystemMessageType getEventType()
    {
        return eventType_;
    }

    /**
     * @param eventType the eventType to set
     */
    public void setEventType(SystemMessageType eventType)
    {
        eventType_ = eventType;
    }

    /**
     * @return the message
     */
    public Object getMessage()
    {
        return message_;
    }

    /**
     * @param message   The message to set
     */
    public void setMessage(Object message)
    {
        this.message_ = message;
    }
    
    
}
