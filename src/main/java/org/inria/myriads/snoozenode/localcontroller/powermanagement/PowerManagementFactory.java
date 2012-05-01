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
package org.inria.myriads.snoozenode.localcontroller.powermanagement;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.ShutdownDriver;
import org.inria.myriads.snoozenode.configurator.energymanagement.enums.SuspendDriver;
import org.inria.myriads.snoozenode.executor.ShellCommandExecuter;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.shutdown.Shutdown;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.shutdown.impl.IPMIShutdown;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.shutdown.impl.SystemShutdown;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.shutdown.impl.Test;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.suspend.Suspend;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.suspend.impl.PmUtils;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.suspend.impl.Uswsusp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Power management features factory.
 * 
 * @author Eugen Feller
 */
public final class PowerManagementFactory 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(PowerManagementFactory.class);
    
    /**
     * Hide the consturctor.
     */
    private PowerManagementFactory() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns suspend logic.
     * 
     * @param suspendDriver     The suspend driver
     * @param executor          The shell command executor
     * @return                  The suspend instance
     */
    public static Suspend newSuspendLogic(SuspendDriver suspendDriver, ShellCommandExecuter executor)
    {
        Guard.check(suspendDriver);
        log_.debug(String.format("The selected suspend driver is %s", suspendDriver));
        
        Suspend suspendLogic = null;       
        switch (suspendDriver)
        {
            case pmutils :
                suspendLogic =  new PmUtils(executor);
                break;
                
            case uswsusp: 
                suspendLogic =  new Uswsusp(executor);
                break;
            
            default:
                log_.error("Unknown suspend implementation selected!");
        }

        return suspendLogic;
    }
    
    /**
     * Returns shutdown logic.
     * 
     * @param shutdownDriver     The shutdown driver
     * @param executor          The shell command executor
     * @return                  The shutdown instance
     */
    public static Shutdown newShutdownLogic(ShutdownDriver shutdownDriver, ShellCommandExecuter executor)
    {
        Guard.check(shutdownDriver);
        log_.debug(String.format("The selected shutdown driver is %s", shutdownDriver));
        
        Shutdown shutdownLogic = null;       
        switch (shutdownDriver)
        {
            case IPMI :
                shutdownLogic = new IPMIShutdown(executor);
                break;
               
            case system: 
                shutdownLogic = new SystemShutdown(executor);
                break;
                
            case test: 
                shutdownLogic = new Test(executor);
                break;
              
            default:
                log_.error("Unknown shutdown implementation selected!");
        }

        return shutdownLogic;        
    }
}
