package org.inria.myriads.snoozenode.configurator.provisioner;

/**
 * 
 * Image disk settings.
 * 
 * @author msimonin
 * 
 *
 */
public class ImageDiskSettings
{

    /** disk bus type. */
    private String diskBusType_;
    
    /** disk device. */
    private String diskDevice_;

    /**
     * @return the diskBusType
     */
    public String getDiskBusType()
    {
        return diskBusType_;
    }

    /**
     * @param diskBusType the diskBusType to set
     */
    public void setDiskBusType(String diskBusType)
    {
        diskBusType_ = diskBusType;
    }

    /**
     * @return the diskDevice
     */
    public String getDiskDevice()
    {
        return diskDevice_;
    }

    /**
     * @param diskDevice the diskDevice to set
     */
    public void setDiskDevice(String diskDevice)
    {
        diskDevice_ = diskDevice;
    }
    
    
}
