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

import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Heartbeat data producer.
 * 
 * @author Eugen Feller
 */
public final class VirtualMachineHeartbeatDataProducer 
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(VirtualMachineHeartbeatDataProducer.class);
    
    /** Data queue reference. */
    private BlockingQueue<AggregatedVirtualMachineData> dataQueue_;

    /** Monitoring interval. */
    private int monitoringInterval_;

    /** Virtual machine heartbeat data. */
    private AggregatedVirtualMachineData heartbeatMessage_;
    
    /** Lock object. */
    private Object lockObject_;

    /** Terminated. */
    private boolean isTerminated_;

    /**
     * Virtual machine heartbeat producer.
     * 
     * @param localControllerId     The local controller identifier
     * @param monitoringInterval    The monitoring interval
     * @param dataQueue             The data queue
     */
    public VirtualMachineHeartbeatDataProducer(String localControllerId, 
                                               int monitoringInterval,
                                               BlockingQueue<AggregatedVirtualMachineData> dataQueue)
    {
        Guard.check(localControllerId, dataQueue);
        log_.debug("Initializing the virtual machine heartbeat producer");
        monitoringInterval_ = monitoringInterval;
        dataQueue_ = dataQueue;
        heartbeatMessage_ = new AggregatedVirtualMachineData("heartbeat", null);
        lockObject_ = new Object();
    }
    
    /** The run() method. */
    public void run() 
    {
        try
        {
            while (!isTerminated_)
            {                    
                log_.debug("Adding virtual machine heartbeat data to the queue");
                dataQueue_.add(heartbeatMessage_);
                
                synchronized (lockObject_)
                {
                    lockObject_.wait(monitoringInterval_);
                }
            }            
        }
        catch (InterruptedException exception) 
        {
            log_.error(String.format("Virtual machine heartbeat data producer was interruped: %s", 
                                      exception.getMessage()));
        }
        
        log_.debug("Virtual machine heartbeat producer is stopped!");
    }
            
    /** 
     * Terminates the thread.
     */
    public synchronized void terminate() 
    {
        log_.debug("Terminating the heartbeat producer");
        synchronized (lockObject_)
        {
            isTerminated_ = true;
            lockObject_.notify();
        }
    }  
}
