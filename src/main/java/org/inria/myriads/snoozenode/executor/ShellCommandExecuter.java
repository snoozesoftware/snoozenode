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
package org.inria.myriads.snoozenode.executor;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.TimeUtils;
import org.inria.myriads.snoozenode.executor.listener.ExecutorListener;
import org.inria.myriads.snoozenode.executor.thread.ExecutorThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shell command executer.
 * 
 * @author Eugen Feller
 */
public final class ShellCommandExecuter 
    implements ExecutorListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(ShellCommandExecuter.class);
    
    /** Timeout value. */
    private int timeout_;

    /** The result. */
    private boolean result_;
    
    /**
     * Shell command executir.
     * 
     * @param timeout   The timeout
     */
    public ShellCommandExecuter(int timeout)
    {
        Guard.check(timeout);
        timeout_ = timeout;
    }
    
    /**
     * Execute shell command.
     * 
     * @param command   The command to execute
     * @return          true if everything ok, false otherwise
     */
    public synchronized boolean execute(String command)
    {
        ExecutorThread executor = new ExecutorThread(command, this);
        Thread executorThread = new Thread(executor);
        executorThread.start();
        
        try 
        {
            Thread.sleep(TimeUtils.convertSecondsToMilliseconds(timeout_));
            executorThread.interrupt();
        }
        catch (InterruptedException exception) 
        {
            log_.debug(String.format("Shell command executor was interrupted: %s", exception.getMessage()));
        }
        
        log_.debug(String.format("Command finished with response: %s!", result_));
        return result_;
    }

    /**
     * Called upon command execution.
     * 
     * @param result    The result
     */
    @Override
    public void onCommandExecuted(boolean result) 
    {
        result_ = result;
    }
}
