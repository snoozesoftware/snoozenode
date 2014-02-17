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
package org.inria.myriads.snoozenode.localcontroller.monitoring.api;

import java.util.ArrayList;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.HostMonitoringData;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitorSettings;
import org.inria.myriads.snoozenode.exception.HostMonitoringException;


/**
 * @author msimonin
 *
 */
/**
 * @author msimonin
 *
 */
public abstract class HostMonitor 
{
    
    protected HostMonitorSettings settings_;
    
    protected LocalControllerDescription localController_;
    
    /**
     * Initializes the host monitor (called after constructor and setter). 
     * @throws HostMonitoringException 
     */
    public abstract void initialize() throws HostMonitoringException;
    
    /**
     * Returns the total capacity.
     * 
     * @return                          The list of double values
     * @throws HostMonitoringException 
     */
    public abstract ArrayList<Double> getTotalCapacity() throws HostMonitoringException;

    /**
     * 
     * 
     * Gets the monitoring data in charge of this monitor.
     * 
     * @return
     */
    public abstract HostMonitoringData getResourceData() throws HostMonitoringException;

    /**
     * @return the hostMonitoringSettings
     */
    public HostMonitorSettings getSettings()
    {
        return settings_;
    }

    /**
     * @param hostMonitoringSettings the hostMonitoringSettings to set
     */
    public void setSettings(HostMonitorSettings settings)
    {
        settings_ = settings;
    }

    /**
     * @return the localController
     */
    public LocalControllerDescription getLocalController()
    {
        return localController_;
    }

    /**
     * @param localController the localController to set
     */
    public void setLocalController(LocalControllerDescription localController)
    {
        localController_ = localController;
    };
}
