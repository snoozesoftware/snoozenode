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
package org.inria.myriads.snoozenode.configurator.node;

import org.inria.myriads.snoozecommon.communication.NodeRole;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.NetworkDemand;
import org.inria.myriads.snoozenode.idgenerator.enums.IdGeneration;

/**
 * Node settings.
 * 
 * @author Eugen Feller
 */
public final class NodeSettings 
{
    /** Node role (Bootstrap, Group manager, Node). */
    private NodeRole role_;
    
    /** Id generator.*/
    private IdGeneration idGenerator_;
    
    /** Node network capacity. */
    private NetworkDemand networkCapacity_;
    
    /** Empty constructor. */
    public NodeSettings()
    {
        networkCapacity_ = new NetworkDemand();
    }
    
    /**
     * Sets the node role.
     * 
     * @param role  The node role
     */
    public void setRole(NodeRole role) 
    {
        role_ = role;
    }

    /**
     * Returns the node role.
     * 
     * @return  The node role
     */
    public NodeRole getRole() 
    {
        return role_;
    }
    
    /**
     * Sets the node network capacity.
     * 
     * @param networkCapacity     The node network capacity
     */
    public void setNetworkCapacity(NetworkDemand networkCapacity) 
    {
        networkCapacity_ = networkCapacity;
    }

    /**
     * Returns the node network capacity.
     * 
     * @return  The network capacity
     */
    public NetworkDemand getNetworkCapacity() 
    {
        return networkCapacity_;
    }

    /**
     * @return the idGenerator
     */
    public IdGeneration getIdGenerator()
    {
        return idGenerator_;
    }

    /**
     * @param idGenerator the idGenerator to set
     */
    public void setIdGenerator(IdGeneration idGenerator)
    {
        idGenerator_ = idGenerator;
    }
}
