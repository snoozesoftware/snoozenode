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

import org.inria.myriads.snoozecommon.guard.Guard;

/**
 * Threshold utility.
 * 
 * @author Eugen Feller
 */
public final class ThresholdUtils
{
    /** Min threshold index. */
    private static final int MIN_THRESHOLD_INDEX = 0;
    
    /** Mid threshold index. */
    private static final int MID_THRESHOLD_INDEX = 1;
    
    /** Max threshold index. */
    private static final int MAX_THRESHOLD_INDEX = 2;
    
    /**
     * Hide the consturctor.
     */
    private ThresholdUtils() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns the min threshold.
     * 
     * @param thresholds    List of thresholds
     * @return              The mid threshold
     */
    public static double getMinThreshold(List<Double> thresholds)
    {
        Guard.check(thresholds);
        return thresholds.get(MIN_THRESHOLD_INDEX);
    }

    /**
     * Returns the mid threshold.
     * 
     * @param thresholds    List of thresholds
     * @return              The mid threshold
     */
    public static double getMidThreshold(List<Double> thresholds)
    {
        Guard.check(thresholds);
        return thresholds.get(MID_THRESHOLD_INDEX);
    }
    
    /**
     * Returns the max threshold.
     * 
     * @param thresholds    List of thresholds
     * @return              The max threshold
     */
    public static double getMaxThreshold(List<Double> thresholds)
    {
        Guard.check(thresholds);
        return thresholds.get(MAX_THRESHOLD_INDEX);
    }
}
