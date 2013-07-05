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

import org.inria.myriads.snoozenode.groupmanager.managerpolicies.enums.Relocation;

/**
 * Relocation settings.
 * 
 * @author Eugen Feller
 */
public class RelocationSettings 
{
    /** Overload relocation policy. */
    private Relocation overloadPolicy_;
    
    /** Underload relocation policy. */
    private Relocation underloadPolicy_;
    
    /**
     * Sets the overload relocation policy.
     * 
     * @param overloadPolicy  The overload relocation policy
     */
    public void setOverloadPolicy(Relocation overloadPolicy) 
    {
        overloadPolicy_ = overloadPolicy;
    }

    /**
     * Sets the underload relocation policy.
     * 
     * @param underloadPolicy  The underload relocation policy
     */
    public void setUnderloadPolicy(Relocation underloadPolicy) 
    {
        underloadPolicy_ = underloadPolicy;
    }
    
    /**
     * Returns the overload relocation policy.
     * 
     * @return      The overload relocation policy
     */
    public Relocation getOverloadPolicy() 
    {
        return overloadPolicy_;
    }

    /**
     * Returns the virtual machine underload relocation policy.
     * 
     * @return      The virtual machine underload relocation policy
     */
    public Relocation getUnderloadPolicy() 
    {
        return underloadPolicy_;
    }
}
