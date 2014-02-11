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
package org.inria.myriads.snoozenode.database;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.database.api.BootstrapRepository;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.database.api.LocalControllerRepository;

import org.inria.myriads.snoozenode.database.api.impl.cassandra.BootstrapCassandraRepository;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.GroupLeaderCassandraRepository;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.GroupManagerCassandraRepository;
import org.inria.myriads.snoozenode.database.api.impl.memory.BootstrapMemoryRepository;
import org.inria.myriads.snoozenode.database.api.impl.memory.GroupLeaderMemoryRepository;
import org.inria.myriads.snoozenode.database.api.impl.memory.GroupManagerMemoryRepository;
import org.inria.myriads.snoozenode.database.api.impl.memory.LocalControllerMemoryRepository;
import org.inria.myriads.snoozenode.database.api.wrapper.GroupLeaderWrapperRepository;
import org.inria.myriads.snoozenode.database.api.wrapper.GroupManagerWrapperRepository;
import org.inria.myriads.snoozenode.database.enums.DatabaseType;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.GroupLeaderPolicyFactory;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database factory.
 * 
 * @author Eugen Feller
 */
public final class DatabaseFactory 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupLeaderPolicyFactory.class);
    
    /** Hide constructor. */
    private DatabaseFactory()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Returns the group leader repository wrapper.
     * 
     * @param groupLeaderDescription    The group Leader description.
     * @param virtualMachineSubnets     The virtual machines subnets.
     * @param settings                  The database settings.
     * @param externalNotifier          The external notifier to use.
     * @return          The group leader repository.
     */
    public static GroupLeaderRepository newGroupLeaderRepository(
            GroupManagerDescription groupLeaderDescription, 
            String[] virtualMachineSubnets,   
            DatabaseSettings settings,
            ExternalNotifier externalNotifier) 
    {
        
        return new GroupLeaderWrapperRepository(
                groupLeaderDescription, 
                virtualMachineSubnets, 
                settings, 
                externalNotifier);
    }
    
    
    /**
     * Returns the group leader repository wrapper.
     * 
     * @param groupLeaderDescription  The Group leader Description. 
     * @param virtualMachineSubnets   The virtual machine subnets
     * @param databaseSettings        The database settings.
     * @return                        The group leader repository
     */
    public static GroupLeaderRepository newGroupLeaderRepository(
            GroupManagerDescription groupLeaderDescription, 
            String[] virtualMachineSubnets,   
            DatabaseSettings databaseSettings)
    {
        
        GroupLeaderRepository repository = null;
        DatabaseType type = databaseSettings.getType();
        switch (type) 
        {
            case memory :       
                repository = new GroupLeaderMemoryRepository(
                        groupLeaderDescription, 
                        virtualMachineSubnets, 
                        databaseSettings.getNumberOfEntriesPerGroupManager());
                break;
                
            case cassandra : 
                String hosts = databaseSettings.getCassandraSettings().getHosts();
                repository = new GroupLeaderCassandraRepository(
                        groupLeaderDescription, 
                        virtualMachineSubnets, 
                        databaseSettings.getNumberOfEntriesPerGroupManager(),
                        databaseSettings.getNumberOfEntriesPerVirtualMachine(),
                        hosts);
                break;
            default:
                log_.error("Unknown group leader database type selected");
        }
        
        return repository;
    }

    
    /**
     * 
     * Return the groupmanager wrapper repository.
     * 
     * @param groupManager              the group manager description.
     * @param maxCapacity               the max capacity
     * @param interval                  the interval of monitoring 
     * @param settings                  the database settings
     * @param externalNotifierSettings  the external notifier settings
     * @param externalNotifier          the external notifier
     * @return  the group manager wrapper repository.
     */
    public static GroupManagerRepository newGroupManagerRepository(
            GroupManagerDescription groupManager,
            int maxCapacity,
            int interval,
            DatabaseSettings settings,
            ExternalNotifierSettings externalNotifierSettings,
            ExternalNotifier externalNotifier
                                                                    ) 
    {
        return new GroupManagerWrapperRepository(
                groupManager,
                maxCapacity,
                interval,
                settings,
                externalNotifierSettings,
                externalNotifier);
    }
    
    /**
     * 
     * Returns the group manager repository.
     * 
     * @param groupManager      The group manager description
     * @param interval          The monitoring interval
     * @param maxCapacity       The max Capacity
     * @param settings          The database settings.
     * @return             The group manager repository.
     */
    public static GroupManagerRepository newGroupManagerRepository(
            GroupManagerDescription groupManager, 
            int interval,
            int maxCapacity,
            DatabaseSettings settings)
    {
        GroupManagerRepository repository = null;
        DatabaseType type = settings.getType();
        switch (type) 
        {
            case memory :       
                repository = new GroupManagerMemoryRepository(groupManager, maxCapacity);
                break;
            case cassandra:
                String hosts = settings.getCassandraSettings().getHosts();
                repository = new GroupManagerCassandraRepository(
                        groupManager,
                        settings.getNumberOfEntriesPerGroupManager(),
                        settings.getNumberOfEntriesPerVirtualMachine(),
                        hosts);
                break;
            default:
                log_.error("Unknown group manager database type selected");
        }
        return repository;
    }

    /**
     * 
     * Returns the bootstrap repository (read only).
     * 
     * @param settings      database settings.
     * @return  The bootstrap repository.
     */
    public static BootstrapRepository newBootstrapRepository(DatabaseSettings settings)
    {
        BootstrapRepository repository = null;
        DatabaseType type = settings.getType();
        switch (type) 
        {
            case memory :       
                repository = new BootstrapMemoryRepository();
                break;
            case cassandra:
                String hosts = settings.getCassandraSettings().getHosts();
                repository = new BootstrapCassandraRepository(hosts);
                break;
            default:
                log_.error("Unknown bootstrap database type selected");
        }
        return repository;
    }
    
    /**
     * Returns the local controller repository.
     * 
     * @param type              The database type.
     * @param externalNotifier  The external notifier.
     * @return                  The local controller repository.
     */
    public static LocalControllerRepository newLocalControllerRepository(
            LocalControllerDescription localController,
            DatabaseType type, 
            ExternalNotifier externalNotifier)
    {
        LocalControllerRepository repository = null;
        switch (type) 
        {
            case memory :       
                repository = new LocalControllerMemoryRepository(localController, externalNotifier);
                break;
                   
            default:
                repository = new LocalControllerMemoryRepository(localController, externalNotifier);
                log_.error("Unknown local controller database type selected");
        }
        return repository;
    }
}
