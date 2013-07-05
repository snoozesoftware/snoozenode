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
package org.inria.myriads.snoozenode.groupmanager.monitoring.consumer;

import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.LocalControllerDataTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local controller summary information consumer.
 * 
 * @author Eugen Feller
 */
public final class LocalControllerSummaryConsumer 
    implements Runnable
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerSummaryConsumer.class);
    
    /** Queue with data. */
    private BlockingQueue<LocalControllerDataTransporter> dataQueue_;
            
    /** The state machine. */
    private StateMachine stateMachine_;
    
    /** Group maanger repository. */
    private GroupManagerRepository repository_;
    
    /** Terminates the consumer. */
    private boolean isTerminated_;
    
    /**
     * Local controller monitoring data consumer.
     * 
     * @param dataQueue       The data queue reference
     * @param stateMachine    The state machine
     * @param repository      The repository
     * @throws Exception      The exception
     */
    public LocalControllerSummaryConsumer(BlockingQueue<LocalControllerDataTransporter> dataQueue,
                                          StateMachine stateMachine,
                                          GroupManagerRepository repository)
        throws Exception 
    {
        log_.debug("Initializing the local controller summary information consumer");
        dataQueue_ = dataQueue;
        stateMachine_ = stateMachine;
        repository_ = repository;
        new Thread(this).start();   
    }
    
    /** The run method. */
    public void run()
    {
        try
        {
            while (true)
            {                            
                LocalControllerDataTransporter monitoringData = dataQueue_.take();   
                String localControllerId = monitoringData.getLocalControllerId();
                if (monitoringData.getData()!=null)
                {
                    repository_.addAggregatedMonitoringData(localControllerId, monitoringData.getData());
                }
                
                if (monitoringData.getMetricData()!=null && monitoringData.getMetricData().getMetricData().size()>0)
                {
                    repository_.addMetricData(localControllerId, monitoringData.getMetricData());
                }
                
                boolean isStable = monitoringData.getState().equals(LocalControllerState.STABLE);                
                if (!isStable)
                {
                    log_.debug("Anomaly on local controller detected!");           
                    stateMachine_.resolveAnomaly(localControllerId, monitoringData.getState());
                }
            }
        }
        catch (InterruptedException exception) 
        {
            if (!isTerminated_)
            {
                log_.error("Local controller summary information consumer was interruped: %s",  
                           exception.getMessage());
            }
        }
        
        log_.debug("Local controller summary consumer is stopped!");
    }
    
    /**
     * Terminates the consumer.
     */
    public void terminate()
    {
        isTerminated_ = true;
        Thread.interrupted();
    }
}
