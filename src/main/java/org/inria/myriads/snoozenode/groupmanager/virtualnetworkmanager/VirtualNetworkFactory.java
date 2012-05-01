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
package org.inria.myriads.snoozenode.groupmanager.virtualnetworkmanager;

import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.groupmanager.virtualnetworkmanager.api.VirtualNetworkManager;
import org.inria.myriads.snoozenode.groupmanager.virtualnetworkmanager.api.impl.HostVirtualNetworkManager;

/**
 * Virtual network factory.
 * 
 * @author Eugen Feller
 */
public final class VirtualNetworkFactory 
{
    /** Hide constructor. */
    private VirtualNetworkFactory()
    {   
        throw new UnsupportedOperationException();
    }
    
    /**
     * Creates a new virtual network manager.
     * 
     * @param repository    The group leader repository
     * @return              The virtual network manager
     */
    public static VirtualNetworkManager newVirtualNetworkManager(GroupLeaderRepository repository)
    {
        return new HostVirtualNetworkManager(repository);
    }
}
