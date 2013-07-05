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
package org.inria.myriads.snoozenode.localcontroller.monitoring.consumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.datastructure.LRUCache;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringThresholds;
import org.inria.myriads.snoozenode.localcontroller.monitoring.listener.VirtualMachineMonitoringListener;
import org.inria.myriads.snoozenode.localcontroller.monitoring.threshold.ThresholdCrossingDetector;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.LocalControllerDataTransporter;
import org.inria.myriads.snoozenode.tcpip.TCPDataSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual machine monitor data consumer.
 * 
 * @author Eugen Feller
 */
public final class LocalControllerMonitoringConsumer extends TCPDataSender
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerMonitoringConsumer.class);
  
    /** Virtual machine monitoring callback. */
    private VirtualMachineMonitoringListener callback_;
    
    /** Thhreshold crossing detector. */
    //private ThresholdCrossingDetector crossingDetector_;
    
    /** Local controller identifier. */
    private String localControllerId_;
    
    /** Signals termination. */
    private boolean isTerminated_;

    private BlockingQueue<LocalControllerDataTransporter> globalQueue_;
    
    /**
     * Constructor.
     * 
     * @param localController       The local controller description
     * @param groupManagerAddress   The group manager address
     * @param dataQueue             The data queue
     * @param monitoringThresholds  The monitoring thresholds
     * @param callback              The monitoring service callback
     * @throws Exception            The exception
     */
    public LocalControllerMonitoringConsumer(LocalControllerDescription localController,
                                             NetworkAddress groupManagerAddress,
                                             BlockingQueue<LocalControllerDataTransporter> globalQueue,
                                             MonitoringThresholds monitoringThresholds,
                                             VirtualMachineMonitoringListener callback) 
        throws Exception
    {
        super(groupManagerAddress);
        log_.debug("Initializing the virtual machine monitoring data consumer"); 
        localControllerId_ = localController.getId();
        globalQueue_ = globalQueue;
        callback_ = callback; 
     //   crossingDetector_ = new ThresholdCrossingDetector(monitoringThresholds, localController.getTotalCapacity());
    }
    
    
       
    /**
     * Sends regular data.
     * 
     * @param localControllerId     The local controller identifier
     * @param aggregatedData        The aggregated data
     * @throws IOException          The I/O exception
     */
    @SuppressWarnings("unchecked")
    private void sendRegularData(LocalControllerDataTransporter localControllerData) 
        throws IOException
    {
        Guard.check(localControllerData);
        // here localControllerData can be hb, metrics, or vmdatas.
//        if (localControllerData.getData() != null)
//        {
//            //Threshold crossing detection on native metrics.
//            boolean isDetected = crossingDetector_.detectThresholdCrossing(localControllerData);
//            if (!isDetected)
//            {
//                log_.debug("No threshold crossing detected! Node seems stable for now!");
//            }
//        }
//        if (localControllerData.getMetricData() != null)
//        {
//            boolean isDetected = crossingDetector_.detectThresholdMetricCrossing(localControllerData);
//            
//            if (!isDetected)
//            {
//                log_.debug("No threshold crossing detected! Node seems stable for now!");
//            }
//        }
        log_.debug("Sending aggregated local controller summary information to group maanger");
        send(localControllerData); 
    }
    
    /** Run method. */
    public void run() 
    {
        ArrayList<AggregatedVirtualMachineData> aggregatedData = new ArrayList<AggregatedVirtualMachineData>();
        try 
        {  
            while (!isTerminated_)
            {                          
                log_.debug("Waiting for virtual machine monitoring data to arrive...");
                LocalControllerDataTransporter localControllerData = globalQueue_.take();   
                log_.debug(String.format("Getting %s data", 
                                         localControllerData.getLocalControllerId()));

                sendRegularData(localControllerData);
            }   
        }
        catch (IOException exception) 
        {
            if (!isTerminated_)
            {
                log_.debug(String.format("I/O error during data sending (%s)! Did the group manager close " +
                                         "its connection unexpectedly?", exception.getMessage()));
                close();
            }
        } 
        catch (InterruptedException exception)
        {
            log_.error("Virtual machine monitoring data consumer thread was interruped", exception);
        }  
        
        log_.debug("Virtual machine monitoring data consumer stopped!");
    }
    

    /**
     * Terminates the consumer.
     */
    public void terminate()
    {
        log_.debug("Terminating the virtual machine monitoring data consumer");
        isTerminated_ = true;
        close();
    }
}
