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
 * Implements a driver to call the Grid5000 kapower3 tool.
 * 
 * @author Eugen Feller
 */
public final class KaPower3 
    implements WakeUp 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(KaPower3.class);
    
    /** Kapower3 location. */
    private static final String KAPOWER3_TOOL_LOCATION = "sudo /usr/bin/kapower3";
    
    /** Powerup command. */
    private static final String POWERON_COMMAND = "--on --no-wait";
    
    /** Shell command executor. */
    private ShellCommandExecuter executor_;
    
    /**
     * Constructor.
     * 
     * @param executor      The shell command executor
     */
    public KaPower3(ShellCommandExecuter executor) 
    {
        executor_ = executor;
    }
    
    /** 
     * KaPower3 wakeup.
     * 
     * @param options    The options
     * @return           true if everything ok, false otherwise  
     */
    @Override
    public boolean wakeUp(String options) 
    {
        Guard.check(options);
        log_.debug("Executing wakeup based on the kapower3 tool");
        
        String command = KAPOWER3_TOOL_LOCATION + " " + options + " " + POWERON_COMMAND;
        log_.debug(String.format("Executing kapower command: %s", command));
        boolean isOn = executor_.execute(command);
        
        log_.debug(String.format("KaPower finished with status: %s", isOn));
        return isOn;
    }
}
