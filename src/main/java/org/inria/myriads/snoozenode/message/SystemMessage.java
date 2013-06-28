package org.inria.myriads.snoozenode.message;

public class SystemMessage
{
    /** event type.*/
    private SystemMessageType eventType_;
    
    /** message. */
    private Object message;

    /**
     * @param eventType
     * @param message
     */
    public SystemMessage(SystemMessageType eventType, Object message)
    {
        eventType_ = eventType;
        this.message = message;
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
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(Object message)
    {
        this.message = message;
    }
    
    
}
