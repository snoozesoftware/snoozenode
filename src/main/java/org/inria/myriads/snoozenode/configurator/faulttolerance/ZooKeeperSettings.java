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
package org.inria.myriads.snoozenode.configurator.faulttolerance;

/**
 * ZooKeeper parameters.
 * 
 * @author Eugen Feller
 */
public class ZooKeeperSettings 
{
    /** Comma separated list of hosts:port pais. */
    private String hosts_;
    
    /** Session timeout. */
    private int sessionTimeOut_;
    
    /**
     * Sets the hosts list.
     * 
     * @param hosts    The hosts list
     */
    public void setHosts(String hosts) 
    {
        hosts_ = hosts;
    }

    /**
     * Returns the hosts.
     * 
     * @return  The hosts
     */
    public String getHosts() 
    {
        return hosts_;
    }
    
    /**
     * Sets the session timeout.
     * 
     * @param sessionTimeOut   The session timeout
     */
    public void setSessionTimeout(int sessionTimeOut) 
    {
        sessionTimeOut_ = sessionTimeOut;
    }

    /**
     * Returns the zookeeper session timeout.
     * 
     * @return  The session timeout
     */
    public int getSessionTimeout() 
    {
        return sessionTimeOut_;
    }
}
