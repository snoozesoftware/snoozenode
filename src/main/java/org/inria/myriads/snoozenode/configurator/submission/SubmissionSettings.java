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
 * Submission settings.
 * 
 * @author Eugen Feller
 */
public class SubmissionSettings 
{
    /** Start dispatching. */
    private PollingSettings dispatching_;
    
    /** Collect submission responses. */
    private PollingSettings collection_;
    
    /** Packing density. */
    private PackingDensity packingDensity_;
    
    /** Constructor. */
    public SubmissionSettings()
    {
        dispatching_ = new PollingSettings();
        collection_ = new PollingSettings();
        packingDensity_ = new PackingDensity();
    }
    
    /**
     * Sets the packing density.
     * 
     * @param packingDensity    The packing density
     */
    public void setPackingDensisty(PackingDensity packingDensity) 
    {
        packingDensity_ = packingDensity;
    }
    
    /**
     * Returns the packing density.
     * 
     * @return      The packing density
     */
    public PackingDensity getPackingDensity()
    {
        return packingDensity_;
    }

    /**
     * Returns the dispatching polling parameters.
     * 
     * @return  The dispatching polling parameters
     */
    public PollingSettings getDispatching() 
    {
        return dispatching_;
    }

    /**
     * Returns the collection polling parameters.
     * 
     * @return  The collection polling parameters
     */
    public PollingSettings getCollection() 
    {
        return collection_;
    }
}
