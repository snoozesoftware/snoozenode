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

import java.util.HashMap;
import java.util.Map;


/**
 * Reconfiguration settings.
 * 
 * @author Eugen Feller
 */
public class ReconfigurationSettings 
{
    /** Selected policy. */
    private String policy_;
    
    /** Enabled or not. */
    private boolean isEnabled_;
    
    /** Interval. */
    private String interval_;
    
    /** Options. */
    private Map<String,String> options_;
    
    /**
     * Constructor. 
     */
    public ReconfigurationSettings()
    {
        options_ = new HashMap<String, String>();
    }
    
    /**
     * Sets the interval.
     * 
     * @param interval    The interval
     */
    public void setInterval(String interval) 
    {
        interval_ = interval;
    }

    /**
     * Returns the interval.
     * 
     * @return  The interval
     */
    public String getInterval() 
    {
        return interval_;
    }

    /**
     * Enables/disables reconfiguration.
     * 
     * @param isEnabled     true if enabled, false otherwise
     */
    public void setEnabled(boolean isEnabled) 
    {
        isEnabled_ = isEnabled;
    }

    /**
     * Returns the enabled flag.
     * 
     * @return  true if enabled, false otherwise
     */
    public boolean isEnabled() 
    {
        return isEnabled_;
    }

    /**
     * Sets the reconfiguration policy.
     * 
     * @param reconfigurationPolicy  The reconfiguration policy
     */
    public void setPolicy(String reconfigurationPolicy) 
    {
        policy_ = reconfigurationPolicy;
    }

    /**
     * Returns the reconfiguration policy.
     * 
     * @return The reconfiguration policy
     */
    public String getPolicy() 
    {
        return policy_;
    }

    /**
     * @return the options
     */
    public Map<String, String> getOptions()
    {
        return options_;
    }

   

    /**
     * @param options the options to set
     */
    public void setOptions(Map<String, String> options)
    {
        options_ = options;
    }
}
