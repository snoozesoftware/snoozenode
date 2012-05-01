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
package org.inria.myriads.snoozenode.configurator.submission;

/**
 * Polling settings.
 * 
 * @author Eugen Feller
 */
public class PollingSettings 
{
    /** Number of retries. */
    private int numberOfRetries_;
    
    /** Retry interval. */
    private int retryInterval_;

    /**
     * Sets the number of retries.
     * 
     * @param numberOfRetries   The number of retries
     */
    public void setNumberOfRetries(int numberOfRetries) 
    {
        numberOfRetries_ =  numberOfRetries;
    }

    /**
     * Returns the number of retries.
     * 
     * @return      The number of retries
     */
    public int getNumberOfRetries() 
    {
        return numberOfRetries_;
    }

    /**
     * Sets the retry interval.
     * 
     * @param retryInterval     The retry interval
     */
    public void setRetryInterval(int retryInterval)
    {
        retryInterval_ = retryInterval;
    }

    /**
     * Returns the retry interval.
     * 
     * @return  The retry interval
     */
    public int getRetryInterval() 
    {
        return retryInterval_;
    }
}
