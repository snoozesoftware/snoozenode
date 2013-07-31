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
package org.inria.myriads.snoozenode.groupmanager;

import java.net.BindException;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.NodeRole;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.faulttolerance.ZooKeeperSettings;
import org.inria.myriads.snoozenode.configurator.networking.NetworkingSettings;
import org.inria.myriads.snoozenode.exception.GroupManagerInitException;
import org.inria.myriads.snoozenode.groupmanager.init.GroupLeaderInit;
import org.inria.myriads.snoozenode.groupmanager.init.GroupManagerInit;
import org.inria.myriads.snoozenode.groupmanager.leadelection.LeaderElectionFactory;
import org.inria.myriads.snoozenode.groupmanager.leadelection.api.LeaderElection;
import org.inria.myriads.snoozenode.groupmanager.leadelection.listener.LeaderElectionListener;
import org.inria.myriads.snoozenode.heartbeat.HeartbeatFactory;
import org.inria.myriads.snoozenode.heartbeat.listener.HeartbeatListener;
import org.inria.myriads.snoozenode.heartbeat.message.HeartbeatMessage;
import org.inria.myriads.snoozenode.heartbeat.receiver.HeartbeatMulticastReceiver;
import org.inria.myriads.snoozenode.util.ManagementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Group manager backend logic.
 * 
 * @author Eugen Feller
 */
