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
package org.inria.myriads.snoozenode.localcontroller.powermanagement.shutdown.impl;

import org.inria.myriads.snoozenode.executor.ShellCommandExecuter;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.shutdown.Shutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shutdown based on the native shutdown command.
 * 
 * @author Eugen Feller
 */
public final class SystemShutdown 
    implements Shutdown 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(SystemShutdown.class);

    /** Command to execute. */
    private static final String SHUTDOWN_COMMAND = "sudo /sbin/shutdown -h now";

    /** Executor. */
    private ShellCommandExecuter executor_;
    
    /**
     * Constructor.
     * 
     * @param executor  The executor
     */ 
    public SystemShutdown(ShellCommandExecuter executor) 
    {
        executor_ = executor;
    }
    
    /**
     * Will call the native "shutdown" command to turn off the resource.
     * 
     * @return      true if everything ok, false otherwise
     */
    @Override
    public boolean shutdown() 
    {
        log_.debug("Executing the native shutdown command");  
        boolean isShutdown = executor_.execute(SHUTDOWN_COMMAND);
        return isShutdown;
    }
}
