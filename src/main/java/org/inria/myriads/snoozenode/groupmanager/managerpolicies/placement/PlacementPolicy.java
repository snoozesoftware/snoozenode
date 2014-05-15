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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozenode.configurator.scheduler.GroupManagerSchedulerSettings;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;

/**
 * Placement policy interface.
 * 
 * @author Eugen Feller
 */
public abstract class PlacementPolicy 
{
    
    /** Resource demand estimator.*/
    protected ResourceDemandEstimator estimator_;
    
    /** Scheduler settings.*/
    protected GroupManagerSchedulerSettings schedulerSettings_;
    
    /** Initialize.*/
    public abstract void initialize();
    
    /**
     * Places the virtual machines.
     * 
     * @param virtualMachines       The virtual machines
     * @param localControllers      The local controllers
     * @return                      The placement plan
     */
    public abstract PlacementPlan place(List<VirtualMachineMetaData> virtualMachines, 
                        List<LocalControllerDescription> localControllers);

    /**
     * @return the estimator
     */
    public ResourceDemandEstimator getEstimator()
    {
        return estimator_;
    }

    /**
     * 
     * Sets the estimator.
     * 
     * @param estimator the estimator to set
     */
    public void setEstimator(ResourceDemandEstimator estimator)
    {
        estimator_ = estimator;
    }

    /**
     * 
     * Sets the scheduler Settings;
     * 
     * @param schedulerSettings     The scheduler settings.
     */
    public void setGroupManagerSettings(GroupManagerSchedulerSettings schedulerSettings)
    {
        schedulerSettings_ = schedulerSettings;
    }
    
   
}
