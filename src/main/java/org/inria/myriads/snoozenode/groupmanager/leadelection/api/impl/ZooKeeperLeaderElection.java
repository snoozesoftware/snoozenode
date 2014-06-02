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
package org.inria.myriads.snoozenode.groupmanager.leadelection.api.impl;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.configurator.faulttolerance.ZooKeeperSettings;
import org.inria.myriads.snoozenode.groupmanager.leadelection.api.LeaderElection;
import org.inria.myriads.snoozenode.groupmanager.leadelection.listener.LeaderElectionListener;
import org.inria.myriads.snoozenode.util.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Leader election implementation based on the Apache ZooKeeper coordination service.
 * 
 * Inspired by the great tutorials at:
 *  http://zookeeper.apache.org/doc/r3.3.1/recipes.html#sc_leaderElection
 *  http://dengyin2000.iteye.com/blog/858939
 * 
 * @author Eugen Feller
 */
public final class ZooKeeperLeaderElection 
    implements LeaderElection, Watcher, Runnable 
{
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(ZooKeeperLeaderElection.class);
    
    /** The root path. */
    private static final String ROOT_PATH = "/snoozenode";

    /** The election path. */
    private static final String ELECTION_PATH = "/election";
    
    /** Full path. */
    private static final String FULL_PATH = ROOT_PATH + ELECTION_PATH;
    
    /** The connection string. */
    private String connectionString_;
    
    /** The zookeeper reference. */
    private ZooKeeper zookeeper_;

    /** Session timeout. */
    private int sessionTimeout_;
    
    /** Leader election listener. */
    private LeaderElectionListener listener_;

    /** Group manager description. */
    private GroupManagerDescription groupManagerDescription_;  

    /**
     * Leader election based on ZooKeeper.
     * 
     * @param zooKeeperParameters        The zookeeper parameters
     * @param groupManagerDescription    The group manager description
     * @param listener                   The leader election listener
     * @throws Exception                 Exception 
     */
    public ZooKeeperLeaderElection(ZooKeeperSettings zooKeeperParameters, 
                                   GroupManagerDescription groupManagerDescription, 
                                   LeaderElectionListener listener)
        throws Exception
    {
        Guard.check(zooKeeperParameters, groupManagerDescription, listener);
        log_.debug("Initializing ZooKeeper based leader election");
        connectionString_ = zooKeeperParameters.getHosts();
        sessionTimeout_ = zooKeeperParameters.getSessionTimeout();
        groupManagerDescription_ = groupManagerDescription;
        listener_ = listener;
    }
    
    /**
     * Returns my sequence identifier.
     * 
     * @param path  The path
     * @return      The sequence identifier
     */
    private int getMySequenceId(String path)
    {
        Guard.check(path);
        log_.debug("Returning my sequence identifier");
        
        int index = ROOT_PATH.length() + ELECTION_PATH.length() + 3;
        return Integer.parseInt(path.substring(index));
    }
    
    /**
     * Initializes the root path.
     * 
     * @throws KeeperException          The zookeeper exception
     * @throws InterruptedException     The interrupted exception
     */
    private void createRootPath() 
        throws KeeperException, InterruptedException
    {
        log_.debug("Initializing the root path");
        
        try  
        {
            zookeeper_.create(ROOT_PATH, 
                              new byte[0], 
                              ZooDefs.Ids.OPEN_ACL_UNSAFE, 
                              CreateMode.PERSISTENT);
    
            zookeeper_.create(FULL_PATH,
                              new byte[0], 
                              ZooDefs.Ids.OPEN_ACL_UNSAFE, 
                              CreateMode.PERSISTENT);    
        } 
        catch (NodeExistsException exception)
        {
            log_.warn(String.format("Node already exists: %s! Ignoring creation!", exception.getMessage()));
        }
    }
    
    /**
     * Creates a node entry.
     * 
     * @return                      The node entry
     * @throws KeeperException      The zookeeper exception
     * @throws InterruptedException The interrupted exception
     * @throws IOException 
     */
    private String createNodeEntry() 
        throws KeeperException, InterruptedException, IOException
    {
        log_.debug("Creating node entry");
        String value = zookeeper_.create(FULL_PATH + "/n_", 
                                         SerializationUtils.serializeObject(groupManagerDescription_), 
                                         ZooDefs.Ids.OPEN_ACL_UNSAFE, 
                                         CreateMode.EPHEMERAL_SEQUENTIAL);   
        return value;
    }
    
    /**
     * Initializes zookeeper.
     * 
     * @return                          The sequence identifier
     * @throws IOException 
     * @throws InterruptedException 
     * @throws KeeperException 
     */
    private long initializeZooKeeper() 
        throws IOException, KeeperException, InterruptedException 
    {           
        zookeeper_ = new ZooKeeper(connectionString_, sessionTimeout_, this);  
        createRootPath();        
        String path = createNodeEntry();
        return getMySequenceId(path);
    }  
           
    /** Run method. */
    public void run() 
    {  
        try
        {
            String childPath = "Missing"; 
            long mySequenceId = initializeZooKeeper();
            log_.debug(String.format("Connection string: %s, session timeout: %s, sequence id: %d", 
                                     connectionString_, sessionTimeout_, mySequenceId)); 
            while (true) 
            {  
                List<String> childList = getChildList();              
                for (int i = 0; i < childList.size(); i++) 
                {  
                    childPath = childList.get(i);  
                    int childSequenceId = getChildSequenceId(childPath);
                    log_.debug(String.format("The child path is: %s with sequence identifier: %d", 
                                             childPath, 
                                             childSequenceId));
                    if  (childSequenceId < mySequenceId)
                    {
                        log_.debug(String.format("Found a sequence id lower then mine: %d", childSequenceId));
                        break;
                    }
                }  
                
                startChildWatcher(childPath);
            }
        } 
        catch (SocketException exception)
        {
            log_.error(String.format("Socket exception: %s! Are you sure network is available?", 
                                      exception.getMessage()));
        }
        catch (Exception exception)
        {
            log_.error("Exception during leader election", exception);
        }
    }
    
    /**
     * Return the child list.
     * 
     * @return                      The child list
     * @throws KeeperException      The zookeeper exception
     * @throws InterruptedException The interrupted exception
     */
    private List<String> getChildList() 
        throws KeeperException, InterruptedException
    {
        log_.debug("Returning the child list");
        
        zookeeper_.sync(FULL_PATH, null, null);
        List<String> childList = zookeeper_.getChildren(FULL_PATH, false);  
        Collections.sort(childList, Collections.reverseOrder());
        return childList;        
    }

    /**
     * Returns the sequence id.
     * 
     * @param path  The path    
     * @return      The sequence identifier
     */
    private int getChildSequenceId(String path)
    {
        Guard.check(path);
        int sequenceId = Integer.parseInt(path.substring(2));    
        return sequenceId;
    }
    
    /**
     * Starts the leader watcher.
     * 
     * @param childPath      The leader path
     * @throws Exception     The exception          
     */
    private void startChildWatcher(String childPath) 
        throws Exception
    {
        Guard.check(childPath);
        LatchChildWatcher latchChildWatcher = new LatchChildWatcher();  
        
        String fullChildPath = FULL_PATH + "/" + childPath; 
        log_.debug(String.format("Starting the child watcher for: %s", fullChildPath));
        byte[] byteData = zookeeper_.getData(fullChildPath, latchChildWatcher, null);  
        
        GroupManagerDescription groupManagerDescription = 
            (GroupManagerDescription) SerializationUtils.deserializeObject(byteData);
        
        log_.debug(String.format("Processing child path: %s", childPath));
        processChildData(groupManagerDescription);
        latchChildWatcher.await();          
    }
    
    /**
     * Processes the child data.
     * 
     * @param groupManagerDescription       The group manager description
     * @throws Exception 
     */
    private void processChildData(GroupManagerDescription groupManagerDescription)  
        throws Exception
    {
        Guard.check(groupManagerDescription);
        log_.debug(String.format("Child data is: %s", groupManagerDescription.getId()));
        log_.debug(String.format("Local data is: %s", groupManagerDescription_.getId()));
        int comparison = groupManagerDescription.getId().compareTo(groupManagerDescription_.getId());
        if (comparison == 0)
        {
            log_.debug("Starting in group leader mode!");
            listener_.onInitGroupLeader();
        } else
        {
            log_.debug("Starting in group manager mode!");
            listener_.onInitGroupManager();
        }
    }
    
    /**
     * Processes the watcher event.
     * 
     * @param event     The watched event
     */
    public void process(WatchedEvent event) 
    {  
        Guard.check(event);
        log_.debug("Processing the watch event");
        
        KeeperState state = event.getState();
        try
        {
            switch (state)
            {
                case Disconnected:
                    log_.debug("Received disconnected event!");
                    initializeZooKeeper();
                    break;
                    
                case Expired:
                    log_.debug("Received expired event!");
                    initializeZooKeeper();
                    break;
                    
                case SyncConnected:
                    log_.debug("Connection estabilished!");
                    break;
                    
                default:
                    log_.debug(String.format("Unknown keeper state received: %s", state));
            }
        }
        catch (Exception exception)
        {
            log_.debug("Exception during zookeeper initialization", exception);
        }
    }  
    
    /**
     * Latch child watcher.
     * 
     * @author Eugen Feller
     */
    private class LatchChildWatcher 
        implements Watcher 
    {    
        /** Latch. */
        private CountDownLatch latch_;
   
        /**
         * Constructor.
         */
        public LatchChildWatcher()
        {  
            latch_ = new CountDownLatch(1);  
        }  
   
        /**
         * Processes the watch event.
         * 
         * @param event     The watch event
         */
        public void process(WatchedEvent event)
        {  
            log_.debug(String.format("Watcher fired on path: %s, state: %s, type: %s",
                                     event.getPath(), event.getState(), event.getType()));  
            latch_.countDown();  
        }  

        /**
         * Waits until latch counter is 0.
         * 
         * @throws InterruptedException     The interrupted exception
         */
        public void await() 
            throws InterruptedException 
        {  
            latch_.await();  
        }  
    }

    /**
     * Starts the leader election.
     */
    public void start() 
    {
        log_.debug("Starting the leader election algorithm");
        new Thread(this, "ZookeeperLeaderElection").start();
    }  
}
