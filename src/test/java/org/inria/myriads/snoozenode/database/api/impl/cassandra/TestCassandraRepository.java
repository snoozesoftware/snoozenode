package org.inria.myriads.snoozenode.database.api.impl.cassandra;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.HColumnFamilyImpl;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HColumnFamily;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.ListenSettings;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.AssignedGroupManager;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.CassandraUtils;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author msimonin
 *
 */
/**
 * @author msimonin
 *
 */
public class TestCassandraRepository extends TestCase 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(TestGroupManagerCassandraRepository.class);
    
    private CassandraRepository repository_;

    private Cluster cluster_;

    private Keyspace keyspace_;

    
    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
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
        cluster_ = HFactory.getOrCreateCluster("Test Cluster",new CassandraHostConfigurator("localhost:9160"));
        
        keyspace_ = HFactory.createKeyspace("snooze", cluster_);   
        
        
        repository_ = new CassandraRepository("localhost:9160");
    
        repository_.clear();
    }
    

    @Override
    protected void tearDown() throws Exception {
        
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
        
        
        ArrayList<GroupManagerDescription> groupManagers = repository_.getGroupManagerDescriptionsOnly(String.valueOf(i), 1, false,0);
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
        for (int i = 0; i<10; i++)
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
        
        ArrayList<GroupManagerDescription> groupManagers = repository_.getGroupManagerDescriptionsOnly("", 10, false,0);
        assertEquals(5, groupManagers.size());
        
    }
    
    /**
     * 10 GMs 
     * Fetch all
     * 
     * 
     */
    public void testGetGroupManagerDescriptionsOnlyFetchAllWithoutTombstones()
    {
        for (int i = 0; i<10; i++)
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
        
        
        ArrayList<GroupManagerDescription> groupManagers = repository_.getGroupManagerDescriptionsOnly("", 10, false,0);
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
        for (int i=0; i<100; i++)
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
        
        for(int i=0 ; i<50; i++)
        {
            CassandraUtils.unassignNodes(keyspace_, Arrays.asList(String.valueOf(i)),CassandraUtils.GROUPMANAGERS_CF);
        }
        
        List<GroupManagerDescription> groupManagers = repository_.getGroupManagerDescriptionsOnly("", 25, true,0);
        
        assertEquals(25, groupManagers.size());
        
    }
    
    
    /**
     * 
     * 10 localcontrollers
     * 5 assigned to gm 0
     * 5 assigned to gm 1
     * Gets all the localcontrollers of gm 1 
     * Should return 5 localcontrollers
     */
    public void testCasandraRepositoryAllLocalControllerOfAGroupmanager()
    {
        for(int i=0; i<10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescriptionCassandra(String.valueOf(i%2), localControllerDescription);
        }
        
        HashMap<String, LocalControllerDescription> localControllers = repository_.getLocalControllerDescriptionsOnly("1", null, -1, 0, false, true);
    
        assertEquals(5, localControllers.size());
    }
    
    /**
     * 
     * 10 localcontrollers
     * 5 assigned to gm 0
     * 5 assigned to gm 1
     * Gets 3 the localcontrollers of gm 1 
     * Should return 3 localcontrollers
     */
    public void testCasandraRepositorySomeLocalControllerOfAGroupmanager()
    {
        for(int i=0; i<10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescriptionCassandra(String.valueOf(i%2), localControllerDescription);
        }
        
        HashMap<String, LocalControllerDescription> localControllers = repository_.getLocalControllerDescriptionsOnly("1", null, 3, 0, false, true);
    
        assertEquals(3, localControllers.size());
    }
    
    /**
     * 
     * 10 localcontrollers
     * 5 assigned to gm 0
     * 5 assigned to gm 1 and 2 unassigned
     * Get assigned LC of gm 1
     * Should return 3 localcontrollers
     */
    public void testCasandraRepositoryAssignedLocalControllerOfAGroupmanager()
    {
        for(int i=0; i<10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescriptionCassandra(String.valueOf(i%2), localControllerDescription);
        }
        
        CassandraUtils.unassignNodes(keyspace_, Arrays.asList("1", "3"), CassandraUtils.LOCALCONTROLLERS_CF);
        
        HashMap<String, LocalControllerDescription> localControllers = repository_.getLocalControllerDescriptionsOnly("1", null, -1, 0, false, true);
    
        assertEquals(3, localControllers.size());
        assertTrue(localControllers.containsKey("5"));
        assertTrue(localControllers.containsKey("7"));
        assertTrue(localControllers.containsKey("9"));
    }
    
    
    /** 
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
       for(int i=0; i<10; i++)
       {
           
           LocalControllerDescription localControllerDescription = new LocalControllerDescription();
           localControllerDescription.setId(String.valueOf(i));
           localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
           if((i-1)%4 != 0)
               localControllerDescription.setStatus(LocalControllerStatus.PASSIVE);
           
           repository_.addLocalControllerDescriptionCassandra(String.valueOf(i%2), localControllerDescription);
       }
       
       CassandraUtils.unassignNodes(keyspace_, Arrays.asList("1", "3"), CassandraUtils.LOCALCONTROLLERS_CF);
       
       HashMap<String, LocalControllerDescription> localControllers = repository_.getLocalControllerDescriptionsOnly("1", null, -1, 0, true, true);
   
       assertEquals(2, localControllers.size());
       assertTrue(localControllers.containsKey("5"));
       assertTrue(localControllers.containsKey("9"));
   }
    
    /**
     * 
     * 10 LCs
     * Fetch LC #2 
     * 
     */
    public void testCasandraRepository1LC()
    {
        for(int i=0; i<10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescriptionCassandra(String.valueOf(i%2), localControllerDescription);
        }
        
        HashMap<String, LocalControllerDescription> localControllers = repository_.getLocalControllerDescriptionsOnly(null,String.valueOf("2"), 1, 0, false, true);
    
        assertEquals(1, localControllers.size());
        assertEquals("2", localControllers.get("2").getId());
    }
    
}
