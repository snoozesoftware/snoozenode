package org.inria.myriads.snoozenode.message;

/**
 * 
 * Management message class.
 * 
 * @author msimonin
 *
 */
public class ManagementMessage
{
    /** event type.*/
    private ManagementMessageType managementMessageType_;
    
    /** message. */
    private Object message_;

    /**
     * @param messageType       The message type
     * @param message           The message.
     */
    public ManagementMessage(ManagementMessageType messageType, Object message)
    {
        managementMessageType_ = messageType;
        this.message_ = message;
    }

    /**
     * @return the success
     */
    public ManagementMessageType getStatus()
    {
        return managementMessageType_;
    }

    /**
     * @param messageType   The message type.
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
        return message_;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(Object message)
    {
        this.message_ = message;
    }
    
    
}