public final class GroupManagerBackend 
    implements LeaderElectionListener, HeartbeatListener
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerBackend.class);
    
    /** Group leader initialization. */
    private GroupLeaderInit groupLeaderInit_;
            
    /** Group manager logic. */
    private GroupManagerInit groupManagerInit_;
    
    /** Nodeparams reference. */
    private NodeConfiguration nodeConfiguration_;
        
    /** Group manager description. */
    private GroupManagerDescription groupManagerDescription_;
     
    /** Heartbeat listener thread. */
    private HeartbeatMulticastReceiver heartbeatListener_;
        
    /** Heartbeat messsage. */
    private HeartbeatMessage heartbeat_;
    
    /** Indicates successfull assignement. */
    private boolean isAssigned_;
    
    /**
     * Constructor.
     * 
     * @param nodeConfiguration    The node parameters
     * @throws Exception           The exception
     */
    public GroupManagerBackend(NodeConfiguration nodeConfiguration)
        throws Exception 
    {
        Guard.check(nodeConfiguration);
        log_.debug("Starting group manager backend");
    
        nodeConfiguration_ = nodeConfiguration;
        createGroupManagerDescription();
        initializeLeaderElection();  
    }
        
    /**
     * Creates group manager description.
     */
    private void createGroupManagerDescription()
    {
        NodeRole nodeRole = nodeConfiguration_.getNode().getRole();
        NetworkingSettings networkingSettings = nodeConfiguration_.getNetworking();
        groupManagerDescription_ = ManagementUtils.createGroupManagerDescription(nodeRole, networkingSettings);
    }
    
    /**
     * Starts the heartbeat multicast listener.
     * 
     * @throws Exception 
     */
    public void onInitGroupManager() 
        throws Exception
    {        
        if (heartbeatListener_ == null)
        {
            log_.debug("Starting the group leader multicast listener");
            NetworkAddress address = 
                nodeConfiguration_.getNetworking().getMulticast().getGroupLeaderHeartbeatAddress();
            int timeout = nodeConfiguration_.getFaultTolerance().getHeartbeat().getTimeout();
            heartbeatListener_ = HeartbeatFactory.newHeartbeatMulticastListener(address, timeout, this);       
            new Thread(heartbeatListener_).start();     
        }
    }
    
    /** 
     * Starts the leader elector.
     * @throws Exception 
     */
    private void initializeLeaderElection() 
        throws Exception 
    {
        ZooKeeperSettings settings = nodeConfiguration_.getFaultTolerance().getZooKeeper();
        LeaderElection leaderElection = LeaderElectionFactory.newLeaderElection(settings, 
                                                                                groupManagerDescription_,
                                                                                this);
        leaderElection.start();
    }
    
    /**
     * Returns the group leader initialization.
     * 
     * @return     The group leader initialization
     */
    public GroupLeaderInit getGroupLeaderInit() 
    {
        return groupLeaderInit_;
    }
    
    /**
     * Returns the group maanger initialization.
     * 
     * @return     The group manager initialization
     */
    public GroupManagerInit getGroupManagerInit() 
    {
        return groupManagerInit_;
    }
    
    /**
     * Returns the node parameters.
     * 
     * @return     The node parameters
     */
    public NodeConfiguration getNodeConfiguration() 
    {
        return nodeConfiguration_;
    }
    
    /**
     * Returns the group manager description.
     * 
     * @return     The group manager description
     */
    public GroupManagerDescription getGroupManagerDescription()
    {
        return groupManagerDescription_;
    }
        
    /**
     * Prepares the group leader switch.
     * 
     * @return                         true if preparation was ok, false otherwise
     */
    private boolean prepareGroupLeaderSwitch() 
    {
        log_.debug("Preparing group leader switch");
        try
        {
            if (heartbeatListener_ != null)
            {
                heartbeatListener_.terminate();
            }
            
            if (groupManagerInit_ != null)
            {
                groupManagerInit_.getRepository().fillGroupManagerDescription(groupManagerDescription_);
                log_.debug("groupManagerDescription filled and local controllers = " + groupManagerDescription_.getLocalControllers().size());
                groupManagerInit_.stopServices();
            }
        } 
        catch (Exception exception)
        {
            log_.error("Exception during group leader switch preparation", exception);
            return false;
        }
        
        return true;
    }
    
    /**
     * Called by the leader election algorithm if current group manager becomes group leader.
     *
     * @return     true if everything ok, false otherwise
     */
    public boolean onInitGroupLeader()
    {
        log_.debug("Starting the group leader logic");     
    
        boolean isPrepared = prepareGroupLeaderSwitch();
        if (!isPrepared)
        {
            return false;
        }
        
        try 
        {       
            groupLeaderInit_ = new GroupLeaderInit(nodeConfiguration_, groupManagerDescription_);
        }   
        catch (Exception exception) 
        {
            log_.error("Exception", exception);
        }
        finally
        {
            if (groupLeaderInit_ != null)
            {
                log_.debug("Group leader logic started successfully!");
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Heartbeat event arrival event.
     * 
     * @param heartbeat    Heartbeat message
     */
    public void onHeartbeatArrival(HeartbeatMessage heartbeat) 
    {   
        Guard.check(heartbeat);
        log_.debug(String.format("Received group leader heartbeat message from: %s, port: %d", 
                                 heartbeat.getListenSettings().getControlDataAddress().getAddress(),
                                 heartbeat.getListenSettings().getControlDataAddress().getPort()));     
                
        if (heartbeat_ == null || heartbeat_.getId().compareTo(heartbeat.getId()) != 0)
        {
            log_.debug("Updating global heartbeat information!");
            heartbeat_ = heartbeat;
        } else if (isAssigned_)
        {
            log_.debug("Ignoring heartbeat message! Already assigned to working group leader!");
            return;
        }
                                  
        boolean hasJoined = false;
        try 
        {
            if (groupManagerInit_ == null)
            {
                groupManagerInit_ = new GroupManagerInit(nodeConfiguration_, groupManagerDescription_);
            } 
                          
            GroupManagerDescription groupLeader = ManagementUtils.createGroupLeaderDescriptionFromHeartbeat(heartbeat);
            hasJoined = groupManagerInit_.onGroupLeaderJoin(groupLeader);
        } 
        catch (GroupManagerInitException exception)
        {
            log_.error(String.format("Group manager initialization error: %s", exception.getMessage()));
        }
        catch (BindException exception)
        {
            log_.error(String.format("Binding error: %s", exception.getMessage()));
        }
        catch (Exception exception) 
        {
            log_.error("Exception", exception);
        } 
        finally
        {
            if (hasJoined)
            {
                log_.debug("Group leader joined successfully!");
                isAssigned_ = true;
            } else
            {
                log_.debug("Failed to join the group leader!");
            }
        }
    }
        
    /** 
     * Called when heartbeat message was not received for timeout.
     */
    public void onHeartbeatFailure() 
    {
        log_.debug("Failed to receive group leader heartbeat message!");        
        isAssigned_ = false;
                
    }
}
