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
package org.inria.myriads.snoozenode.util;

import java.util.List;

import org.inria.myriads.snoozecommon.globals.Globals;
import org.inria.myriads.snoozecommon.guard.Guard;

/**
 * Utilization utils.
 * 
 * @author Eugen Feller
 */
public final class UtilizationUtils 
{
    /**
     * Hide the consturctor.
     */
    private UtilizationUtils() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns CPU utilization information.
     * 
     * @param vector    The vector
     * @return          The utilization
     */
    public static double getCpuUtilization(List<Double> vector)
    {
        Guard.check(vector);
        return vector.get(Globals.CPU_UTILIZATION_INDEX);
    }
    
    /**
     * Returns memory utilization information.
     * 
     * @param vector    The vector
     * @return          The utilization
     */
    public static double getMemoryUtilization(List<Double> vector)
    {
        Guard.check(vector);
        return vector.get(Globals.MEMORY_UTILIZATION_INDEX);
    }
    
    /**
     * Returns network Rx utilization information.
     * 
     * @param vector    The vector
     * @return          The utilization
     */
    public static double getNetworkRxUtilization(List<Double> vector)
    {
        Guard.check(vector);
        return vector.get(Globals.NETWORK_RX_UTILIZATION_INDEX);
    }
    
    /**
     * Returns network Tx utilization information.
     * 
     * @param vector    The vector
     * @return          The utilization
     */
    public static double getNetworkTxUtilization(List<Double> vector)
    {
        Guard.check(vector);
        return vector.get(Globals.NETWORK_TX_UTILIZATION_INDEX);
    }
}
