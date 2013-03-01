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
package org.inria.myriads.snoozenode.configurator.energymanagement;

/**
 * Thresholds settings.
 * 
 * @author Eugen Feller
 */
public class ThresholdSettings
{
    /** Idle time. */
    private int idleTime_;

    /** Wakeup time. */
    private int wakeupTime_;
    
    /** Empty. */
    public ThresholdSettings()
    {    
    }
        
    /**
     * Sets the idle time threshold.
     * 
     * @param idleTime     The idle time threshold
     */
    public void setIdleTime(int idleTime) 
    {
        idleTime_ = idleTime;
    }

    /**
     * Returns the idle time threshold.
     * 
     * @return  The idle time threshold
     */
    public int getIdleTime() 
    {
        return idleTime_;
    }

    /**
     * @param wakeupTime   The wakeup time
     */
    public void setWakeupTime(int wakeupTime) 
    {
        wakeupTime_ = wakeupTime;
    }

    /**
     * @return the wakeupTimeThreshold
     */
    public int getWakeupTime() 
    {
        return wakeupTime_;
    }
}
