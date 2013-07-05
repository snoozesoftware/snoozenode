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
package org.inria.myriads.snoozenode.executor.thread;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.executor.listener.ExecutorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executor thread.
 * 
 * @author Eugen Feller
 */
public final class ExecutorThread 
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(ExecutorThread.class);
        
    /** Command. */
    private String command_;

    /** Execution listener. */
    private ExecutorListener executor_;
    
    /**
     * Wakeup watchdog.
     * 
     * @param command   The command
     * @param executor  The executor listener
     */
    public ExecutorThread(String command, ExecutorListener executor)
    {
        Guard.check(command, executor);
        command_ = command;
        executor_ = executor;
    }

    /**
     * Run method.
     */
    @Override
    public void run() 
    {
        log_.debug(String.format("Executing command on shell: %s", command_));
       
        int exitCode = 1;
        Process process = null;
        try 
        {
            process = Runtime.getRuntime().exec(command_);
            exitCode = process.waitFor();
        }
        catch (IOException exception) 
        {
            log_.error(String.format("Failed to execute the shell command: %s", exception.getMessage()));
        }
        catch (InterruptedException exception) 
        {
            log_.debug(String.format("Interrupted while waiting for process to finish"));
            process.destroy();
            return;
        }
        
        if (process != null) 
        {
            closeStreams(process);
        }

        if (exitCode == 0)
        {
            executor_.onCommandExecuted(true);
        } else
        {
            executor_.onCommandExecuted(false);
        }
    }
    
    /**
     * Closes process streams.
     * 
     * @param process   The process
     */
    private void closeStreams(Process process)
    {
        log_.debug("Closing streams!");
        IOUtils.closeQuietly(process.getOutputStream());
        IOUtils.closeQuietly(process.getInputStream());
        IOUtils.closeQuietly(process.getErrorStream()); 
        log_.debug("Streams closed!");
    }
}
