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
package org.inria.myriads.snoozenode.localcontroller.monitoring.information;

import java.util.List;

import org.inria.myriads.snoozecommon.guard.Guard;

/**
 * Virtual machine monitoring information.
 * 
 * @author Eugen Feller
 */
public class VirtualMachineInformation 
{
    /** Cpu time in nanoseconds. */
    private long cpuTime_;
    
    /** Memory usage (Bytes) .*/
    private long memoryUsage_;
    
    /** Network traffic information. */
    private List<NetworkTrafficInformation> networkTraffic_;
    
    /** Number of virtual cpus. */
    private int numberOfVirtualCpus_;

    /**
     * Constructor.
     * 
     * @param numberOfVirtualCpus   The number of virtual CPUs
     * @param cpuTime               The CPU time
     * @param memoryUsage           The memory usage
     * @param networkTraffic        The network traffic
     */
    public VirtualMachineInformation(int numberOfVirtualCpus,
                                     long cpuTime,
                                     long memoryUsage, 
                                     List<NetworkTrafficInformation> networkTraffic)
    {
        Guard.check(numberOfVirtualCpus, cpuTime, memoryUsage, networkTraffic);
        
        numberOfVirtualCpus_ = numberOfVirtualCpus;
        cpuTime_ = cpuTime;
        memoryUsage_ = memoryUsage;
        networkTraffic_ = networkTraffic;
    }
    
    
    
    /**
     * 
     */
    public VirtualMachineInformation()
    {
        super();
    }



    /**
     * Returns the CPU time.
     * 
     * @return  The CPU time
     */
    public long getCpuTime() 
    {
        return cpuTime_;
    }

    /**
     * Returns the memory usage.
     * 
     * @return  The memory usage
     */
    public long getMemoryUsage() 
    {
        return memoryUsage_;
    }

    /**
     * Returns the number of virtual CPUs.
     * 
     * @return  Then number of virtual CPUs
     */
    public int getNumberOfVirtualCpus() 
    {
        return numberOfVirtualCpus_;
    }

    /**
     * Returns the network traffic information.
     * 
     * @return  The network traffic information
     */
    public List<NetworkTrafficInformation> getNetworkTraffic() 
    {
        return networkTraffic_;
    }



    /**
     * @param cpuTime the cpuTime to set
     */
    public void setCpuTime(long cpuTime)
    {
        cpuTime_ = cpuTime;
    }



    /**
     * @param memoryUsage the memoryUsage to set
     */
    public void setMemoryUsage(long memoryUsage)
    {
        memoryUsage_ = memoryUsage;
    }



    /**
     * @param networkTraffic the networkTraffic to set
     */
    public void setNetworkTraffic(List<NetworkTrafficInformation> networkTraffic)
    {
        networkTraffic_ = networkTraffic;
    }



    /**
     * @param numberOfVirtualCpus the numberOfVirtualCpus to set
     */
    public void setNumberOfVirtualCpus(int numberOfVirtualCpus)
    {
        numberOfVirtualCpus_ = numberOfVirtualCpus;
    }
}
