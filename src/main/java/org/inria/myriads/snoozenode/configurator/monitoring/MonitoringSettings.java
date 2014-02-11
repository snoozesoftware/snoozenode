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
package org.inria.myriads.snoozenode.configurator.monitoring;

import org.inria.myriads.snoozecommon.communication.localcontroller.MonitoringThresholds;

/**
 * Monitoring settings.
 * 
 * @author Eugen Feller
 */
public final class MonitoringSettings 
{
    /** Monitoring interval. */
    private int interval_;

    /** Monitoring timeout. */
    private int timeout_;
    
    /** Number of monitoring entries. */
    private int numberOfMonitoringEntries_;
    
    /** Monitoring thresholds. */
    private MonitoringThresholds thresholds_;
     
    /**
     * Sets the number of monitoring entries.
     * 
     * @param numberOfMonitoringEntries     The aggregated history size
     */
    public void setNumberOfMonitoringEntries(int numberOfMonitoringEntries) 
    {
        numberOfMonitoringEntries_ = numberOfMonitoringEntries;
    }

    /**
     * Returns the number of monitoring entries.
     * 
     * @return  The number of monitoring entries
     */
    public int getNumberOfMonitoringEntries() 
    {
        return numberOfMonitoringEntries_;
    }
    
    /**
     * Sets the monitoring timeout.
     * 
     * @param timeout  The monitor timeout
     */
    public void setTimeout(int timeout) 
    {
        timeout_ = timeout;
    }
    
    /**
     * Returns the monitor timeout.
     * 
     * @return  The monitor timeout
     */
    public int getTimeout()
    {
        return timeout_;
    }
        
    /**
     * Sets the monitoring interval.
     * 
     * @param interval   The monitoring interval
     */
    public void setInterval(int interval) 
    {
        interval_ = interval;
    }

    /**
     * Returns the monitoring interval.
     * 
     * @return The monitoring interval
     */
    public int getInterval() 
    {
        return interval_;
    }
    
    /**
     * Sets the monitoring thresholds.
     * 
     * @param thresholds      The monitoring thresholds
     */
    public void setThresholds(MonitoringThresholds thresholds)
    {
        thresholds_ = thresholds;
    }
    
    /**
     * Returns the monitoring thresholds.
     * 
     * @return      The monitoring thresholds
     */
    public MonitoringThresholds getThresholds()
    {
        return thresholds_;
    }
}
