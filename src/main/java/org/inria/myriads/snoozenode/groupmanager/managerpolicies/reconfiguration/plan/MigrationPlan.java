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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration.plan;

import java.util.Map;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;

/**
 * Migration plan.
 * 
 * @author Eugen Feller
 */
public final class MigrationPlan 
{        
    /** Number of used nodes. */
    private int numberOfUsedNodes_;
    
    /** Number of released nodes. */
    private int numberOfReleasedNodes_;
    
    /** New mapping of VMs to local controllers. */
    private Map<VirtualMachineMetaData, LocalControllerDescription> mapping_;
    
    /**
     * Constructor.
     * 
     * @param mapping                   The mapping
     * @param numberOfUsedNodes         The number of used nodes
     * @param numberOfReleasedNodes     The number of released nodes
     */
    public MigrationPlan(Map<VirtualMachineMetaData, LocalControllerDescription> mapping,  
                         int numberOfUsedNodes,
                         int numberOfReleasedNodes) 
    {
        mapping_ = mapping;
        numberOfUsedNodes_ = numberOfUsedNodes;
        numberOfReleasedNodes_ = numberOfReleasedNodes;
    }
    
    /**
     * Returns the number of used nodes.
     * 
     * @return  The number of used nodes
     */
    public int getNumberOfUsedNodes() 
    {
        return numberOfUsedNodes_;
    }
        
    /**
     * Returns the number of migrations.
     * 
     * @return  The number of migrations
     */
    public int getNumberOfMigrations() 
    {
        return mapping_.size();
    }
    
    /**
     * Returns the number of released nodes.
     * 
     * @return  The number of released nodes
     */
    public int getNumberOfReleasedNodes() 
    {
        return numberOfReleasedNodes_;
    }

    /**
     * Returns the mapping.
     * 
     * @return  The mapping
     */
    public Map<VirtualMachineMetaData, LocalControllerDescription> getMapping() 
    {
        return mapping_;
    }
}
