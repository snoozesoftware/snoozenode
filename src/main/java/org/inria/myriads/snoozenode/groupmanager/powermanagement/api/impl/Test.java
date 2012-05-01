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
package org.inria.myriads.snoozenode.groupmanager.powermanagement.api.impl;

import org.inria.myriads.snoozenode.executor.ShellCommandExecuter;
import org.inria.myriads.snoozenode.groupmanager.powermanagement.api.WakeUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a dummy wakeup driver.
 * 
 * @author Eugen Feller
 */
public final class Test 
    implements WakeUp 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(Test.class);
    
    /** Shell command executor. */
    private ShellCommandExecuter executor_;
    
    /**
     * Constructor.
     * 
     * @param executor      The shell command executor
     */
    public Test(ShellCommandExecuter executor) 
    {
        executor_ = executor;
    }

    /** 
     * Test wakeup.
     * 
     * @param options    The options
     * @return           true if everything ok, false otherwise  
     */
    @Override
    public boolean wakeUp(String options) 
    {
        log_.debug(String.format("Test wakeup done on: %s!", options));
        boolean isExecuted = executor_.execute("ls");
        return isExecuted;
    }
}
