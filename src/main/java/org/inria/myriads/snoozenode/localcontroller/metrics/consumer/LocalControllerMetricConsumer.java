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
package org.inria.myriads.snoozenode.localcontroller.metrics.consumer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.codehaus.jackson.map.ObjectMapper;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.datastructure.LRUCache;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.metric.Metric;
import org.inria.myriads.snoozenode.configurator.localcontrollermetrics.LocalControllerMetricsSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.MonitoringThresholds;
import org.inria.myriads.snoozenode.localcontroller.metrics.transport.AggregatedMetricData;
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
public final class LocalControllerMetricConsumer
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerMetricConsumer.class);
  
    /** Metric queue. */
    private BlockingQueue<AggregatedMetricData> metricQueue_;
    
    /** Global queue*/
    private BlockingQueue<LocalControllerDataTransporter> globalQueue_;
     
    /** Local controller identifier. */
    private String localControllerId_;
    
    /** Signals termination. */
    private boolean isTerminated_;

    /** number of metrics entries to collect. */
    private int numberOfMetricsEntries_;

    private ThresholdCrossingDetector crossingDetector_;
    /**
     * Constructor.
     * 
     * @param localController       The local controller description
     * @param monitoringThresholds 
     * @param dataQueue             The data queue
     * @param monitoringThresholds  The monitoring thresholds
     * @param callback              The monitoring service callback
     * @throws Exception            The exception
     */
    public LocalControllerMetricConsumer(LocalControllerDescription localController,
                                             BlockingQueue<AggregatedMetricData> metricQueue,
                                             BlockingQueue<LocalControllerDataTransporter> globalQueue,
                                             LocalControllerMetricsSettings localControllerMetricsSettings, MonitoringThresholds monitoringThresholds        
            ) 
        throws Exception
    {
        log_.debug("Initializing the local controller metric data consumer"); 
        localControllerId_ = localController.getId();
        metricQueue_ = metricQueue;
        globalQueue_ = globalQueue;
        numberOfMetricsEntries_ = localControllerMetricsSettings.getNumberOfMetricsEntries(); 
        crossingDetector_ = new ThresholdCrossingDetector(monitoringThresholds, localController.getTotalCapacity(), localControllerMetricsSettings);
    }
    
    
    /**
     * Sends regular data.
     * 
     * @param localControllerId     The local controller identifier
     * @param aggregatedData        The aggregated data
     * @throws IOException          The I/O exception
     */
    @SuppressWarnings("unchecked")
    private void sendRegularData(AggregatedMetricData metricData)
        throws IOException
    {
        Guard.check(metricData);
        
        AggregatedMetricData metricDataClone = (AggregatedMetricData) metricData.clone();
                
        
        LocalControllerDataTransporter localControllerData = 
            new LocalControllerDataTransporter(localControllerId_, null,metricDataClone);
        
        boolean isDetected = crossingDetector_.detectMetricThresholdCrossing(localControllerData);
        if (!isDetected)
        {
            log_.debug("No threshold crossing detected! Node seems stable for now!");
        }
    
        ObjectMapper mapper = new ObjectMapper();
        StringWriter s = new StringWriter();
        mapper.writeValue(s, localControllerData);
        log_.debug(s.toString());
        log_.debug("Sending aggregated metrics");
        globalQueue_.add(localControllerData);
    }
    
    /** Run method. */
    public void run() 
    {
        AggregatedMetricData metricData = new AggregatedMetricData();
        int numberOfMetrics = 0 ;
        try 
        {  
            while (!isTerminated_)
            {                          
                log_.debug("Waiting for metric data to arrive...");
                AggregatedMetricData currentData = metricQueue_.take(); 
                metricData.merge(currentData);
                numberOfMetrics ++ ;
                log_.debug(String.format("numberOfMetrics = %d / %d", numberOfMetrics, numberOfMetricsEntries_));
                if (numberOfMetrics == numberOfMetricsEntries_)
                {
                    sendRegularData(metricData);
                    metricData.clear();
                    numberOfMetrics = 0 ;
                }
            }   
        }
        catch (IOException exception) 
        {
            if (!isTerminated_)
            {
                log_.debug(String.format("I/O error during data sending (%s)! Did the group manager close " +
                                         "its connection unexpectedly?", exception.getMessage()));
                //close();
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
        //aclose();
    }
}
