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
package org.inria.myriads.snoozenode.groupmanager.managerpolicies.reconfiguration;

import java.util.List;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.configurator.scheduler.ReconfigurationSettings;
import org.inria.myriads.snoozenode.estimator.api.ResourceDemandEstimator;

/**
 * @author Eugen Feller, msimonin
 *
 */
public abstract class ReconfigurationPolicy 
{

    /** Resource demand estimator.*/
    protected ResourceDemandEstimator estimator_;
    
    /** Reconfiguration Settings.*/
    protected ReconfigurationSettings reconfigurationSettings_;
    
    /**
     * Initializes the reconfiguration policy. 
     */
    public abstract void initialize();
    
    /**
     * Reconfigures local controllers.
     * 
     * @param localControllerDescriptions     The local controller descriptions
     * @return                                The migration plan
     */
    public abstract ReconfigurationPlan reconfigure(List<LocalControllerDescription> localControllerDescriptions);

    /**
     * @return the estimator
     */
    public ResourceDemandEstimator getEstimator()
    {
        return estimator_;
    }

    /**
     * @param estimator the estimator to set
     */
    public void setEstimator(ResourceDemandEstimator estimator)
    {
        estimator_ = estimator;
    }

    /**
     * @return the reconfigurationSettings
     */
    public ReconfigurationSettings getReconfigurationSettings()
    {
        return reconfigurationSettings_;
    }

    /**
     * @param reconfigurationSettings the reconfigurationSettings to set
     */
    public void setReconfigurationSettings(ReconfigurationSettings reconfigurationSettings)
    {
        reconfigurationSettings_ = reconfigurationSettings;
    }
    
    
}
