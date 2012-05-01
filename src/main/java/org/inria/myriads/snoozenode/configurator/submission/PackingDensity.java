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
package org.inria.myriads.snoozenode.configurator.submission;

/**
 * Packing density.
 * 
 * @author Eugen Feller
 */
public final class PackingDensity 
{
    /** CPU. */
    private double cpu_;
    
    /** Memory. */
    private double memory_;
    
    /** Network. */
    private double network_;
        
    /**
     * Sets the CPU density.
     * 
     * @param cpu   The CPU density
     */
    public void setCPU(double cpu)
    {
        cpu_ = cpu;
    }
    
    /**
     * Returns the CPU packing density.
     * 
     * @return  The CPU packing densisty
     */
    public double getCPU() 
    {
        return cpu_;
    }
    
    /**
     * Sets the memory density.
     * 
     * @param memory    The memory density
     */
    public void setMemory(double memory)
    {
        memory_ = memory;
    }
        
    /**
     * Returns the memory packing densisty.
     * 
     * @return  The memory packing density
     */
    public double getMemory() 
    {
        return memory_;
    }
    
    /**
     * Sets the network density.
     * 
     * @param network  The network density
     */
    public void setNetwork(double network)
    {
        network_ = network;
    }
    
    /**
     * Returns the network packing densisty.
     * 
     * @return  The network packing densisty
     */
    public double getNetwork() 
    {
        return network_;
    }
}
