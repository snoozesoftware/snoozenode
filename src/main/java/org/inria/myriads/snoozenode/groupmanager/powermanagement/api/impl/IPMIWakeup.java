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
package org.inria.myriads.snoozenode.groupmanager.powermanagement.api.impl;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.executor.ShellCommandExecuter;
import org.inria.myriads.snoozenode.groupmanager.powermanagement.api.WakeUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements native IPMI communicator by wrapping calls to ipmitools.
 * 
 * @author Eugen Feller
 */
public final class IPMIWakeup 
    implements WakeUp
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(IPMIWakeup.class);
    
    /** Tool location. */
    private static final String IPMI_TOOLS_LOCATION = "sudo /usr/bin/ipmitool";
    
    /** Powerup command. */
    private static final String POWERON_COMMAND = "chassis power on";
    
    /** Shell command executor. */
    private ShellCommandExecuter executor_;
    
    /**
     * Constructor.
     * 
     * @param executor      The shell command executor
     */
    public IPMIWakeup(ShellCommandExecuter executor) 
    {
        executor_ = executor;
    }

    /** 
     * IPMI wakeup.
     * 
     * @param options    The options
     * @return           true if everything ok, false otherwise  
     */
    @Override
    public boolean wakeUp(String options) 
    {
        Guard.check(options);
        log_.debug("Executing the IPMI tool for wakeup");
        
        String command = IPMI_TOOLS_LOCATION + " " + options + " " + POWERON_COMMAND;
        log_.debug(String.format("Final IPMI command: %s", command));
        boolean isOn = executor_.execute(command);
        return isOn;
    }
}
