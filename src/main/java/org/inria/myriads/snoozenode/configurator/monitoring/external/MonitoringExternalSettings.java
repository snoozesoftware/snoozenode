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
package org.inria.myriads.snoozenode.configurator.monitoring.external;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.monitoring.TransportProtocol;

/**
 * Monitoring settings.
 * 
 * @author Eugen Feller
 */
public final class MonitoringExternalSettings 
{
    /** The transport protocol. */
    private TransportProtocol transportProtocol_; 

    /** Destination address.*/
    private NetworkAddress address_;
    
    /** Username (rabbitmq specific).*/
    private String username_;
    
    /** Password (rabbitmq specific).*/
    private String password_;
    
    /** vhost (rabbitmq specific).*/
    private String vhost;

    /** number of retries. */
    private int numberOfRetries_;
    
    /** retry interval (ms). */
    private int retryInterval_;
    
    /**
     * @return the transportProtocol
     */
    public TransportProtocol getTransportProtocol()
    {
        return transportProtocol_;
    }

    /**
     * @param transportProtocol the transportProtocol to set
     */
    public void setTransportProtocol(TransportProtocol transportProtocol)
    {
        transportProtocol_ = transportProtocol;
    }

    /**
     * @return the address
     */
    public NetworkAddress getAddress()
    {
        return address_;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(NetworkAddress address)
    {
        address_ = address;
    }

    /**
     * @return the username
     */
    public String getUsername()
    {
        return username_;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username)
    {
        username_ = username;
    }

    /**
     * @return the password
     */
    public String getPassword()
    {
        return password_;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password)
    {
        password_ = password;
    }

    /**
     * @return the vhost
     */
    public String getVhost()
    {
        return vhost;
    }

    /**
     * @param vhost the vhost to set
     */
    public void setVhost(String vhost)
    {
        this.vhost = vhost;
    }

    /**
     * @return the retryInterval
     */
    public int getRetryInterval()
    {
        return retryInterval_;
    }

    /**
     * @return the numberOfRetries
     */
    public int getNumberOfRetries()
    {
        return numberOfRetries_;
    }

    /**
     * @param numberOfRetries the numberOfRetries to set
     */
    public void setNumberOfRetries(int numberOfRetries)
    {
        numberOfRetries_ = numberOfRetries;
    }

    /**
     * @param retryInterval the retryInterval to set
     */
    public void setRetryInterval(int retryInterval)
    {
        retryInterval_ = retryInterval;
    }
    
    
    
}
