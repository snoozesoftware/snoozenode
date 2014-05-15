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
package org.inria.myriads.snoozenode.configurator.scheduler;

import java.util.Map;


/**
 * Group manager scheduler settings.
 * 
 * @author Eugen Feller
 */
public class GroupManagerSchedulerSettings 
{
    /** Placement policy. */
    private String placementPolicy_;
    
    /** Relocation parameters. */
    private RelocationSettings relocation_;
    
    /** Reconfiguration settings. */
    private ReconfigurationSettings reconfiguration_;

    /** Options.*/
    private Map<String, String> options_;
    
   
    /** Empty constructor. */
    public GroupManagerSchedulerSettings()
    {
        relocation_ = new RelocationSettings();
        reconfiguration_ = new ReconfigurationSettings();
    }
    
    /**
     * Returns the relocation settings.
     * 
     * @return  The relocation settings
     */
    public RelocationSettings getRelocationSettings()
    {
        return relocation_;
    }
    
    /**
     * Returns the reconfiguration settings.
     * 
     * @return  The reconfiguration settings
     */
    public ReconfigurationSettings getReconfigurationSettings()
    {
        return reconfiguration_;
    }
    
    /**
     * Sets the placement policy.
     * 
     * @param placementPolicy    The placement policy
     */
    public void setPlacementPolicy(String placementPolicy) 
    {
        placementPolicy_ = placementPolicy;
    }

    /**
     * Returns the placement policy.
     * 
     * @return  The placement policy
     */
    public String getPlacementPolicy() 
    {
        return placementPolicy_;
    }
    
    /**
     * 
     * Gets the options.
     * 
     * @return
     */
    public Map<String, String> getOptions()
    {
        return options_;
    }

    /**
     * 
     * Sets the options.
     * 
     * @param options
     */
    public void setOptions(Map<String, String> options)
    {
        options_ = options;
    }

    
}
