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
 * Implements a Wake-on-Lan (WOL) driver.
 * 
 * @author Eugen Feller
 */
public final class WakeOnLan 
    implements WakeUp
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(WakeOnLan.class);

    /** Command to execute. */
    private static final String WAKE_ON_LAN_COMMAND = "sudo /usr/bin/wakeonlan";
 
    /** Shell command executor. */
    private ShellCommandExecuter executor_;
    
    /**
     * Constructor.
     * 
     * @param executor      The shell command executor
     */
    public WakeOnLan(ShellCommandExecuter executor) 
    {
        executor_ = executor;
    }
    
    /** 
     * Wake-On-Lan (WOL) wakeup.
     * 
     * @param options    The options
     * @return           true if everything ok, false otherwise  
     */
    @Override
    public boolean wakeUp(String options) 
    {
        Guard.check(options);
        
        String command = WAKE_ON_LAN_COMMAND + " " + options;
        log_.debug(String.format("Calling WOL for: %s" + command));
        boolean isExecuted = executor_.execute(command);
        return isExecuted;
    }
}
