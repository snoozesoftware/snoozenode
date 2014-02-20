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

import java.util.HashMap;
import java.util.Map;

import org.inria.myriads.snoozenode.groupmanager.managerpolicies.sort.SortNorm;

/**
 * Estimator settings.
 * 
 * @author Eugen Feller
 */
public final class EstimatorSettings 
{
    
    /** Name.*/
    private String name_;
    
    /** Policies. */
//    private EstimatorPolicy policy_;
    
    /** Virtual machine demand measure. */
//    private SortNorm sortNorm_;

    /** Number of monitoring entries. */
    private int numberOfMonitoringEntries_;
    
    /** Static estimations. */
//    private boolean isStatic_;
    
    private Map<String, String> options_;
    
    /** Constructor. */
    public EstimatorSettings()
    {
//        policy_ = new EstimatorPolicy();
        options_ = new HashMap<String, String>();
    }
    
    /**
     * Sets the sort norm.
     * 
     * @param sortNorm  The sort norm
     */
//    public void setSortNorm(SortNorm sortNorm) 
//    {
//        sortNorm_ = sortNorm;
//    }

    /**
     * Returns the demand measure.
     * 
     * @return  The sort norm
     */
//    public SortNorm getSortNorm() 
//    {
//        return sortNorm_;
//    }

    /**
     * Sets the number of monitoring entries.
     * 
     * @param numberOfMonitoringEntries        The number of monitoring entries
     */
    public void setNumberOfMonitoringEntries(int numberOfMonitoringEntries) 
    {
        numberOfMonitoringEntries_ = numberOfMonitoringEntries;
    }
    
    /**
     * Returns the number of monitoring entries.
     * 
     * @return  The number of monitoring entries
     */
    public int getNumberOfMonitoringEntries()
    {
        return numberOfMonitoringEntries_;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name_;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        name_ = name;
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

    /**
     * Sets the used capacity flag.
     * 
     * @param isStatic  true if static estimations are desired, false otherwise
     */
//    public void setStatic(boolean isStatic) 
//    {
//        isStatic_ = isStatic;
//    }

    /**
     * Returns the static flag.
     * 
     * @return  true if static estimations are desired, false otherwise
     */
//    public boolean isStatic() 
//    {
//        return isStatic_;
//    }

    /**
     * Returns the policy.
     * 
     * @return  The policy
     */
//    public EstimatorPolicy getPolicy() 
//    {
//        return policy_;
//    }
}
