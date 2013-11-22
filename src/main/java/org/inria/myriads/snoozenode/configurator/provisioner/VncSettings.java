package org.inria.myriads.snoozenode.configurator.provisioner;


/**
 * 
 * VNC settings.
 * 
 * @author msimonin
 *
 */
public class VncSettings
{
    /** Enable VNC console.*/
    private boolean enableVnc_;
    
    /** Listen address. */
    private String listenAddress_;
    
    /** startPort.*/
    private int startPort_;
    
    /** Start port.*/
    private int vncPortRange_;
    
    /** Keymap.*/
    private String keymap_;

    /**
     * @return the listenAddress
     */
    public String getListenAddress()
    {
        return listenAddress_;
    }

    /**
     * @param listenAddress the listenAddress to set
     */
    public void setListenAddress(String listenAddress)
    {
        listenAddress_ = listenAddress;
    }

    /**
     * @return the startPort
     */
    public int getStartPort()
    {
        return startPort_;
    }

    /**
     * @param startPort the startPort to set
     */
    public void setStartPort(int startPort)
    {
        startPort_ = startPort;
    }

    /**
     * @return the vncPortRange
     */
    public int getVncPortRange()
    {
        return vncPortRange_;
    }

    /**
     * @param vncPortRange the vncPortRange to set
     */
    public void setVncPortRange(int vncPortRange)
    {
        vncPortRange_ = vncPortRange;
    }

    /**
     * @return the enableVnc
     */
    public boolean isEnableVnc()
    {
        return enableVnc_;
    }

    /**
     * @param enableVnc the enableVnc to set
     */
    public void setEnableVnc(boolean enableVnc)
    {
        enableVnc_ = enableVnc;
    }

    /**
     * @return the keymap
     */
    public String getKeymap()
    {
        return keymap_;
    }

    /**
     * @param keymap the keymap to set
     */
    public void setKeymap(String keymap)
    {
        keymap_ = keymap;
    }
    
    
    
}
