package org.inria.myriads.snoozenode.database.api.impl.cassandra;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerLocation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.CassandraUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author msimonin
 *
 */
public class TestCassandraRepository extends TestCase 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(TestGroupManagerCassandraRepository.class);
    
    /** Repository under test. */
    private CassandraRepository repository_;

    /** Cassandra Cluster.*/
    private Cluster cluster_;

    /** Keyspace .*/
    private Keyspace keyspace_;

    
    @Override
    protected void setUp() throws Exception 
    {
        System.getProperties().put("org.restlet.engine.loggerFacadeClass", 
                "org.restlet.ext.slf4j.Slf4jLoggerFacade");
        String logFile = "./configs/log4j.xml";
        File file = new File(logFile);
        if (file.exists() && file.canRead()) 
        {   
            DOMConfigurator.configure(logFile);
        } 
        else
        {
            System.out.println("Log file " + logFile + " does not exist or is not readable! Falling back to default!");
            BasicConfigurator.configure();
        }
        cluster_ = HFactory.getOrCreateCluster("Test Cluster", new CassandraHostConfigurator("localhost:9160"));
        
        keyspace_ = HFactory.createKeyspace("snooze", cluster_);   
        
        
        repository_ = new CassandraRepository("localhost:9160");
    
        repository_.clear();
    }
    

    @Override
    protected void tearDown() throws Exception 
    {
        
    }
    
    /**
     * Only one GM in the db 
     * Fetch it.
     */
    public void testGetGroupManagerDescriptionsOnly1GM()
    {
        int i = 0;
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        groupManagerDescription.setId(String.valueOf(i));
        groupManagerDescription.setHostname("mafalda" + String.valueOf(i));
        groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
        groupManagerDescription.getHeartbeatAddress().setPort(9000);
        groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
        repository_.addGroupManagerDescriptionCassandra(
                groupManagerDescription, false, true);
        
        
        ArrayList<GroupManagerDescription> groupManagers = 
                repository_.getGroupManagerDescriptionsOnly(
                        String.valueOf(i),
                        1,
                        false,
                        0,
                        new ArrayList<String>());
        assertEquals(1, groupManagers.size());
        assertEquals("0", groupManagers.get(0).getId());
        
    }
    
    
    /**
     * 10 GMs 
     * 5 tombstoned
     * Request for 10, should return 5 groupmanagers.
     * 
     */
    public void testGetGroupManagerDescriptionsOnlyFetchAllWithTombstones()
    {
        for (int i = 0; i < 10; i++)
        {
            GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
            groupManagerDescription.setId(String.valueOf(i));
            groupManagerDescription.setHostname("mafalda" + String.valueOf(i));
            groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
            groupManagerDescription.getHeartbeatAddress().setPort(9000);
            groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
            groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
            groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
            groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
            repository_.addGroupManagerDescriptionCassandra(
                    groupManagerDescription, false, true);
        }
        
        
        CassandraUtils.drop(keyspace_, Arrays.asList("0", "1", "2", "3", "4"), CassandraUtils.GROUPMANAGERS_CF);
        
        ArrayList<GroupManagerDescription> groupManagers = 
                repository_.getGroupManagerDescriptionsOnly(
                        "",
                        10,
                        false,
                        0,
                        new ArrayList<String>());
        assertEquals(5, groupManagers.size());
        
    }
    
    /**
     * 
     * Test GetGroupManagerOnly.
     * 
     * 10 GMs 
     * Fetch all
     * 
     * 
     */
    public void testGetGroupManagerDescriptionsOnlyFetchAllWithoutTombstones()
    {
        for (int i = 0; i < 10; i++)
        {
            GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
            groupManagerDescription.setId(String.valueOf(i));
            groupManagerDescription.setHostname("mafalda" + String.valueOf(i));
            groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
            groupManagerDescription.getHeartbeatAddress().setPort(9000);
            groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
            groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
            groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
            groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
            repository_.addGroupManagerDescriptionCassandra(
                    groupManagerDescription, false, true);
        }
        
        
        ArrayList<GroupManagerDescription> groupManagers = 
                repository_.getGroupManagerDescriptionsOnly(
                        "",
                        10,
                        false,
                        0,
                        new ArrayList<String>());
        assertEquals(10, groupManagers.size());
        
    }
    
    /**
     * 
     * Get group manager descriptions.
     * 100 groupmanagers inserted.
     * 1/2 unassigned
     * get 25 groupmanagers
     * 
     * @param groupManagerStart
     * @param numberToFetch
     */
    public void testGetGroupManagerDescriptionsOnlyAssignedOnly()
    {
        for (int i = 0; i < 100; i++)
        {
            GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
            groupManagerDescription.setId(String.valueOf(i));
            groupManagerDescription.setHostname("mafalda" + String.valueOf(i));
            groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
            groupManagerDescription.getHeartbeatAddress().setPort(9000);
            groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
            groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
            groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
            groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
            repository_.addGroupManagerDescriptionCassandra(
                    groupManagerDescription, false, true);
        }
        
        for (int i = 0; i < 50; i++)
        {
            CassandraUtils.unassignNodes(
                    keyspace_, 
                    Arrays.asList(String.valueOf(i)),
                    CassandraUtils.GROUPMANAGERS_CF);
        }
        
        List<GroupManagerDescription> groupManagers = 
                repository_.getGroupManagerDescriptionsOnly(
                        "", 
                        25, 
                        true,
                        0, 
                        new ArrayList<String>());
        
        assertEquals(25, groupManagers.size());
        
    }
    
    
    /**
     * 
     * Get group manager descriptions.
     * 100 groupmanagers inserted.
     * with 1 gl 
     * get all groupmanagers
     * 
     * @param groupManagerStart
     * @param numberToFetch
     */
    public void testGetGroupManagerDescriptionsWithoutGroupLeader()
    {
        for (int i = 0; i < 100; i++)
        {
            GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
            groupManagerDescription.setId(String.valueOf(i));
            groupManagerDescription.setHostname("mafalda" + String.valueOf(i));
            groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
            groupManagerDescription.getHeartbeatAddress().setPort(9000);
            groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
            groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
            groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
            groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
            if (i == 10)
            {
                repository_.addGroupManagerDescriptionCassandra(
                        groupManagerDescription, true, true);
            }
            else
            {
                repository_.addGroupManagerDescriptionCassandra(
                        groupManagerDescription, false, true);
            }
        }
        

        List<GroupManagerDescription> groupManagers = 
                repository_.getGroupManagerDescriptionsOnly(
                        "",
                        100,
                        true,
                        0,
                        Arrays.asList(String.valueOf(10)));
        
        assertEquals(99, groupManagers.size());
        
        
    }
    
    
    /**
     * 
     * Test GetLocalControllerDescriptionsOnly.
     * 
     * 10 localcontrollers
     * 5 assigned to gm 0
     * 5 assigned to gm 1
     * Gets all the localcontrollers of gm 1 
     * Should return 5 localcontrollers
     */
    public void testCasandraRepositoryAllLocalControllerOfAGroupmanager()
    {
        for (int i = 0; i < 10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescriptionCassandra(String.valueOf(i % 2), localControllerDescription);
        }
        ArrayList<LocalControllerDescription> localControllers = 
                repository_.getLocalControllerDescriptionsOnly(
                        "1",
                        null,
                        -1,
                        0,
                        false,
                        true);
    
        assertEquals(5, localControllers.size());
    }
    
    /**
     * 
     *  Test GetLocalControllerDescriptionsOnly.
     * 
     * 10 localcontrollers
     * 5 assigned to gm 0
     * 5 assigned to gm 1
     * Gets all the localcontrollers of gm 1
     * 4 localcontrollers unassigned 
     * Should return 10 localcontrollers (4 unassigned and 6 assigned)
     * 
     */
    public void testCasandraRepositoryAllLocalController()
    {
        for (int i = 0; i < 10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescriptionCassandra(String.valueOf(i % 2), localControllerDescription);
        }
        
        List<String> unassigned = Arrays.asList("0", "1", "2", "3");
        CassandraUtils.unassignNodes(keyspace_, unassigned, CassandraUtils.LOCALCONTROLLERS_CF);
        
        ArrayList<LocalControllerDescription> localControllers = 
                repository_.getLocalControllerDescriptionsOnly(
                        null,
                        null,
                        -1,
                        0,
                        false,
                        false);

        assertEquals(10, localControllers.size());
        for (LocalControllerDescription localController : localControllers)
        {
            if (unassigned.contains(localController.getId()))
            {
                assertFalse(localController.getIsAssigned());
            }
            else
            {
                assertTrue(localController.getIsAssigned());
            }
        }
    }
    
    /**
     * 
     * Test GetLocalControllerDescriptionsOnly.
     * 
     * 10 localcontrollers
     * 5 assigned to gm 0
     * 5 assigned to gm 1
     * Gets 3 the localcontrollers of gm 1 
     * Should return 3 localcontrollers
     */
    public void testCasandraRepositorySomeLocalControllerOfAGroupmanager()
    {
        for (int i = 0; i < 10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescriptionCassandra(String.valueOf(i % 2), localControllerDescription);
        }
        
        ArrayList<LocalControllerDescription> localControllers = 
                repository_.getLocalControllerDescriptionsOnly(
                        "1",
                        null,
                        3, 
                        0,
                        false,
                        true);
    
        assertEquals(3, localControllers.size());
    }
    
    /**
     *
     * Test GetLocalControllerDescriptionsOnly.
     * 
     * 10 localcontrollers
     * 5 assigned to gm 0
     * 5 assigned to gm 1 and 2 unassigned
     * Get assigned LC of gm 1
     * Should return 3 localcontrollers
     */
    public void testCasandraRepositoryAssignedLocalControllerOfAGroupmanager()
    {
        for (int i = 0; i < 10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            String localControllerId = String.valueOf(i);
            localControllerDescription.setId(localControllerId);
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            String groupManagerId = String.valueOf(i % 2);
            LocalControllerLocation location = new LocalControllerLocation();
            
            location.setLocalControllerId(localControllerId);
            location.setGroupManagerId(groupManagerId);
            NetworkAddress networkAddress = new NetworkAddress();
            networkAddress.setAddress("10.0.0." + groupManagerId);
            networkAddress.setPort(6000 + i % 2);
            location.setGroupManagerControlDataAddress(networkAddress);
            localControllerDescription.setLocation(location);
            repository_.addLocalControllerDescriptionCassandra(groupManagerId, localControllerDescription);
        }
        
        CassandraUtils.unassignNodes(keyspace_, Arrays.asList("1", "3"), CassandraUtils.LOCALCONTROLLERS_CF);
        
        ArrayList<LocalControllerDescription> localControllers = 
                repository_.getLocalControllerDescriptionsOnly(
                        "1",
                        null,
                        -1,
                        0,
                        false,
                        true);
    
        assertEquals(3, localControllers.size());
        for (LocalControllerDescription localController : localControllers)
        {
            assertTrue(Arrays.asList("5", "7", "9").contains(localController.getId()));
            assertEquals(localController.getId(), localController.getLocation().getLocalControllerId());
            assertEquals("10.0.0.1", localController.getLocation().getGroupManagerControlDataAddress().getAddress());
            assertEquals(6001, localController.getLocation().getGroupManagerControlDataAddress().getPort());
        }
        
    }
    
    
    /** 
    * 
    * Test GetLocalControllerDescriptionsOnly.
    * 
    * 
    * 10 localcontrollers
    * 5 assigned to gm 0
    * 5 assigned to gm 1 
    * #1 -> unassigned and Active
    * #3 -> unassigned and Passive
    * #5 -> assigned and Active
    * #7 -> assigned and Passive
    * #9 -> assigned and Active 
    * Get assigned LC of gm 1 and active
    * Should return 2 localcontrollers (5 and 9)
    */
   public void testCasandraRepositoryAssignedAndAciveLocalControllerOfAGroupmanager()
   {
       for (int i = 0; i < 10; i++)
       {
           
           LocalControllerDescription localControllerDescription = new LocalControllerDescription();
           localControllerDescription.setId(String.valueOf(i));
           localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
           if ((i - 1) % 4 != 0)
               localControllerDescription.setStatus(LocalControllerStatus.PASSIVE);
           
           repository_.addLocalControllerDescriptionCassandra(String.valueOf(i % 2), localControllerDescription);
       }
       
       CassandraUtils.unassignNodes(keyspace_, Arrays.asList("1", "3"), CassandraUtils.LOCALCONTROLLERS_CF);
       
       ArrayList<LocalControllerDescription> localControllers = 
               repository_.getLocalControllerDescriptionsOnly(
                       "1",
                       null,
                       -1,
                       0, 
                       true,
                       true);
   
       assertEquals(2, localControllers.size());
       for (LocalControllerDescription localController : localControllers)
       {
           assertTrue(Arrays.asList("5", "9").contains(localController.getId()));
       }
       
       
   }
    
    /**
     * Test GetLocalControllerDescriptionsOnly.
     * 
     * 10 LCs
     * Fetch LC #2 
     * 
     */
    public void testCasandraRepository1LC()
    {
        for (int i = 0; i < 10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescriptionCassandra(String.valueOf(i % 2), localControllerDescription);
        }
        
        ArrayList<LocalControllerDescription> localControllers = 
                repository_.getLocalControllerDescriptionsOnly(
                        null,
                        String.valueOf("2"),
                        1,
                        0,
                        false,
                        true);
    
        assertEquals(1, localControllers.size());
        //assertEquals("2", localControllers.get("2").getId());
        assertEquals("2", localControllers.get(0).getId());
        
    }
    
    /**
     * 
     * Test GetVirtualMachineDescriptionsOnly.
     * 
     * 1 GM
     * 2 LCs
     * 10 vms (round robin assignements)
     * 
     * 
     */
    public void testGetVirtualMachineDescriptionsGetAll1GM2LCs10VMs()
    {
        for (int i = 0; i < 10; i++)
        {
            VirtualMachineMetaData virtualMachine = new VirtualMachineMetaData();
            virtualMachine.getVirtualMachineLocation().setVirtualMachineId("vm" + String.valueOf(i));
            virtualMachine.getVirtualMachineLocation().setGroupManagerId("gm0");
            virtualMachine.getVirtualMachineLocation().setLocalControllerId("lc" + String.valueOf(i % 2));
            repository_.addVirtualMachineCassandra(virtualMachine);
        }
        ArrayList<VirtualMachineMetaData> virtualMachines =
                repository_.getVirtualMachineDescriptionsOnly(
                        null,
                        null,
                        null,
                        -1,
                        0,
                        true);
        
        //Gets all
        assertEquals(10, virtualMachines.size());
        
        //unassign some of them
        CassandraUtils.unassignNodes(
                keyspace_, 
                Arrays.asList("vm0", "vm1", "vm5", "vm6"), 
                CassandraUtils.VIRTUALMACHINES_CF);
        virtualMachines = repository_.getVirtualMachineDescriptionsOnly(null, null, null, -1, 0, true);
        assertEquals(6, virtualMachines.size());
        
        //get vm from lc1 (with unassign)
        virtualMachines = repository_.getVirtualMachineDescriptionsOnly(null, "lc1", null, -1, 0, false);
        assertEquals(5, virtualMachines.size());
        
      //get vm from lc1 (with unassign) with limit
        virtualMachines = repository_.getVirtualMachineDescriptionsOnly(null, "lc1", null, 2, 0, false);
        assertEquals(2, virtualMachines.size());
        
        //get vm from lc1 (without unassign)
        virtualMachines = repository_.getVirtualMachineDescriptionsOnly(null, "lc1", null, -1, 0, true);
        assertEquals(3, virtualMachines.size());
        
        //get vm from lc1 (with unassign) with gm0
        virtualMachines = repository_.getVirtualMachineDescriptionsOnly("gm0", "lc1", null, -1, 0, false);
        assertEquals(5, virtualMachines.size());
        
        //get vm from lc1 (without unassign) with gm0
        virtualMachines = repository_.getVirtualMachineDescriptionsOnly("gm0", "lc1", null, -1, 0, true);
        assertEquals(3, virtualMachines.size());
        
        //get vm from lc1 (with unassign) with gm1 -> not found
        virtualMachines = repository_.getVirtualMachineDescriptionsOnly("gm1", "lc1", null, -1, 0, false);
        assertEquals(0, virtualMachines.size());
        
        // get a specific vm (found)
        virtualMachines = repository_.getVirtualMachineDescriptionsOnly("gm0", "lc0", "vm0", 1, 0, false);
        assertEquals(1, virtualMachines.size());
        assertEquals("vm0", virtualMachines.get(0).getVirtualMachineLocation().getVirtualMachineId());
        
  
        
    }
    
}
