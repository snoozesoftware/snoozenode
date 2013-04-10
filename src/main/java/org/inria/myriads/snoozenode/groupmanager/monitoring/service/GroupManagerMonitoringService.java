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
package org.inria.myriads.snoozenode.groupmanager.monitoring.service;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.external.MonitoringExternalSettings;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.monitoring.producer.GroupManagerSummaryProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager monitoring service.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerMonitoringService 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerMonitoringService.class);
    
    /** Monitoring data producer. */
    private GroupManagerSummaryProducer monitoringDataProducer_;

    /** Group manager repository. */
    private GroupManagerRepository repository_;

    /** Monitoring settings.*/
    private MonitoringSettings monitoringSettings_;
    
    /** Monitoring external.*/
    private MonitoringExternalSettings monitoringExternalSettings_;
    
    /**
     * Group manager monitoring service.
     * 
     * @param repository            The group manager repository
     * @param monitoringInterval    The monitoring interval
     * @throws Exception            The exception
     */
    public GroupManagerMonitoringService(GroupManagerRepository repository, 
                                         MonitoringSettings monitoringSettings,
                                         MonitoringExternalSettings monitoringExternalSettings)
        throws Exception
    {
        Guard.check(repository, monitoringSettings, monitoringExternalSettings);
        log_.debug("Initializing the group manager monitoring service");
        repository_ = repository;
        monitoringSettings_ = monitoringSettings;
        monitoringExternalSettings_ = monitoringExternalSettings;
    }
    
    /**
     * Starts the summary information producer.
     * 
     * @param groupLeader   The group leader monitoring address
     * @param estimator     The estimator
     * @throws Exception    The exception
     */
    public void startSummaryProducer(NetworkAddress groupLeader, ResourceDemandEstimator estimator)
        throws Exception
    {
        log_.debug("Starting the group manager monitoring data producer");   
        monitoringDataProducer_ = new GroupManagerSummaryProducer(repository_,
                                                                  groupLeader,
                                                                  estimator,
                                                                  monitoringSettings_,
                                                                  monitoringExternalSettings_
                                                                  );
        new Thread(monitoringDataProducer_).start();
    }
        
    /**
     * Terminates the monitoring service.
     */
    public void terminate()
    {
        log_.debug("Terminating the group manager monitoring service");        
        if (monitoringDataProducer_ != null)
        {
            monitoringDataProducer_.terminate();
        }
    }
}
