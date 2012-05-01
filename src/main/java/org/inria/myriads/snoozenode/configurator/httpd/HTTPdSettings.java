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
    
    /** Maximum number of connections. */
    private String maximumNumberOfConnections_;
    
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
