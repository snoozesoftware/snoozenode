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
package org.inria.myriads.snoozenode.localcontroller.monitoring.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitorSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.HostMonitoringSettings;
import org.inria.myriads.snoozenode.database.api.LocalControllerRepository;
import org.inria.myriads.snoozenode.exception.HostMonitoringException;
import org.inria.myriads.snoozenode.localcontroller.monitoring.consumer.HostMonitorDataConsumer;
import org.inria.myriads.snoozenode.localcontroller.monitoring.listener.HostMonitoringListener;
import org.inria.myriads.snoozenode.localcontroller.monitoring.producer.HostHeartbeatDataProducer;
import org.inria.myriads.snoozenode.localcontroller.monitoring.producer.HostMonitorDataProducer;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedHostMonitoringData;
import org.inria.myriads.snoozenode.monitoring.comunicator.api.MonitoringCommunicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual machine monitoring service.
 * 
 * @author Eugen Feller
 */
public final class HostMonitoringService 
    implements HostMonitoringListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(HostMonitoringService.class);
    
    /** Keeps track of the monitoring threads. */
    private Map<String, HostMonitorDataProducer> producerThreads_;
    
    /** Data queue. */
    private BlockingQueue<AggregatedHostMonitoringData> dataQueue_;
    
    /** Virtual machine heartbeat producer. */
    private HostHeartbeatDataProducer heartbeatProducer_;
    
    /** Virtual machine monitor consumer. */
    private HostMonitorDataConsumer monitorDataConsumer_;
    
    /** Local controller repository. */
    private LocalControllerRepository repository_;
    
    /** Resource monitoring. */
    private InfrastructureMonitoring monitoring_;
    
    /** Local controller description. */
    private LocalControllerDescription localController_;
    
    /** Database Settings. */
    private NodeConfiguration nodeConfiguration_;

    /**
     * Constructor.
     * 
     * @param localController       The local controller description
     * @param repository            The local controller repository
     * @param monitoring            The infrastructure monitoring
     * @param nodeConfiguration      The database settings
     */
    public HostMonitoringService(LocalControllerDescription localController,
                                           LocalControllerRepository repository,
                                           InfrastructureMonitoring monitoring,
                                           NodeConfiguration nodeConfiguration
                                  )
    {
        Guard.check(localController, repository, monitoring);
        log_.debug("Initializing host monitoring service");
        
        localController_ = localController;
        repository_ = repository;
        monitoring_ = monitoring;
        nodeConfiguration_ = nodeConfiguration;
        nodeConfiguration.getHostMonitoringSettings();
        dataQueue_ = new LinkedBlockingQueue<AggregatedHostMonitoringData>();
        producerThreads_ = Collections.synchronizedMap(new HashMap<String, HostMonitorDataProducer>());
    }

    /**
     * 
     * Starts the host monitor service.
     * 
     * @param communicator  The communicator.
     * @throws Exception    The exception
     */
    public synchronized void startService(MonitoringCommunicator communicator) 
        throws Exception
    {
        log_.debug("Starting the host monitoring service");
        startHostMonitorDataConsumer(communicator);
        startHeartbeatProducer();
        startHostMonitorDataProducer();
    }

    /**
     * Starts the producers.
     */
    private void startHostMonitorDataProducer() 
    {
        log_.debug("Starting the host monitoring data consumer");
        HostMonitoringSettings hostMonitoringSettings = nodeConfiguration_.getHostMonitoringSettings();
        for (Entry<String, HostMonitorSettings> monitor : hostMonitoringSettings.getHostMonitorSettings().entrySet())
        {
            HostMonitorDataProducer producer = producerThreads_.get(monitor.getKey());
            if (producer == null)
            {
                String producerId = UUID.randomUUID().toString();
                HostMonitorDataProducer dataProducer;
                try
                {
                    dataProducer = new HostMonitorDataProducer(
                            producerId,
                            dataQueue_,
                            monitor.getValue(), 
                            localController_,
                            this);
                
                new Thread(dataProducer, "HostMonitorDataProducer-" + monitor.getValue()).start();
                producerThreads_.put(monitor.getKey(), dataProducer);
                }
                catch (HostMonitoringException e)
                {
                    log_.error("Unable to start the producer, removing it");
                }
            }
            else
            {
                log_.debug("Producer already exist !");
            }
            
        }
    }

    /**
     * @param communicator  The communicator.
     * @throws Exception    The exception.
     */
    private synchronized void startHostMonitorDataConsumer(MonitoringCommunicator communicator)
        throws Exception
    {
        Guard.check(communicator);
        log_.debug("Starting the host monitoring data consumer");
      
        
        monitorDataConsumer_ = new HostMonitorDataConsumer(localController_,
                                                            repository_,
                                                             communicator, 
                                                             dataQueue_,
                                                             monitoring_,
                                                             nodeConfiguration_,
                                                             this);
        new Thread(monitorDataConsumer_, "HostMonitorDataConsumer").start(); 
    }

    /**
     * Starts the heartbeat producer.
     */
    private synchronized void startHeartbeatProducer()
    {
        log_.debug("Starting the host heartbeat producer");
        heartbeatProducer_ = 
            new HostHeartbeatDataProducer(localController_.getId(), 
                                                    monitoring_.getMonitoringSettings().getInterval(), 
                                                    dataQueue_);
        new Thread(heartbeatProducer_, "HostHeartbeatDataProducer").start();
    }

    
    /**
     * Suspend the monitoring of a virtual machine.
     * 
     * @param monitorId     The virtual machine identifier
     * @return                     true if everything ok, false otherwise
     */
    public synchronized boolean suspend(String monitorId)
    {
        Guard.check(monitorId);
        
        HostMonitorDataProducer dataProducer = producerThreads_.get(monitorId);
        if (dataProducer == null)
        {
            log_.error("No monitoring loop exists for this virtual machine");
            return false;
        }

        log_.debug(String.format("Suspending monitor %s monitoring", monitorId));
        dataProducer.setSuspend();  
        return true;
    }

    /**
     * Wakes up monitoring of a virtual machine.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      true if everyhting ok, false otherwise
     */
    public synchronized boolean resume(String virtualMachineId)
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Resuming virtual machine %s monitoring", virtualMachineId));
        
        HostMonitorDataProducer dataProducer = producerThreads_.get(virtualMachineId);
        if (dataProducer == null)
        {
            log_.error("No monitoring loop exists for this virtual machine");
            return false;
        }
        
        log_.debug("Calling wakeup on the producer!");
        dataProducer.wakeup();
        return true; 
    }
    
    /**
     * Stops monitoring of a monitor.
     * 
     * @param monitorId            The monitor identifier identifier
     * @return                     true if everyhting ok, false otherwise
     */
    public synchronized boolean stop(String monitorId)
    {
        Guard.check(monitorId);
        log_.debug(String.format("Stopping monitor %s monitoring", monitorId));
       
        HostMonitorDataProducer monitorProducer = producerThreads_.get(monitorId);
        if (monitorProducer == null)
        {
            log_.debug("No monitoring loop exists for this monitorId");
            return false;
        }
        
        monitorProducer.terminate();
        producerThreads_.remove(monitorId);
        return true;
    }

    /**
     * Stops the service.
     * 
     * @throws InterruptedException 
     */
    public void stopService() 
        throws InterruptedException
    {
        log_.debug("Stopping the host monitoring service");
        
        if (heartbeatProducer_ != null)
        {
            log_.debug("Terminating the heartbeat data producer");
            heartbeatProducer_.terminate();
        }
        
        if (monitorDataConsumer_ != null)
        {
            log_.debug("Terminating the monitoring data consumer");
            monitorDataConsumer_.terminate();
        }
        
        
    }
    
    /**
     * Returns the amount of active virtual machines.
     * 
     * @return     The amount of active VMs
     */
    @Override
    public int getNumberOfActiveMonitor()
    {
        return producerThreads_.size();
    }
    

    @Override
    public synchronized boolean onMonitoringFailure(String monitorId) 
    {
        Guard.check(monitorId);
        log_.debug("Stopping monitoring service for " + monitorId);
        return true;
    }


}
