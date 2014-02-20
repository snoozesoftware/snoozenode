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
package org.inria.myriads.snoozenode.groupmanager.monitoring.receiver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.localcontroller.monitoring.enums.LocalControllerState;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.LocalControllerDataTransporter;
import org.inria.myriads.snoozenode.tcpip.DataListener;
import org.inria.myriads.snoozenode.tcpip.TCPDataReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local controller monitoring data receiver.
 * 
 * @author Eugen Feller
 */
public final class LocalControllerSummaryReceiver extends TCPDataReceiver 
    implements DataListener 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(LocalControllerSummaryReceiver.class);
    
    /** The blocking queue reference. */
    private BlockingQueue<LocalControllerDataTransporter> dataQueue_;
   
    /** Group manager identifiers. */
    private Map<String, String> localControllerIds_;
    
    /** State machine. */
    private StateMachine stateMachine_;

    /** Group manager repository. */
    private GroupManagerRepository repository_;
   
    /**
     * Constructor.
     * 
     * @param networkAddress     The network address
     * @param timeOut            The timeout
     * @param dataQueue          The data queue
     * @param stateMachine       The state machine
     * @param repository         The group manager repository
     * @throws Exception 
     */
    public LocalControllerSummaryReceiver(NetworkAddress networkAddress, 
                                          int timeOut,
                                          BlockingQueue<LocalControllerDataTransporter> dataQueue, 
                                          StateMachine stateMachine,
                                          GroupManagerRepository repository) 
        throws Exception 
    {
        super(networkAddress, timeOut);
        log_.debug("Initializing the local controller data receiver");
        
        dataQueue_ = dataQueue;
        stateMachine_ = stateMachine;
        repository_ = repository;
        localControllerIds_ = new HashMap<String, String>();
        starReceiver();
        log_.debug("Local controller monitoring data receiver started");
    }
    
    /**
     * Starts the TCP data receiver.
     */
    private void starReceiver()
    {
        setHandler(this);
        new Thread(this, "LocalControllerSummaryReceiver").start();
    }
    
    /**
     * The failure event callback.
     * 
     * @param workerThreadId   The worker thread identifier
     */
    @Override
    public synchronized void onFailure(String workerThreadId) 
    {
        Guard.check(workerThreadId);
        log_.debug(String.format("Failed to receive local controller summary information from worker thread: %s",
                                 workerThreadId));
        String localControllerId = localControllerIds_.get(workerThreadId);
        if (localControllerId == null)
        {
            log_.debug("No local controller identifier exists for this worker thread!");
            return;
        }
                
        boolean isDropped = repository_.dropLocalController(localControllerId, false);
        if (isDropped)
        {
            log_.debug("Local controller dropped successfully!");
        }  
    }
    
    /** 
     * Data event callback.
     *  
     * @param data              The data object
     * @param workerThreadId    The worker thread identifier
     */
    @Override
    public synchronized void onDataArrival(Object data, String workerThreadId) 
    {
        Guard.check(data, workerThreadId);
        LocalControllerDataTransporter monitoringData = (LocalControllerDataTransporter) data;
        String localControllerId = monitoringData.getLocalControllerId();
        if (localControllerIds_.get(workerThreadId) == null)
        {
            log_.debug(String.format("No mapping exists for this worker thread! Adding %s to %s mapping",
                                     workerThreadId,
                                     localControllerId));
            localControllerIds_.put(workerThreadId, localControllerId);     
        }
        
        // TODO : figure out why it's needed (or no).
        if (monitoringData.getVirtualMachineAggregatedData() == null)
        {
            dataQueue_.add(monitoringData);
        } 
    
        log_.debug(String.format("Worker thread: %s received local controller summary information from: %s",
                                 workerThreadId,
                                 localControllerId));
        
        if (stateMachine_.isBusy() && !monitoringData.getState().equals(LocalControllerState.STABLE))
        {
            log_.debug("System is BUSY! Skipping overloaded/underloaded local controller monitoring data!");
            // otherwise ? 
            return;
        }
        
        log_.debug(String.format("Adding local controller %s summary information for to the queue", 
                                 localControllerId));
        dataQueue_.add(monitoringData);     
    }
}
