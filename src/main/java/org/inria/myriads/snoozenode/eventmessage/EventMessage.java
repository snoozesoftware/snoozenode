package org.inria.myriads.snoozenode.eventmessage;

public class EventMessage
{
    /** event type.*/
    private EventType eventType_;
    
    /** message. */
    private Object message;

    /**
     * @param eventType
     * @param message
     */
    public EventMessage(EventType eventType, Object message)
    {
        eventType_ = eventType;
        this.message = message;
    }

    /**
     * @return the eventType
     */
    public EventType getEventType()
    {
        return eventType_;
    }

    /**
     * @param eventType the eventType to set
     */
    public void setEventType(EventType eventType)
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
