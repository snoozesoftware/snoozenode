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
package org.inria.myriads.snoozenode.localcontroller.monitoring.producer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.NetworkDemand;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.MathUtils;
import org.inria.myriads.snoozenode.exception.HostMonitoringException;
import org.inria.myriads.snoozenode.exception.VirtualMachineMonitoringException;
import org.inria.myriads.snoozenode.localcontroller.monitoring.api.VirtualMachineMonitor;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.NetworkDirection;
import org.inria.myriads.snoozenode.localcontroller.monitoring.information.NetworkTrafficInformation;
import org.inria.myriads.snoozenode.localcontroller.monitoring.information.VirtualMachineInformation;
import org.inria.myriads.snoozenode.localcontroller.monitoring.listener.VirtualMachineMonitoringListener;
import org.inria.myriads.snoozenode.localcontroller.monitoring.service.InfrastructureMonitoring;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual machine monitoring data producer.
 * 
 * @author Eugen Feller
 */
public final class VirtualMachineMonitorDataProducer 
    extends Thread
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(VirtualMachineMonitorDataProducer.class);
          
    /** Position of the network interface. */
    private static final int NETWORK_INTERFACE_POSITION = 0;
    
    /** Monitoring callback. */
    private VirtualMachineMonitoringListener monitoringListener_;
      
    /** Infrastructure monitoring. */
    private InfrastructureMonitoring infrastructureMonitoring_;
    
    /** Data queue. */
    private BlockingQueue<AggregatedVirtualMachineData> dataQueue_;
    
    /** Montioring data. */
    private ArrayList<VirtualMachineMonitoringData> aggregatedData_;
    
    /** Virtual machine meta data. */
    private VirtualMachineMetaData virtualMachineMetaData_;
            
    /** Indicates if a measurement interval is first. */
    private boolean isFirst_;
    
    /** Used to terminate the thread. */
    private boolean isTerminated_;

    /** Used to suspend the thread. */
    private boolean isSuspended_;
    
    /** Timestamp before sleep. */
    private long beforeSleepTime_;

    /** Current system time. */
    private long currentSystemTime_;
    
    /** Time difference. */
    private long samplingTimeDifference_;

    /** The cpu time stamp. */
    private long cpuTimeStamp_;
    
    /** Network Rx traffic. */
    private double networkRxBytes_;

    /** Network Tx traffic. */
    private double networkTxBytes_;
    
    /** Lock object. */
    private Object lockObject_;
    
    /**
     * Constructor.
     * 
     * @param virtualMachineMetaData       The virtual machine meta data
     * @param infrastructureMonitoring     The infrastructure monitoring
     * @param dataQueue                    The data queue
     * @param monitoringListener           The virtual machine monitoring callback
     */
    public VirtualMachineMonitorDataProducer(VirtualMachineMetaData virtualMachineMetaData,
                                             InfrastructureMonitoring infrastructureMonitoring,
                                             BlockingQueue<AggregatedVirtualMachineData> dataQueue,
                                             VirtualMachineMonitoringListener monitoringListener) 
    {   
        super("VirtualMachineMonitorDataProducer :" + 
                    virtualMachineMetaData.getVirtualMachineLocation().getVirtualMachineId());
        Guard.check(virtualMachineMetaData, monitoringListener, dataQueue, monitoringListener);   
        log_.debug(String.format("Initializing virtual machine monitoring data producer for %s", 
                                 virtualMachineMetaData.getVirtualMachineLocation().getVirtualMachineId()));
        virtualMachineMetaData_ = virtualMachineMetaData;
        infrastructureMonitoring_ = infrastructureMonitoring;        
        dataQueue_ = dataQueue;
        monitoringListener_ = monitoringListener;
        lockObject_ = new Object();
        aggregatedData_ = new ArrayList<VirtualMachineMonitoringData>();
        isFirst_ = true;
    }
            
    /**
     * Creates aggregated virtual machine data.
     * 
     * @param monitoringData    The monitoring data
     * @return                  The aggregated virtual machine data
     */
    @SuppressWarnings("unchecked")
    private AggregatedVirtualMachineData createAggregatedVirtualMachineData(ArrayList<VirtualMachineMonitoringData> 
                                                                            monitoringData)
    {
        log_.debug("Creating aggregated virtual machine data object");
              
        ArrayList<VirtualMachineMonitoringData> clonedData =
            (ArrayList<VirtualMachineMonitoringData>) monitoringData.clone();
      
        String virtualMachineId = virtualMachineMetaData_.getVirtualMachineLocation().getVirtualMachineId();
        AggregatedVirtualMachineData aggregatedData = new AggregatedVirtualMachineData(virtualMachineId, 
                                                                                       clonedData);
        return aggregatedData;        
    }
    
    /**
     * The run routine.
     */
    public void run() 
    {
        VirtualMachineMonitor virtualMachineMonitor = infrastructureMonitoring_.getVirtualMachineMonitor();
        String virtualMachineId = virtualMachineMetaData_.getVirtualMachineLocation().getVirtualMachineId();
        int historySize = infrastructureMonitoring_.getMonitoringSettings().getNumberOfMonitoringEntries();
        int monitoringInterval =  infrastructureMonitoring_.getMonitoringSettings().getInterval();
        
        log_.debug(String.format("Starting virtual machine monitoring data producer for %s", virtualMachineId));
        try
        {
            while (true)
            {            
                
                if (isTerminated_)
                {
                    break;
                }
                         
                if (!isFirst_)
                {
                    currentSystemTime_ = System.nanoTime(); 
                    samplingTimeDifference_ = currentSystemTime_ - beforeSleepTime_; 
                }
                
                // this call allow us to know if the vm is still alive.
                VirtualMachineInformation virtualMachineInformation = 
                    virtualMachineMonitor.getVirtualMachineInformation(virtualMachineId);
                
                if (!isSuspended_)
                {
                    VirtualMachineMonitoringData monitoringData = 
                        createDynamicMonitoringData(virtualMachineInformation);
                    
                    log_.debug(String.format("Size of aggregated virtual machnine %s monitoring data is %d / %d",
                                             virtualMachineId, aggregatedData_.size(), historySize));                
                    if (aggregatedData_.size() == historySize)
                    {
                        log_.debug(String.format("Adding aggregated virtual machine %s monitoring data to the " +
                                                 "monitoring service queue", 
                                                 virtualMachineId));
                        
                        AggregatedVirtualMachineData data = createAggregatedVirtualMachineData(aggregatedData_);
                        dataQueue_.put(data);
                        aggregatedData_.clear();
                    } else
                    {
                        log_.debug(String.format("Adding virtual machine %s monitoring data: %s to " +
                                                 "the local monitoring data list", 
                                                 virtualMachineId,
                                                 monitoringData.getUsedCapacity()));
                        aggregatedData_.add(monitoringData);
                    }
                }
                
                beforeSleepTime_ = System.nanoTime();      
                doSleep(monitoringInterval);       
                setGlobalUtilization(virtualMachineInformation); 
                isFirst_ = false;
            }
        }
        catch (Exception exception) 
        {
            log_.debug(String.format("Failed to generate virtual machine monitoring data: %s", 
                                     exception.getMessage()));
            monitoringListener_.onMonitoringFailure(virtualMachineMetaData_.getVirtualMachineLocation());
        }
        
        log_.debug(String.format("Virtual machine: %s monitoring thread terminated!",  virtualMachineId));  
    }

    /**
     * Sets the global utilization variables.
     * Note that only the first network interface is taken into account!
     * 
     * @param virtualMachineInformation     The virtual machine information
     */
    private void setGlobalUtilization(VirtualMachineInformation virtualMachineInformation)
    {                   
        cpuTimeStamp_ = virtualMachineInformation.getCpuTime();   
        
        List<NetworkTrafficInformation> networkTraffic = virtualMachineInformation.getNetworkTraffic();
        if (networkTraffic != null)
        {
            networkRxBytes_ = networkTraffic.get(NETWORK_INTERFACE_POSITION).getNetworkDemand().getRxBytes();
            networkTxBytes_ = networkTraffic.get(NETWORK_INTERFACE_POSITION).getNetworkDemand().getTxBytes();
        }
    }
    
    /**
     * Creates a virtual machine data object.
     * 
     * @param information                           The domain information
     * @return                                      The virtual machine data
     * @throws VirtualMachineMonitoringException 
     * @throws HostMonitoringException 
     */
    private VirtualMachineMonitoringData createDynamicMonitoringData(VirtualMachineInformation information)
        throws VirtualMachineMonitoringException, HostMonitoringException
    {        
        ArrayList<Double> currentUtilization = getCurrentUtilization(information);
        VirtualMachineMonitoringData data = new VirtualMachineMonitoringData();
        data.setUsedCapacity(currentUtilization);
        return data;
    }
    
    /**
     * Computes the used capacity.
     * 
     * @param virtualMachineInformation             The virtual machine information
     * @return                                      The used capacity vector
     * @throws VirtualMachineMonitoringException 
     * @throws HostMonitoringException 
     */
    private ArrayList<Double> getCurrentUtilization(VirtualMachineInformation virtualMachineInformation) 
        throws VirtualMachineMonitoringException, HostMonitoringException
    {
        Guard.check(virtualMachineInformation);
        
        double cpuUtilization = 0;
        double memoryUsage = 0;
        double networkRxBytes = 0;
        double networkTxBytes = 0;
     
        /**
         * Make sure during first monitoring cycle no CPU and networking
         * utilization is computed (values for cpuTimeStamp_ and networkTx/RxBytes_ are
         * not available and result in 100% utilization)
         */
        if (!isFirst_)
        {
            cpuUtilization = computeProzessorUtilization(virtualMachineInformation.getCpuTime());        
            memoryUsage = virtualMachineInformation.getMemoryUsage();
            
            // Only first interface is considered
            List<NetworkTrafficInformation> networkTraffic = virtualMachineInformation.getNetworkTraffic();
            if (networkTraffic != null)
            {
                double traffic = networkTraffic.get(NETWORK_INTERFACE_POSITION).
                                                    getNetworkDemand().getRxBytes();
                networkRxBytes = computeNetworkUtilization(traffic, NetworkDirection.Rx);
                
                traffic = networkTraffic.get(NETWORK_INTERFACE_POSITION).
                                            getNetworkDemand().getTxBytes();
                networkTxBytes = computeNetworkUtilization(traffic, NetworkDirection.Tx);
            }      
        }
               
        NetworkDemand networkDemand = new NetworkDemand(networkRxBytes, networkTxBytes);
        ArrayList<Double> utilizationVector = MathUtils.createCustomVector(cpuUtilization,
                                                                           memoryUsage,
                                                                           networkDemand);
        return utilizationVector;
    }

    /**
     * Computes the CPU utilization.
     * 
     * @param currentCpuTime                        The current cpu time
     * @return                                      The prozessor utilzation
     * @throws VirtualMachineMonitoringException 
     * @throws HostMonitoringException 
     */
    private double computeProzessorUtilization(long currentCpuTime) 
        throws VirtualMachineMonitoringException, HostMonitoringException
    {                  
        long cpuTimeDiff = currentCpuTime - cpuTimeStamp_;
        double cpuUsagePercentage = cpuTimeDiff / (samplingTimeDifference_ * 1.0);

        if (cpuUsagePercentage < 0.0)
        {
            log_.debug("CPU utilization is NEGATIVE!");
            cpuUsagePercentage = 0.0;
        }
              
        return cpuUsagePercentage;
    }
       
    /**
     * Computes the network utilization.
     * 
     * @param currentNetworkTraffic     The current network utilization
     * @param networkDirection          The network direction
     * @return                          Number of kilobytes
     */
    private double computeNetworkUtilization(double currentNetworkTraffic, NetworkDirection networkDirection) 
    {                
        double networkTrafficDifference = 0.0;
        switch (networkDirection)
        {
            case Rx :
                networkTrafficDifference = currentNetworkTraffic - networkRxBytes_;
                break;
                
            case Tx :
                networkTrafficDifference = currentNetworkTraffic - networkTxBytes_;
                break;
                
            default :
                log_.error(String.format("Unknown network direction selected: %s", networkDirection));
                return 0;
        }
        
        double utilization = networkTrafficDifference / 1024;        
        return utilization;
    }

    /**
     * Puts the thread to sleep.
     *  
     * @param milliseconds   The number of milliseconds
     * @throws InterruptedException 
     */
    private void doSleep(long milliseconds) 
        throws InterruptedException
    {
        synchronized (lockObject_)
        {
            lockObject_.wait(milliseconds);
        }
    }
        
    /** 
     * Terminates the thread.
     */
    public synchronized void terminate() 
    {          
        isTerminated_ = true;
        if (isSuspended_)
        {
            wakeup();
        }
    }
    
    /**
     * Suspends the tread.
     */
    public synchronized void setSuspend()
    {
        isSuspended_ = true;
    }
    
    /**
     * Wakeup the thread.
     */
    public synchronized void wakeup() 
    {
        synchronized (lockObject_)
        {
            isSuspended_ = false;
            lockObject_.notify();
        }
    }
}
