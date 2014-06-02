package org.inria.myriads.snoozenode.configurator.provisioner;

/**
 * @author msimonin
 *
 */
public class ProvisionerSettings
{
    /** Enable a serial console. */
    private boolean enableSerial_;
    
    /** Vnc settings. */
    private VncSettings vncSettings_;
    
    /** Image disk settings. */
    private ImageDiskSettings firstHdSettings_;
    
    /** Image disk settings. */
    private ImageDiskSettings firstCdSettings_;

    
    public ProvisionerSettings()
    {
        firstHdSettings_ = new ImageDiskSettings();
        firstCdSettings_ = new ImageDiskSettings();
        vncSettings_ = new VncSettings();
    }
    
    /**
     * @return the enableSerial
     */
    public boolean isEnableSerial()
    {
        return enableSerial_;
    }

    /**
     * @param enableSerial the enableSerial to set
     */
    public void setEnableSerial(boolean enableSerial)
    {
        enableSerial_ = enableSerial;
    }

    /**
     * @return the vncSettings
     */
    public VncSettings getVncSettings()
    {
        return vncSettings_;
    }

    /**
     * @param vncSettings the vncSettings to set
     */
    public void setVncSettings(VncSettings vncSettings)
    {
        vncSettings_ = vncSettings;
    }

    /**
     * @return the firstHdSettings
     */
    public ImageDiskSettings getFirstHdSettings()
    {
        return firstHdSettings_;
    }


    /**
     * @param firstHdSettings the firstHdSettings to set
     */
    public void setFirstHdSettings(ImageDiskSettings firstHdSettings)
    {
        firstHdSettings_ = firstHdSettings;
    }

    /**
     * @return the firstCdSettings
     */
    public ImageDiskSettings getFirstCdSettings()
    {
        return firstCdSettings_;
    }

    /**
     * @param firstCdSettings the firstCdSettings to set
     */
    public void setFirstCdSettings(ImageDiskSettings firstCdSettings)
    {
        firstCdSettings_ = firstCdSettings;
    }
    
    
}
