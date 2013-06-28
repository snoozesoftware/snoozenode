package org.inria.myriads.snoozenode.message;

public class ManagementMessage
{
    /** event type.*/
    private ManagementMessageType managementMessageType_;
    
    /** message. */
    private Object message;

    /**
     * @param success
     * @param message
     */
    public ManagementMessage(ManagementMessageType messageType, Object message)
    {
        managementMessageType_ = messageType;
        this.message = message;
    }

    /**
     * @return the success
     */
    public ManagementMessageType getStatus()
    {
        return managementMessageType_;
    }

    /**
     * @param success the success to set
     */
    public void setStatus(ManagementMessageType messageType)
    {
        managementMessageType_ = messageType;
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
