/**
 * Copyright (C) 2010-2012 Eugen Feller, INRIA <eugen.feller@inria.fr>
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

import org.inria.myriads.snoozecommon.communication.localcontroller.wakeup.WakeupSettings;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.ShutdownDriver;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.SuspendDriver;

/**
 * Driver settings.
 * 
 * @author Eugen Feller
 */
public class DriverSettings 
{
    /** Shutdown driver. */
    private ShutdownDriver shutdown_;

    /** Suspend driver. */
    private SuspendDriver suspend_;

    /** Wakeup driver. */
    private WakeupSettings wakeup_;
       
    /** Constructor. */
    public DriverSettings()
    {
        wakeup_ = new WakeupSettings();
    }
        
    /**
     * Returns the wakeup driver.
     * 
     * @return  The wakeup settings
     */
    public WakeupSettings getWakeup() 
    {
        return wakeup_;
    }

    /**
     * Sets the shutdown driver.
     * 
     * @param shutdown    The shutdown driver
     */
    public void setShutdown(ShutdownDriver shutdown) 
    {
        shutdown_ = shutdown;
    }

    /**
     * Returns the shutdown driver.
     * 
     * @return      The shutdown driver
     */
    public ShutdownDriver getShutdown() 
    {
        return shutdown_;
    }

    /**
     * Sets the suspend driver.
     * 
     * @param shutdown     The suspend driver
     */
    public void setSuspend(SuspendDriver shutdown) 
    {
        suspend_ = shutdown;
    }

    /**
     * Returns the suspend driver.
     * 
     * @return The suspend driver
     */
    public SuspendDriver getSuspend() 
    {
        return suspend_;
    }
}
