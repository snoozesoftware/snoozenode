package org.inria.myriads.snoozenode.configurator.monitoring;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.localcontroller.Resource;

/**
 * @author msimonin
 *
 */
public class HostMonitorSettings
{
    /** Type of monitor.*/
    private HostMonitorType type_;
    
    /** Contact address.*/
    private NetworkAddress contactAddress_;
    
    /** Resources monitored by this monitor.*/
    private List<Resource> resources_;
    
    /** Interval.*/
    private int interval_;

    /**
     * 
     */
    public HostMonitorSettings()
    {
        resources_ = new ArrayList<Resource>(); 
    }

    /**
     * @return the type
     */
    public HostMonitorType getType()
    {
        return type_;
    }

    /**
     * @param type the type to set
     */
    public void setType(HostMonitorType type)
    {
        type_ = type;
    }

    /**
     * @return the contactAddress
     */
    public NetworkAddress getContactAddress()
    {
        return contactAddress_;
    }

    /**
     * @param contactAddress the contactAddress to set
     */
    public void setContactAddress(NetworkAddress contactAddress)
    {
        contactAddress_ = contactAddress;
    }

    /**
     * @return the resources
     */
    public List<Resource> getResources()
    {
        return resources_;
    }

    /**
     * @param resources the resources to set
     */
    public void setResources(List<Resource> resources)
    {
        resources_ = resources;
    }

    public void add(Resource resource)
    {
        resources_.add(resource);
    }

    /**
     * @return the interval
     */
    public int getInterval()
    {
        return interval_;
    }

    /**
     * @param interval the interval to set
     */
    public void setInterval(int interval)
    {
        interval_ = interval;
    }
    
    
    
}
