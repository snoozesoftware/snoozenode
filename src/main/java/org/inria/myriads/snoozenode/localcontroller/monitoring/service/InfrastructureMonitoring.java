/**
 * Copyright (C) 2010-2013 Eugen Feller, INRIA <eugen.feller@inria.fr>
 *
 * This file is part of Snooze, a scalable, autonomic, and
 * energy-aware virtual machine (VM) management framework.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package org.inria.myriads.snoozenode.localcontroller.monitoring.service;

import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.HostMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.VirtualMachineMonitor;

/**
 * Resource monitoring class.
 * 
 * @author Eugen Feller
 */
public final class InfrastructureMonitoring 
{
    /** Virtual machine monitor. */
    private VirtualMachineMonitor virtualMachineMonitor_;
    
    /** Host monitoring. */
    private HostMonitor hostMonitor_;
    
    /** Monitoring settings. */
    private MonitoringSettings monitoringSettings_;
    
    /** Monitoring External Settngs. */
    private ExternalNotifierSettings monitoringExternalSettings_;
   
    /**
     * Constructor.
     * 
     * @param virtualMachineMonitor     The virtual machine monitor
     * @param hostMonitor               The host monitor
     * @param monitoringSettings        The monitoring settings
     */
    public InfrastructureMonitoring(VirtualMachineMonitor virtualMachineMonitor, 
                                    HostMonitor hostMonitor, 
                                    MonitoringSettings monitoringSettings,
                                    ExternalNotifierSettings monitoringExternalSettings
                                    ) 
    {
        virtualMachineMonitor_ = virtualMachineMonitor;
        hostMonitor_ = hostMonitor;
        monitoringSettings_ = monitoringSettings;
        monitoringExternalSettings_ = monitoringExternalSettings; 
    }

    /**
     * Returns the virtual machine monitor.
     * 
     * @return  The virtual machine monitor
     */
    public VirtualMachineMonitor getVirtualMachineMonitor() 
    {
        return virtualMachineMonitor_;
    }

    /**
     * Returns the host monitor.
     * 
     * @return  The host monitor
     */
    public HostMonitor getHostMonitor() 
    {
        return hostMonitor_;
    }
    
    /**
     * Returns the monitoring settings.
     * 
     * @return  The monitoring settings
     */
    public MonitoringSettings getMonitoringSettings() 
    {
        return monitoringSettings_;
    }

    /**
     * @return the monitoringExternalSettings
     */
    public ExternalNotifierSettings getMonitoringExternalSettings()
    {
        return monitoringExternalSettings_;
    }
}
