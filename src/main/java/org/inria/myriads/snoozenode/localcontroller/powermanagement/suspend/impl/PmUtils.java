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
package org.inria.myriads.snoozenode.localcontroller.powermanagement.suspend.impl;

import org.inria.myriads.snoozenode.executor.ShellCommandExecuter;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.suspend.Suspend;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.suspend.SuspendState;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.util.PowerManagementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around pm-utils package.
 * 
 * @author Eugen Feller
 */
public final class PmUtils 
    implements Suspend 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(PmUtils.class);
    
    /** Suspend to ram command. */
    private static final String SUSPEND_TO_RAM_COMMAND = "sudo /usr/sbin/pm-suspend";
    
    /** Suspend to disk command. */
    private static final String SUSPEND_TO_DISK_COMMAND = "sudo /usr/sbin/pm-hibernate";
    
    /** Suspend to both command. */
    private static final String SUSPEND_TO_BOTH_COMMAND = "sudo /usr/sbin/pm-suspend-hybrid";

    /** Executor. */
    private ShellCommandExecuter executor_;
    
    /**
     * Constructor.
     * 
     * @param executor  The executor
     */ 
    public PmUtils(ShellCommandExecuter executor) 
    {
        executor_ = executor;
    }

    /**
     * Suspends the node to ram.
     * 
     * @return      true if everything ok, false otherwise
     */
    @Override
    public boolean suspendToRam() 
    {
        log_.debug("Executing suspend to ram!");
        boolean isSupported = PowerManagementUtils.hasSuspendSupport(SuspendState.mem);
        if (!isSupported) 
        {
            log_.debug("Suspend to ram is not supported by your system");
            return false;
        }
        
        boolean isSuspended = executor_.execute(SUSPEND_TO_RAM_COMMAND);
        return isSuspended;
    }

    /** 
     * Suspends the node to disk.
     * 
     * @return      true if everything ok, false otherwise
     */
    @Override
    public boolean suspendToDisk() 
    {
        log_.debug("Executing suspend to disk!");
        boolean isSupported = PowerManagementUtils.hasSuspendSupport(SuspendState.disk);
        if (!isSupported) 
        {
            log_.debug("Suspend to disk is not supported by your system");
            return false;
        }
        
        boolean isSuspended = executor_.execute(SUSPEND_TO_DISK_COMMAND);
        return isSuspended;
    }
    
    /** 
     * Suspends the node to disk and memory.
     * 
     * @return      true if everything ok, false otherwise
     */
    @Override
    public boolean suspendToBoth() 
    {
        log_.debug("Executing suspend to both!");
        boolean isSupported = PowerManagementUtils.hasSuspendSupport(SuspendState.both);
        if (!isSupported) 
        {
            log_.debug("Suspend to both (disk and memory) is not supported by your system");
            return false;
        }
        
        boolean isSuspended = executor_.execute(SUSPEND_TO_BOTH_COMMAND);
        return isSuspended;
    }
}
