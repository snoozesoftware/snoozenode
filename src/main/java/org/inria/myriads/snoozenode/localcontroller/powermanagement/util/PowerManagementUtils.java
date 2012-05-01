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
package org.inria.myriads.snoozenode.localcontroller.powermanagement.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.localcontroller.powermanagement.suspend.SuspendState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains some helper functions for power management.
 * 
 * @author Eugen Feller
 */
public final class PowerManagementUtils 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(PowerManagementUtils.class);
    
    /** Command to check for supported power modes. */
    private static final String SUPPORTED_POWER_STATES = "/bin/cat /sys/power/state";
    
    /**
     * Hide the consturctor.
     */
    private PowerManagementUtils() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Checks if the suspend state is supported.
     * 
     * @param  suspendState         The suspend state
     * @return                      true if everything ok, false otherwise
     */
    public static boolean hasSuspendSupport(SuspendState suspendState) 
    {
        Guard.check(suspendState);
        log_.debug(String.format("Checking if the system supports suspend state: %s", suspendState));
        
        Process process = null; 
        boolean isSupported = false;
        try 
        {
            process = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", SUPPORTED_POWER_STATES});
            process.waitFor();

            InputStream inputStream = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            
            String content;
            while ((content = bufferedReader.readLine()) != null) 
            {
                log_.debug(String.format("Content: %s" , content));
                isSupported = hasState(suspendState, content);
                if (isSupported)
                {
                    break;
                }
            } 
        }
        catch (IOException exception) 
        {
            log_.error(String.format("Failed to execute the command: %s", exception.getMessage()));
        }
        catch (InterruptedException exception) 
        {    
            log_.error(String.format("Interrupted exception: %s", exception.getMessage()));
        } 
        catch (Exception exception)
        {
            log_.error(String.format("Exception: %s", exception.getMessage()));
        }
        finally
        {
            if (process != null) 
            {
                IOUtils.closeQuietly(process.getOutputStream());
                IOUtils.closeQuietly(process.getInputStream());
                IOUtils.closeQuietly(process.getErrorStream());
            }
        }
        
        return isSupported;  
    }
 
    /**
     * Check the content for a suspend state.
     * 
     * @param suspendState   The suspend state
     * @param content        The content      
     * @return               true if everything ok, false otherwise
     */
    private static boolean hasState(SuspendState suspendState, String content) 
    {
        Guard.check(suspendState, content);
        
        String splitWhitespace = "\\s";
        String[] states = content.split(splitWhitespace);
       
        for (int i = 0; i < states.length; i++)
        {
            SuspendState state = SuspendState.valueOf(states[i]);
            log_.debug("Checking existing state: " + state);
            if (suspendState.equals(state))
            {
                log_.debug("Supported power state found");
                return true;
            }
        }
        
        log_.debug("No supported suspent state found"); 
        return false;
    }
}
