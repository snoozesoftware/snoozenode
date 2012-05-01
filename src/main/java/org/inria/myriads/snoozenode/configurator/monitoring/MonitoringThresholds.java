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
package org.inria.myriads.snoozenode.configurator.monitoring;

import java.util.List;

/**
 * Monitoring thresholds.
 * 
 * @author Eugen Feller
 */
public final class MonitoringThresholds 
{
    /** The cpu utilization thresholds. */
    private List<Double> cpu_;

    /** The memory utilziation thresholds. */
    private List<Double> memory_;

    /** The network utilization thresholds. */
    private List<Double> network_;
        
    /**
     * Memory thresholds.
     *  
     * @param cpu      The CPU utilization thresholds
     * @param memory   The memory utilization thresholds
     * @param network  The network utilization thresholds
     */
    public MonitoringThresholds(List<Double> cpu, List<Double> memory, List<Double> network) 
    {
        cpu_ = cpu;
        memory_ = memory;
        network_ = network;
    }

    /**
     * Returns the CPU utilization thresholds.
     * 
     * @return  The CPU utilization thresholds
     */
    public List<Double> getCPU() 
    {
        return cpu_;
    }

    /**
     * Returns the memory utilization thresholds.
     * 
     * @return  The memory utilization thresholds
     */
    public List<Double> getMemory() 
    {
        return memory_;
    }

    /**
     * Returns the network utilization thresholds.
     * 
     * @return      The network utilization thresholds
     */
    public List<Double> getNetwork() 
    {
        return network_;
    }
}
