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
package org.inria.myriads.snoozenode.localcontroller.powermanagement.shutdown.impl;

import org.inria.myriads.snoozenode.executor.ShellCommandExecuter;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.shutdown.Shutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy implementation of the shutdown command.
 * 
 * @author Eugen Feller
 */
public final class Test 
    implements Shutdown
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(Test.class);
   
    /** Executor. */
    private ShellCommandExecuter executor_;
    
    /**
     * Constructor.
     * 
     * @param executor  The executor
     */ 
    public Test(ShellCommandExecuter executor) 
    {
        executor_ = executor;
    }
    
    /**
     * Kills the local controller.
     * 
     * @return      true if everything ok, false otherwise
     */
    @Override
    public boolean shutdown() 
    {
        log_.debug("Executing dummy node shutdown command");
        executor_.execute("ls");
        System.exit(0);
        return true;
    }
}
