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
package org.inria.myriads.snoozenode.configurator.estimator;

import org.inria.myriads.snoozenode.groupmanager.estimator.enums.Estimator;

/**
 * Estimator policies.
 * 
 * @author Eugen Feller
 */
public class EstimatorPolicy 
{
    /** Virtual machine CPU demand estimators. */
    private Estimator cpu_;
    
    /** Virtual machine memory demand estimator. */
    private Estimator memory_;
    
    /** Virtual machine network demand estimator. */
    private Estimator network_;
    
    /**
     * Sets the CPU demand estimator.
     * 
     * @param cpu  The cpu demand estimator
     */
    public void setCPU(Estimator cpu) 
    {
        cpu_ = cpu;
    }

    /**
     * Returns the CPU demand estimator.
     * 
     * @return  The virtual machine CPU demand estimator
     */
    public Estimator getCPU() 
    {
        return cpu_;
    }

    /**
     * Sets the memory demand estimator.
     * 
     * @param memory     The memory demand estimator
     */
    public void setMemory(Estimator memory) 
    {
        memory_ = memory;
    }

    /**
     * Returns the memory demand estimator.
     * 
     * @return  The memory demand estimator
     */
    public Estimator getMemory() 
    {
        return memory_;
    }

    /**
     * Sets the network demand estimator.
     * 
     * @param network  The network demand estimator
     */
    public void setNetwork(Estimator network) 
    {
        network_ = network;
    }

    /**
     * Returns the network demand estimator.
     * 
     * @return  The network demand estimator
     */
    public Estimator getNetwork() 
    {
        return network_;
    }
}
