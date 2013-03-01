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
package org.inria.myriads.snoozenode.configurator.database;

import org.inria.myriads.snoozenode.database.enums.DatabaseType;

/**
 * Database settings.
 * 
 * @author Eugen Feller
 */
public final class DatabaseSettings 
{
    /** Database type. */
    private DatabaseType type_;
    
    /** Maximum number of entries per group manager. */
    private int numberOfMonitoringEntriesPerGroupManager_;
    
    /** Maximum number of entries per virtual machine. */
    private int numberOfMonitoringEntriesPerVirtualMachine_;

    /**
     * Sets the number of monitoring entries per group manager.
     * 
     * @param numberOfMonitoringEntriesPerGroupManager    The number of monitoring entries per group manager
     */
    public void setNumberOfEntriesPerGroupManager(int numberOfMonitoringEntriesPerGroupManager) 
    {
        numberOfMonitoringEntriesPerGroupManager_ = numberOfMonitoringEntriesPerGroupManager;
    }

    /**
     * Returns the number of monitoring entries per group manager.
     * 
     * @return  The number of monitoring entries per group manager
     */
    public int getNumberOfEntriesPerGroupManager() 
    {
        return numberOfMonitoringEntriesPerGroupManager_;
    }

    /**
     * Sets the number of monitoring entries per virtual machine.
     * 
     * @param numberOfMonitoringEntriesPerVirtualMachine  The number of monitoring entries per virtual machine
     */
    public void setNumberOfEntriesPerVirtualMachine(int numberOfMonitoringEntriesPerVirtualMachine) 
    {
        numberOfMonitoringEntriesPerVirtualMachine_ = numberOfMonitoringEntriesPerVirtualMachine;
    }

    /**
     * Returns the number of monitoring entries per virtual machine.
     * 
     * @return  The number of virtual machine entries
     */
    public int getNumberOfEntriesPerVirtualMachine() 
    {
        return numberOfMonitoringEntriesPerVirtualMachine_;
    }

    /**
     * Sets the database type.
     * 
     * @param type  The database type
     */
    public void setType(DatabaseType type) 
    {
        type_ = type;
    }

    /**
     * Returns the database type.
     * 
     * @return  The database type
     */
    public DatabaseType getType() 
    {
        return type_;
    }
}
