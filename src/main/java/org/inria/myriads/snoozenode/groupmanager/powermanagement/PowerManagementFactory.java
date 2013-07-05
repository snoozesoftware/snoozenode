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
package org.inria.myriads.snoozenode.groupmanager.powermanagement;

import org.inria.myriads.snoozecommon.communication.localcontroller.wakeup.WakeupDriver;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.executor.ShellCommandExecuter;
import org.inria.myriads.snoozenode.groupmanager.powermanagement.api.WakeUp;
import org.inria.myriads.snoozenode.groupmanager.powermanagement.api.impl.IPMIWakeup;
import org.inria.myriads.snoozenode.groupmanager.powermanagement.api.impl.KaPower3;
import org.inria.myriads.snoozenode.groupmanager.powermanagement.api.impl.Test;
import org.inria.myriads.snoozenode.groupmanager.powermanagement.api.impl.WakeOnLan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Power management factory.
 * 
 * @author Eugen Feller
 */
public final class PowerManagementFactory 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(PowerManagementFactory.class);
    
    /** Hide constructor. */
    private PowerManagementFactory()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Creates the wakeup logic.
     * 
     * @param wakeupMethod      The selected wakeup method
     * @param executor          The shell command executor
     * @return                  The wakeup logic
     */
    public static WakeUp newWakeupDriver(WakeupDriver wakeupMethod, ShellCommandExecuter executor) 
    {
        Guard.check(wakeupMethod);
        log_.debug(String.format("The selected wakeup method is %s", wakeupMethod));
        
        WakeUp wakeupLogic = null;     
        switch (wakeupMethod)
        {
            case IPMI :
                wakeupLogic =  new IPMIWakeup(executor);
                break;
                
            case WOL :
                wakeupLogic = new WakeOnLan(executor);
                break;
                
                
            case kapower3: 
                wakeupLogic =  new KaPower3(executor);
                break;
                
            case test:
                wakeupLogic = new Test(executor);
                break;
            
            default:
                log_.error("Unknown wakeup method selected!");
        }
        
        return wakeupLogic;
    }
}
