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
package org.inria.myriads.snoozenode.configurator.httpd;

/**
 * HTTPd settings.
 * 
 * @author Eugen Feller
 */
public final class HTTPdSettings 
{
    /** Maximum number of threads. */
    private String maxNumberOfThreads_;
    
    /**
     * @return the minThreads
     */
    public String getMinThreads()
    {
        return minThreads_;
    }

    /**
     * Sets the min Threads setting.
     * 
     * @param minThreads the minThreads to set
     */
    public void setMinThreads(String minThreads)
    {
        minThreads_ = minThreads;
    }

    /**
     * 
     * Gets the low Threads setting.
     * 
     * @return the lowThreads
     */
    public String getLowThreads()
    {
        return lowThreads_;
    }

    /**
     * 
     * Sets the low threads setting.
     * 
     * @param lowThreads the lowThreads to set
     */
    public void setLowThreads(String lowThreads)
    {
        lowThreads_ = lowThreads;
    }

    /**
     * 
     * Gets the max Threads setting.
     * 
     * @return the maxThreads
     */
    public String getMaxThreads()
    {
        return maxThreads_;
    }

    /**
     * 
     * Set the max Thread setting.
     * 
     * @param maxThreads the maxThreads to set
     */
    public void setMaxThreads(String maxThreads)
    {
        maxThreads_ = maxThreads;
    }

    /**
     * 
     * Gets the max queued setting.
     * 
     * @return the maxQueued
     */
    public String getMaxQueued()
    {
        return maxQueued_;
    }

    /**
     * 
     * Sets the max queued settings.
     * 
     * @param maxQueued the maxQueued to set
     */
    public void setMaxQueued(String maxQueued)
    {
        maxQueued_ = maxQueued;
    }

    
    /**
     * 
     * Gets the maxIoIdleTimeMs setting.
     * 
     * @return the maxIoIdleTimeMs
     */
    public String getMaxIoIdleTimeMs()
    {
        return maxIoIdleTimeMs;
    }

    /**
     * 
     * Sets the maxIoIdleTimeMs setting.
     * 
     * @param maxIoIdleTimeMs the maxIoIdleTimeMs to set
     */
    public void setMaxIoIdleTimeMs(String maxIoIdleTimeMs)
    {
        this.maxIoIdleTimeMs = maxIoIdleTimeMs;
    }

    /** Maximum number of connections. */
    private String maximumNumberOfConnections_;
    
    /** Min number of threads. */
    private String minThreads_; 
    
    /** Min number of threads. */
    private String lowThreads_;
    
    /** Max number of threads. */
    private String maxThreads_;
    
    /** Max number of threads queued. */
    private String maxQueued_;
    
    /** Max amount of time during which the server waits an io. */
    private String maxIoIdleTimeMs;
    
    /**
     * Sets the maximum number of threads.
     * 
     * @param maximumNumberOfThreads   The maximum number of threads
     */
    public void setMaximumNumberOfThreads(String maximumNumberOfThreads) 
    {
        maxNumberOfThreads_ = maximumNumberOfThreads;
    }

    /**
     * Returns the maximum number of threads.
     * 
     * @return  The maximum number of threads
     */
    public String getMaximumNumberOfThreads() 
    {
        return maxNumberOfThreads_;
    }

    /**
     * Sets the maximum number of connections.
     * 
     * @param maximumNumberOfConnections    The maximum number of connections
     */
    public void setMaximumNumberOfConnections(String maximumNumberOfConnections) 
    {
        maximumNumberOfConnections_ = maximumNumberOfConnections;
    }

    /**
     * Returns the maximum number of connections.
     * 
     * @return the maxNumberOfConnections   The maximum number of connections
     */
    public String getMaximumNumberOfConnections() 
    {
        return maximumNumberOfConnections_;
    }
}
