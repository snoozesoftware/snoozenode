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
package org.inria.myriads.snoozenode.groupmanager.monitoring;

import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.monitoring.consumer.LocalControllerSummaryConsumer;
import org.inria.myriads.snoozenode.groupmanager.monitoring.receiver.GroupManagerSummaryReceiver;
import org.inria.myriads.snoozenode.groupmanager.monitoring.receiver.LocalControllerSummaryReceiver;
import org.inria.myriads.snoozenode.groupmanager.monitoring.service.GroupManagerMonitoringService;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.LocalControllerDataTransporter;

/**
 * Monitoring factory.
 * 
 * @author Eugen Feller
 */
public final class MonitoringFactory 
{
    /**
     * Hide the consturctor.
     */
    private MonitoringFactory() 
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Creates a new group leader summary receiver.
     * 
     * @param networkAddress    The network address
     * @param timeOut           The time out
     * @param repository        The repository
     * @return                  The group leader monitoring data receiver
     * @throws Exception        Exception 
     */
    public static GroupManagerSummaryReceiver 
        newGroupManagerSummaryReceiver(NetworkAddress networkAddress, 
                                       int timeOut, 
                                       GroupLeaderRepository repository) 
        throws Exception
    {
        return new GroupManagerSummaryReceiver(networkAddress, timeOut, repository);
    }
    
    /**
     * Creates a new local controller monitoring data receiver.
     * 
     * @param networkAddress     The network address
     * @param timeOut            The timeout
     * @param dataQueue          The data queue
     * @param stateMachine       The state machine
     * @param repository         The group manager repository
     * @return                   The summary data receiver
     * @throws Exception        ExceptionÈ
     */
    public static LocalControllerSummaryReceiver
        newLocalControllerSummaryReceiver(NetworkAddress networkAddress, 
                                          int timeOut, 
                                          BlockingQueue<LocalControllerDataTransporter> dataQueue,
                                          StateMachine stateMachine,
                                          GroupManagerRepository repository) 
        throws Exception
    {
        return new LocalControllerSummaryReceiver(networkAddress, timeOut, dataQueue, stateMachine, repository);     
    }
    
    /**
     * Creates a new local controller summary information consumer.
     * 
     * @param dataQueue            The data queue
     * @param stateMachine         The state machine
     * @param repository           The repository
     * @return                     The local controller monitoring data consumer
     * @throws Exception           Exception 
     */
    public static LocalControllerSummaryConsumer 
        newLocalControllerSummaryConsumer(BlockingQueue<LocalControllerDataTransporter> dataQueue,
                                          StateMachine stateMachine,
                                          GroupManagerRepository repository)
        throws Exception
    {
        return new LocalControllerSummaryConsumer(dataQueue, stateMachine, repository);     
    }
    
    /**
     * Creates a new group manager monitoring service.
     * 
     * @param groupManagerId                The group manager id.
     * @param repository                    The group manager repository.
     * @param estimator                     The resource demand estimator.
     * @param databaseSettings              The database settings.
     * @param monitoringSettings            The monitoring settings.
     * @param monitoringExternalSettings    The monitoringExternalSettings.
     * @return                              The group manager monitoring data sender.
     * @throws Exception              The exception
     */
    public static GroupManagerMonitoringService 
        newGroupManagerMonitoringService(
                                         String groupManagerId,
                                         GroupManagerRepository repository,
                                         ResourceDemandEstimator estimator,
                                         DatabaseSettings databaseSettings,
                                         MonitoringSettings monitoringSettings,
                                         ExternalNotifierSettings monitoringExternalSettings)
        throws Exception 
    {
        return new GroupManagerMonitoringService(
                groupManagerId, 
                repository, 
                estimator, 
                databaseSettings,
                monitoringSettings, 
                monitoringExternalSettings);
    }
}
