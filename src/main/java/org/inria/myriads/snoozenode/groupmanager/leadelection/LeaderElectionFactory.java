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
package org.inria.myriads.snoozenode.groupmanager.leadelection;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozenode.configurator.faulttolerance.ZooKeeperSettings;
import org.inria.myriads.snoozenode.groupmanager.leadelection.api.LeaderElection;
import org.inria.myriads.snoozenode.groupmanager.leadelection.api.impl.ZooKeeperLeaderElection;
import org.inria.myriads.snoozenode.groupmanager.leadelection.listener.LeaderElectionListener;

/**
 * Leader election factory.
 * 
 * @author Eugen Feller
 */
public final class LeaderElectionFactory 
{
    /** Hide constructor. */
    private LeaderElectionFactory()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Creates the leader election logic.
     * 
     * @param zooKeeperParameters          The zookeeper parameters
     * @param groupManagerDescription      The group manager description
     * @param listener                     The leader election callback
     * @return                             The leader election object
     * @throws Exception 
     */
    public static LeaderElection newLeaderElection(ZooKeeperSettings zooKeeperParameters, 
                                                   GroupManagerDescription groupManagerDescription, 
                                                   LeaderElectionListener listener) throws Exception 
    {
        return new ZooKeeperLeaderElection(zooKeeperParameters, groupManagerDescription, listener);
    }
}
