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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.configurator.localcontrollermetrics.LocalControllerMetricsSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringThresholds;
import org.inria.myriads.snoozenode.database.api.LocalControllerRepository;
import org.inria.myriads.snoozenode.localcontroller.metrics.consumer.LocalControllerMetricConsumer;
import org.inria.myriads.snoozenode.localcontroller.metrics.producer.LocalControllerMetricProducer;
import org.inria.myriads.snoozenode.localcontroller.metrics.transport.AggregatedMetricData;
import org.inria.myriads.snoozenode.localcontroller.monitoring.consumer.LocalControllerMonitoringConsumer;
import org.inria.myriads.snoozenode.localcontroller.monitoring.consumer.VirtualMachineMonitorDataConsumer;
import org.inria.myriads.snoozenode.localcontroller.monitoring.listener.VirtualMachineMonitoringListener;
import org.inria.myriads.snoozenode.localcontroller.monitoring.producer.VirtualMachineHeartbeatDataProducer;
import org.inria.myriads.snoozenode.localcontroller.monitoring.producer.VirtualMachineMonitorDataProducer;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.LocalControllerDataTransporter;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual machine monitoring service.
 * 
 * @author Eugen Feller
 */
public final class VirtualMachineMonitoringService 
    implements VirtualMachineMonitoringListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(VirtualMachineMonitoringService.class);
    
    /** Keeps track of the monitoring threads. */
    private Map<String, VirtualMachineMonitorDataProducer> producerThreads_;
    
    /** Data queue. */
    private BlockingQueue<AggregatedVirtualMachineData> dataQueue_;
    
    /** Metric queue. */
    private BlockingQueue<AggregatedMetricData> metricQueue_;
    
    /** Virtual machine heartbeat producer. */
    private VirtualMachineHeartbeatDataProducer heartbeatProducer_;
    
    /** Virtual machine monitor consumer. */
    private VirtualMachineMonitorDataConsumer monitorDataConsumer_;
    
    /** Local controller repository. */
    private LocalControllerRepository repository_;
    
    /** Resource monitoring. */
    private InfrastructureMonitoring monitoring_;
    
    /** Local controller description. */
    private LocalControllerDescription localController_;

    private LocalControllerMetricsSettings localControllerMetricsSettings_;

    private LinkedBlockingQueue<LocalControllerDataTransporter> globalQueue_;

    private LocalControllerMetricProducer metricsProducer_;

    private LocalControllerMetricConsumer metricConsumer_;
    
    /**
     * Constructor.
     * 
     * @param localController       The local controller description
     * @param repository            The local controller repository
     * @param monitoring            The infrastructure monitoring
     */
    public VirtualMachineMonitoringService(LocalControllerDescription localController,
                                           LocalControllerRepository repository,
                                           InfrastructureMonitoring monitoring,
                                           LocalControllerMetricsSettings metricSettings 
                                            )
    {
        Guard.check(localController, repository, monitoring);
        log_.debug("Initializing virtual machine monitoring service");
        
        localController_ = localController;
        repository_ = repository;
        monitoring_ = monitoring;
        dataQueue_ = new LinkedBlockingQueue<AggregatedVirtualMachineData>();
        metricQueue_ = new LinkedBlockingQueue<AggregatedMetricData>();
        globalQueue_ = new LinkedBlockingQueue<LocalControllerDataTransporter>();
        localControllerMetricsSettings_ = metricSettings;
        producerThreads_ = Collections.synchronizedMap(new HashMap<String, VirtualMachineMonitorDataProducer>());
    }

    /**
     * Starts the virtual machine monitor service.
     * 
     * @param groupManagerAddress      The group manager address
     * @throws Exception               The exception
     */
    public synchronized void startService(NetworkAddress groupManagerAddress) 
        throws Exception
    {
        log_.debug("Starting the virtual machine monitoring service");
        Guard.check(groupManagerAddress);
        startVirtualMachineMonitorDataConsumer(groupManagerAddress);
        startMetricsService(localControllerMetricsSettings_);
        startHeartbeatProducer();
        startGlobalService(groupManagerAddress);
    }

    private void startGlobalService(NetworkAddress groupManagerAddress)
    {
        try
        {
            MonitoringThresholds thresholds = monitoring_.getMonitoringSettings().getThresholds();
            LocalControllerMonitoringConsumer globalConsumer = 
                    new LocalControllerMonitoringConsumer(localController_, groupManagerAddress, globalQueue_, thresholds, this);
            new Thread(globalConsumer).start();
            log_.debug("GLOBAL STARTED");
        }
        catch (Exception e)
        {
             e.printStackTrace();
        }
    }

    private void startMetricsService(
            LocalControllerMetricsSettings localControllerMetricsSettings) throws Exception
    {
        MonitoringThresholds thresholds = monitoring_.getMonitoringSettings().getThresholds();
        log_.debug("Starting the virtual machine heartbeat producer");
        metricsProducer_ = 
                new LocalControllerMetricProducer(localControllerMetricsSettings_,
                            metricQueue_);
        metricConsumer_ = 
                new LocalControllerMetricConsumer(localController_,
                        metricQueue_,
                        globalQueue_,
                        localControllerMetricsSettings,
                        thresholds
                        );
        new Thread(metricsProducer_).start();
        new Thread(metricConsumer_).start();
    }

    /**
     * Starts the virtual machine data consumer.
     * 
     * @param groupManagerAddress      The group manager address
     * @throws Exception               The exception
     */
    private synchronized void startVirtualMachineMonitorDataConsumer(NetworkAddress groupManagerAddress) 
        throws Exception
    {
        Guard.check(groupManagerAddress);
        log_.debug("Starting the virtual machine monitoring data consumer");
      
        MonitoringThresholds thresholds = monitoring_.getMonitoringSettings().getThresholds();
        monitorDataConsumer_ = new VirtualMachineMonitorDataConsumer(localController_,
                                                                     groupManagerAddress, 
                                                                     dataQueue_,
                                                                     globalQueue_,
                                                                     thresholds,
                                                                     localControllerMetricsSettings_,
                                                                     this                                                     
                );
        new Thread(monitorDataConsumer_).start(); 
    }

    /**
     * Starts the heartbeat producer.
     */
    private synchronized void startHeartbeatProducer()
    {
        log_.debug("Starting the virtual machine heartbeat producer");
        heartbeatProducer_ = 
            new VirtualMachineHeartbeatDataProducer(localController_.getId(), 
                                                    monitoring_.getMonitoringSettings().getInterval(), 
                                                    globalQueue_);
        new Thread(heartbeatProducer_).start();
    }

    /**
     * Start monitoring of a virtual machine.
     * 
     * @param virtualMachineMetaData   The virtual machine identifier
     * @return                         true if added, false otherwise
     */
    public synchronized boolean start(VirtualMachineMetaData virtualMachineMetaData) 
    {
        Guard.check(virtualMachineMetaData);
        log_.debug("Starting virtual machine monitoring");
        
        String virtualMachineId = virtualMachineMetaData.getVirtualMachineLocation().getVirtualMachineId();
        if (producerThreads_.containsKey(virtualMachineId))
        {
            log_.debug("This virtual machine is already beeing monitored!");
            return false;
        }
                   
        ManagementUtils.setVirtualMachineRunning(virtualMachineMetaData, localController_);
        boolean isAdded = repository_.addVirtualMachineMetaData(virtualMachineMetaData);
        if (!isAdded)
        {
            log_.debug("Failed to add virtual machine meta data");
            return isAdded;
        }
        
        log_.debug(String.format("Starting monitoring of virtual machine: %s", virtualMachineId));
        VirtualMachineMonitorDataProducer producer = 
            new VirtualMachineMonitorDataProducer(virtualMachineMetaData, 
                                                  monitoring_,
                                                  dataQueue_,
                                                  this);
        producer.start();
        producerThreads_.put(virtualMachineId, producer);
        return true;
    }

    /**
     * Halts the monitoring of a virtual machine.
     * 
     * @param virtualMachineId     The virtual machine identifier
     * @return                     true if everything ok, false otherwise
     */
    public synchronized boolean suspend(String virtualMachineId)
    {
        Guard.check(virtualMachineId);
        
        VirtualMachineMonitorDataProducer dataProducer = producerThreads_.get(virtualMachineId);
        if (dataProducer == null)
        {
            log_.error("No monitoring loop exists for this virtual machine");
            return false;
        }

        log_.debug(String.format("Suspending virtual machine %s monitoring", virtualMachineId));
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
        
        VirtualMachineMonitorDataProducer dataProducer = producerThreads_.get(virtualMachineId);
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
     * Stops monitoring of a virtual machine.
     * 
     * @param virtualMachineId     The virtual machine identifier
     * @return                     true if everyhting ok, false otherwise
     */
    public synchronized boolean stop(String virtualMachineId)
    {
        Guard.check(virtualMachineId);
        log_.debug(String.format("Stopping virtual machine %s monitoring", virtualMachineId));
       
        VirtualMachineMonitorDataProducer monitorProducer = producerThreads_.get(virtualMachineId);
        if (monitorProducer == null)
        {
            log_.debug("No monitoring loop exists for this virtual machine");
            return false;
        }
        
        monitorProducer.terminate();
        producerThreads_.remove(virtualMachineId);
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
        log_.debug("Stopping the virtual machine monitoring service");
        
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
     * Restarts the monitoring.
     * 
     * @param virtualMachineId      The virtual machine identifier
     * @return                      true if everything ok, false otherwise
     */
    public boolean restart(String virtualMachineId)
    {
        log_.debug(String.format("Restarting virtual machine %s monitoring", virtualMachineId));
        
        VirtualMachineMetaData metaData = repository_.getVirtualMachineMetaData().get(virtualMachineId);
        if (metaData == null)
        {
           log_.error("No virtual machine meta data exists!");
           return false;
        }
       
        VirtualMachineMonitorDataProducer producer = new VirtualMachineMonitorDataProducer(metaData, 
                                                                                           monitoring_, 
                                                                                           dataQueue_, 
                                                                                           this);
        producer.start();
        producerThreads_.put(metaData.getVirtualMachineLocation().getVirtualMachineId(), producer);
        return false;
    }
    
    /**
     * Returns the amount of active virtual machines.
     * 
     * @return     The amount of active VMs
     */
    @Override
    public int getNumberOfActiveVirtualMachines()
    {
        return producerThreads_.size();
    }
    
    /**
     * Drops virtual machine meta data.
     * 
     * @param location    The virtual machine location
     * @return            true if everything ok, false otherwise
     */
    @Override
    public synchronized boolean onMonitoringFailure(VirtualMachineLocation location) 
    {
        Guard.check(location);
        
        String virtualMachineId = location.getVirtualMachineId();   
        if (virtualMachineId == null)
        {
            log_.error("Virtual machine identifier is NULL");
            return false;
        }
        
        VirtualMachineMetaData metaData = repository_.getVirtualMachineMetaData(virtualMachineId);
        if (metaData == null)
        {
            log_.error("Virtual machine meta data is NULL!");
            return false;
        }
        
        log_.debug(String.format("Dropping virtual machine meta data for: %s", virtualMachineId));
        
        GroupManagerAPI comminucator = 
            CommunicatorFactory.newGroupManagerCommunicator(metaData.getGroupManagerControlDataAddress());
        boolean isDropped = comminucator.dropVirtualMachineMetaData(location);
        if (!isDropped)
        {
            log_.error("Failed to remove virtual machine meta data from remote group manager!");
            return false;
        }
        
        log_.debug("Virtual machine meta data removed from group manager!");
        isDropped = repository_.dropVirtualMachineMetaData(virtualMachineId);
        if (!isDropped)
        {
            log_.error("Failed to remove virtual machine meta data from local repository!");
            return false;
        }
        
        boolean isStopped = stop(virtualMachineId);
        if (!isStopped)
        {
            log_.error("Failed to stop virtual machine monitoring");
            return false;
        }
        
        return true;
    }


}
