package org.inria.myriads.snoozenode.database.api.impl.cassandra;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.CassandraUtils;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author msimonin
 *
 */
public class TestGroupLeaderCassandraRepository extends TestCase 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(TestGroupManagerCassandraRepository.class);
    
    /** The repository under test.*/
    private GroupLeaderCassandraRepository repository_;
    
    /** The cluster.*/
    private Cluster cluster_;
    
    /** The keyspace.*/
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
        
        GroupManagerDescription groupLeader = new GroupManagerDescription();
        groupLeader.setId("groupleader");
        String[] subnets = new String[1];
        subnets[0] = "192.168.2.1/31";
        repository_ = new GroupLeaderCassandraRepository(groupLeader, subnets, 60, "localhost:9160");
        //repository_ = new GroupLeaderCassandraRepository();
        repository_.clear();
    }
    

    @Override
    protected void tearDown() throws Exception 
    {

    }
    
    /**
     * Add a group manager description to the repository.
     */
    public void testAddGroupManagerDescription() 
    {
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        groupManagerDescription.setId("1234");
        groupManagerDescription.setHostname("mafalda");
        groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
        groupManagerDescription.getHeartbeatAddress().setPort(9000);
        groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
        
        // Add
        repository_.addGroupManagerDescription(groupManagerDescription);
        
        // Check the database
        // Get the description 
        StringSerializer stringSerializer = new StringSerializer();
        HColumnFamily<String, String> columnFamily =
                new HColumnFamilyImpl<String, String>(
                        keyspace_,
                        CassandraUtils.GROUPMANAGERS_CF,
                        stringSerializer,
                        stringSerializer);
        
            columnFamily.addKey("1234")
            .addColumnName("hostname")
            .addColumnName("listenSettings")
            .addColumnName("hearbeatAddress")
            .addColumnName("isAssigned");
        
        NetworkAddress heartbeatAddress = 
                (NetworkAddress) columnFamily.getValue("heartbeatAddress", new JsonSerializer(NetworkAddress.class));
        ListenSettings listenSettings = 
                (ListenSettings) columnFamily.getValue("listenSettings", new JsonSerializer(ListenSettings.class));
        
        assertEquals(groupManagerDescription.getId(), "1234");
        //assertEquals(groupManagerDescription.getHostname(), columnFamily.getString("hostname"));
        assertEquals(groupManagerDescription.getHeartbeatAddress().getAddress(), heartbeatAddress.getAddress());
        assertEquals(groupManagerDescription.getHeartbeatAddress().getPort(), heartbeatAddress.getPort());
        assertEquals(
                groupManagerDescription.getListenSettings().getControlDataAddress().getAddress(),
                listenSettings.getControlDataAddress().getAddress());
        
        assertEquals(
                groupManagerDescription.getListenSettings().getControlDataAddress().getPort(),
                listenSettings.getControlDataAddress().getPort());
        
    }

    
    /**
     * 
     * 2 GM descriptions with 10 monitoring values are inserted.
     * We retrieved these descriptions with 2 monitoring values (timestamp 9l and 8l)
     * 
     * 
     */
    public void testGetGroupManagerDescriptions() 
    {
        
        
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        groupManagerDescription.setId("1234567");
        groupManagerDescription.setHostname("mafalda");
        groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
        groupManagerDescription.getHeartbeatAddress().setPort(9000);
        groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
        
        
        repository_.addGroupManagerDescription(groupManagerDescription);
        
        
        ArrayList<Double> capacity = new ArrayList<Double>();
        capacity.add(1d);
        List<GroupManagerSummaryInformation> summaryInformationList =
                generateGroupManagerSummaryInformation(10, capacity);
        
        for (GroupManagerSummaryInformation summary : summaryInformationList)
        {
            repository_.addGroupManagerSummaryInformation("1234567", summary);
        }
        
        groupManagerDescription = new GroupManagerDescription();
        groupManagerDescription.setId("12345678");
        groupManagerDescription.setHostname("mafalda");
        groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.2");
        groupManagerDescription.getHeartbeatAddress().setPort(9000);
        groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.2");
        groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.2");
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
        repository_.addGroupManagerDescription(groupManagerDescription);

        summaryInformationList = generateGroupManagerSummaryInformation(10, capacity);
        for (GroupManagerSummaryInformation summary : summaryInformationList)
        {
            repository_.addGroupManagerSummaryInformation("12345678", summary);
        }
        
        ArrayList<GroupManagerDescription> retrievedGroupManagers = repository_.getGroupManagerDescriptions(2);
        
        assertEquals(2, retrievedGroupManagers.size());
        assertEquals(2, retrievedGroupManagers.get(0).getSummaryInformation().size());
        assertEquals(2, retrievedGroupManagers.get(1).getSummaryInformation().size());
        assertEquals(capacity, retrievedGroupManagers.get(0).getSummaryInformation().get(9L).getActiveCapacity());
        assertEquals(capacity, retrievedGroupManagers.get(0).getSummaryInformation().get(9L).getPassiveCapacity());
        assertEquals(capacity, retrievedGroupManagers.get(0).getSummaryInformation().get(9L).getRequestedCapacity());
        assertEquals(capacity, retrievedGroupManagers.get(0).getSummaryInformation().get(9L).getUsedCapacity());
    }

    /**
     * 
     * 1 GM description with 10 monitoring values is inserted.
     * We retrieved this description with 2 monitoring values (timestamp 9l and 8l)
     * 
     * 
     */
    public void testGetGroupManagerDescriptionNotFound() 
    {
        for (int i = 0; i < 10; i++)
        {
            GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
            groupManagerDescription.setId("gm" + String.valueOf(i));
            groupManagerDescription.setHostname("mafalda");
            groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
            groupManagerDescription.getHeartbeatAddress().setPort(9000);
            
            groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
            groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
            groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
            groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
            
            repository_.addGroupManagerDescription(groupManagerDescription);
        }
        
        GroupManagerDescription retrievedDescription = repository_.getGroupManagerDescription("gm10", 0);
        assertNull(retrievedDescription);
        
        CassandraUtils.drop(keyspace_, Arrays.asList("gm0"), CassandraUtils.GROUPMANAGERS_CF);
        retrievedDescription = repository_.getGroupManagerDescription("gm0", 0);
        assertNull(retrievedDescription);
        
        
        CassandraUtils.unassignNodes(keyspace_, Arrays.asList("gm4"), CassandraUtils.GROUPMANAGERS_CF);
        retrievedDescription = repository_.getGroupManagerDescription("gm4", 0);
        assertNull(retrievedDescription);
     
    }
    
    /**
     * 
     * 1 GM description with 10 monitoring values is inserted.
     * We retrieved this description with 2 monitoring values (timestamp 9l and 8l)
     * 
     * 
     */
    public void testGetGroupManagerDescriptionFound() 
    {
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        groupManagerDescription.setId("123456");
        groupManagerDescription.setHostname("mafalda");
        groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
        groupManagerDescription.getHeartbeatAddress().setPort(9000);
        
        groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
        
        repository_.addGroupManagerDescription(groupManagerDescription);
        
        ArrayList<Double> capacity = new ArrayList<Double>();
        capacity.add(1d);
        List<GroupManagerSummaryInformation> summaryInformationList = 
                generateGroupManagerSummaryInformation(10, capacity);
        for (GroupManagerSummaryInformation summary : summaryInformationList)
        {
            repository_.addGroupManagerSummaryInformation("123456", summary);
        }
        
        
        GroupManagerDescription retrievedDescription = repository_.getGroupManagerDescription("123456", 2);
        
        assertEquals("123456", retrievedDescription.getId());
        assertEquals("mafalda", retrievedDescription.getHostname());
        assertEquals("127.0.0.1", retrievedDescription.getHeartbeatAddress().getAddress());
        assertEquals(9000, retrievedDescription.getHeartbeatAddress().getPort());
        assertEquals("127.0.0.1", retrievedDescription.getListenSettings().getControlDataAddress().getAddress());
        assertEquals(5000, retrievedDescription.getListenSettings().getControlDataAddress().getPort());
        assertEquals("127.0.0.1", retrievedDescription.getListenSettings().getMonitoringDataAddress().getAddress());
        assertEquals(6000, retrievedDescription.getListenSettings().getMonitoringDataAddress().getPort());
        assertTrue(retrievedDescription.getIsAssigned());
        
        // size check
        assertEquals(2, retrievedDescription.getSummaryInformation().size());
        // content check
        assertEquals(capacity, retrievedDescription.getSummaryInformation().get(9L).getActiveCapacity());
        assertEquals(capacity, retrievedDescription.getSummaryInformation().get(9L).getPassiveCapacity());
        assertEquals(capacity, retrievedDescription.getSummaryInformation().get(9L).getUsedCapacity());
        assertEquals(capacity, retrievedDescription.getSummaryInformation().get(9L).getRequestedCapacity());
        assertEquals(capacity, retrievedDescription.getSummaryInformation().get(8L).getActiveCapacity());
        assertEquals(capacity, retrievedDescription.getSummaryInformation().get(8L).getPassiveCapacity());
        assertEquals(capacity, retrievedDescription.getSummaryInformation().get(8L).getUsedCapacity());
        assertEquals(capacity, retrievedDescription.getSummaryInformation().get(8L).getRequestedCapacity());
        
        // order check
        long j = 9;
        for (long i : retrievedDescription.getSummaryInformation().keySet())
        {
            assertEquals(j, i);
            j--;
        }
    }

    /**
     * 
     * Helper to generate summary information.
     * 
     * @param numberOfLogs      Number of Monitoring entries.
     * @param capacity          Capacity.
     * @return  list of summary information.
     */
    private List<GroupManagerSummaryInformation> generateGroupManagerSummaryInformation(
            int numberOfLogs, ArrayList<Double> capacity) 
    {
        List<GroupManagerSummaryInformation> summaryList = new ArrayList<GroupManagerSummaryInformation>();
        
        for (int i = 0; i < numberOfLogs; i++)
        {
            
            GroupManagerSummaryInformation groupManagerSummaryInformation = new GroupManagerSummaryInformation();
            groupManagerSummaryInformation.setTimeStamp(Long.valueOf(i));
            groupManagerSummaryInformation.setActiveCapacity(capacity);
            groupManagerSummaryInformation.setPassiveCapacity(capacity);
            groupManagerSummaryInformation.setRequestedCapacity(capacity);
            groupManagerSummaryInformation.setUsedCapacity(capacity);
            summaryList.add(groupManagerSummaryInformation);
        }
        return summaryList;
    }


    /**
     * 
     * Test Add Group Manager Summary Information.
     * 
     * 
     */
    public void testAddGroupManagerSummaryInformation() 
    {
        //add to summary
        GroupManagerSummaryInformation groupManagerSummaryInformation = new GroupManagerSummaryInformation();
        groupManagerSummaryInformation.setTimeStamp(123456789L);
        ArrayList<Double> active = new ArrayList<Double>();
        active.add(1d); active.add(1d); active.add(1d); active.add(1d);
        groupManagerSummaryInformation.setActiveCapacity(active);
        groupManagerSummaryInformation.setPassiveCapacity(active);
        groupManagerSummaryInformation.setRequestedCapacity(active);
        groupManagerSummaryInformation.setUsedCapacity(active);
        repository_.addGroupManagerSummaryInformation("12345", groupManagerSummaryInformation);
  
        
        //check the repository
        
        SliceQuery<String, Long, Object> query = 
                HFactory.createSliceQuery(
                        keyspace_,
                        StringSerializer.get(),
                        LongSerializer.get(),
                        new JsonSerializer(GroupManagerSummaryInformation.class))
                        .setKey("12345")
                        .setColumnFamily(CassandraUtils.GROUPMANAGERS_MONITORING_CF)
                        .setRange(null, null , true, 5);
        
        QueryResult<ColumnSlice<Long, Object>> columns = query.execute();
        
        GroupManagerDescription retrievedDescription = new GroupManagerDescription();
        for (HColumn<Long, Object> col : columns.get().getColumns())
        {
            GroupManagerSummaryInformation summary = (GroupManagerSummaryInformation) col.getValue();
            retrievedDescription.getSummaryInformation().put(summary.getTimeStamp(), summary);
        }
        
        assertTrue(retrievedDescription.getSummaryInformation().containsKey(123456789L));
        assertEquals(active, retrievedDescription.getSummaryInformation().get(123456789L).getActiveCapacity());
        
    }

    /**
     * Test Add ip address.
     */
    public void testAddIpAddress() 
    {
        // how to test ?
        boolean isAdded = repository_.addIpAddress("10.0.0.1");
        
        assertTrue(isAdded);
    }

    /**
     * Test remove ip address.
     */
    public void testRemoveIpAddress() 
    {
        boolean isAdded = repository_.addIpAddress("10.0.0.10");
        boolean isRemoved = repository_.removeIpAddress("10.0.0.10");
        
        assertTrue(isRemoved);
    }

    public void testGetFreeIpAddress() 
    {
        boolean isAdded = repository_.addIpAddress("10.0.0.10");
        String ip = repository_.getFreeIpAddress();
        assertNotNull(ip);
    }
    
    /**
     * Test get free ip address.
     */
    public void testGetFreeIpAddressEmpty() 
    {
        cluster_.truncate("snooze", CassandraUtils.IPSPOOL_CF);
        String ip = repository_.getFreeIpAddress();
        assertNull(ip);
    }


    /**
     * Test getAssignedGroupManager.
     * Here the GM is found
     */
    public void testGetAssignedGroupManagerAssignedAndFound() 
    {
        
        NetworkAddress contactInformation = new NetworkAddress();
        contactInformation.setAddress("10.0.0.1");
        contactInformation.setPort(5000);
        // Add some LCs
        Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
        mutator.addInsertion("098", CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("isAssigned", true, StringSerializer.get(), new BooleanSerializer())) 
               .addInsertion("098", CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createStringColumn("groupmanager", "123"));
       
        
        mutator.insert(contactInformation.toString(), CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, HFactory.createStringColumn("id", "098"));
        mutator.execute();
        
        // Add Gm
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        groupManagerDescription.setId("123");
        groupManagerDescription.setHostname("mafalda");
        groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
        groupManagerDescription.getHeartbeatAddress().setPort(9000);
        groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
        
        // Add
        repository_.addGroupManagerDescription(groupManagerDescription);
        
        AssignedGroupManager assignedGroupManager = repository_.getAssignedGroupManager(contactInformation);
        assertNotNull(assignedGroupManager);
        assertEquals("098", assignedGroupManager.getLocalControllerId());
        assertEquals("123", assignedGroupManager.getGroupManager().getId());
    }

    
    /**
     * Test getAssignedGroupManager.
     * Here the GM is found.
     */
    public void testGetAssignedGroupManagerNotAssignedNotFound() 
    {
        
        NetworkAddress contactInformation = new NetworkAddress();
        contactInformation.setAddress("10.0.0.1");
        contactInformation.setPort(5000);
        // Add some LCs
        Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
        mutator.addInsertion(contactInformation.toString(), CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createStringColumn("id", "098"))
        .addInsertion("098", CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("isAssigned", false, StringSerializer.get(), new BooleanSerializer()))
        .addInsertion("098", CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createStringColumn("groupmanager", "1234"));
        
        mutator.insert(contactInformation.toString(), CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, HFactory.createStringColumn("id", "098"));
        mutator.execute();
        // Add Gm
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        groupManagerDescription.setId("123");
        groupManagerDescription.setHostname("mafalda");
        groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
        groupManagerDescription.getHeartbeatAddress().setPort(9000);
        groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
        
        // Add
        repository_.addGroupManagerDescription(groupManagerDescription);
        
        AssignedGroupManager assignedGroupManager = repository_.getAssignedGroupManager(contactInformation);
        assertNull(assignedGroupManager);
        
    }
    
    /**
     * Test getAssignedGroupManager.
     * Here the GM is found.
     */
    public void testGetAssignedGroupManagerAssignedNotFound() 
    {
        
        NetworkAddress contactInformation = new NetworkAddress();
        contactInformation.setAddress("10.0.0.1");
        contactInformation.setPort(5000);
        // Add some LCs
        Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
        mutator.addInsertion(contactInformation.toString(), CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createStringColumn("id", "098"))
            .addInsertion("098", CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("isAssigned", true, StringSerializer.get(), new BooleanSerializer()))
            .addInsertion("098", CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createStringColumn("groupmanager", "1234"));

        mutator.insert(contactInformation.toString(), CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, HFactory.createStringColumn("id", "098"));
        mutator.execute();
        // Add Gm
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        groupManagerDescription.setId("123");
        groupManagerDescription.setHostname("mafalda");
        groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
        groupManagerDescription.getHeartbeatAddress().setPort(9000);
        groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
        
        // Add
        repository_.addGroupManagerDescription(groupManagerDescription);
        
        AssignedGroupManager assignedGroupManager = repository_.getAssignedGroupManager(contactInformation);
        
        assertNull(assignedGroupManager);
        
    }
    
    /**
     * Test getAssignedGroupManager.
     * Here the GM is found.
     */
    public void testGetAssignedGroupManagerNotAssignedFound() 
    {
        
        NetworkAddress contactInformation = new NetworkAddress();
        contactInformation.setAddress("10.0.0.1");
        contactInformation.setPort(5000);
        // Add some LCs
        Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
        mutator.addInsertion(contactInformation.toString(), CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createStringColumn("id", "098"))
        .addInsertion("098", CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("isAssigned", false, StringSerializer.get(), new BooleanSerializer()))
        .addInsertion("098", CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createStringColumn("groupmanager", "123"));
//        .addInsertion(contactInformation.toString(), LOCALCONTROLLERS_MAPPING_CF, HFactory.createStringColumn("id", "098"));
        
        mutator.insert(contactInformation.toString(), CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, HFactory.createStringColumn("id", "098"));
        mutator.execute();
        // Add Gm
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        groupManagerDescription.setId("123");
        groupManagerDescription.setHostname("mafalda");
        groupManagerDescription.getHeartbeatAddress().setAddress("127.0.0.1");
        groupManagerDescription.getHeartbeatAddress().setPort(9000);
        groupManagerDescription.getListenSettings().getControlDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getControlDataAddress().setPort(5000);
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setAddress("127.0.0.1");
        groupManagerDescription.getListenSettings().getMonitoringDataAddress().setPort(6000);
        
        // Add
        repository_.addGroupManagerDescription(groupManagerDescription);
        
        AssignedGroupManager assignedGroupManager = repository_.getAssignedGroupManager(contactInformation);
        assertNull(assignedGroupManager);
        
    }
    

    /**
     * Test generate address pool from subnet.
     */
    public void testGenerateAddressPoolOneSubnet()
    {
        String[] virtualMachineSubnets = {"192.168.122.0/30"};
        List<String> ips = repository_.generateAddressPool(virtualMachineSubnets);
        assertEquals(2, ips.size());
    }
    
    /**
     * Test generate address pool from 2 subnets.
     */
    public void testGenerateAddressPoolTwoSubnets()
    {
        String[] virtualMachineSubnets = {"192.168.122.0/22", "10.0.0.1/22"};
        List<String> ips = repository_.generateAddressPool(virtualMachineSubnets);
        assertEquals(2044, ips.size());
    }
   
   
    /**
     * Test populate address.
     */
    public void testPopulateAddressPool()
    {
        cluster_.truncate("snooze", CassandraUtils.IPSPOOL_CF);
        
        List<String> ips = new ArrayList<String>();
        ips.add("192.168.1.1");
        ips.add("192.168.1.2");
        repository_.populateAddressPool(ips);
        String ip = repository_.getFreeIpAddress();
        assertNotNull(ip);
    }

    /**
     * Test repopulate address pool.
     */
    public void testPopulateAddressPoolAlreadyPopulated()
    {
        cluster_.truncate("snooze", CassandraUtils.IPSPOOL_CF);
        
        List<String> ips = new ArrayList<String>();
        ips.add("192.168.1.1");
        repository_.populateAddressPool(ips);
        
        ips = new ArrayList<String>();
        ips.add("192.168.1.2");
        repository_.populateAddressPool(ips);
        String ip = repository_.getFreeIpAddress();
        repository_.removeIpAddress(ip);
        ip = repository_.getFreeIpAddress();
        assertNull(ip);
    }
    
    
    /**
     * 
     * 2 GM descriptions with 10 monitoring values are inserted.
     * We retrieved these descriptions with 2 monitoring values (timestamp 9l and 8l)
     * 
     * 
     */
    public void testGetGroupManagerDescriptionUnassigned() 
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
            repository_.addGroupManagerDescription(groupManagerDescription);
        }
        
        CassandraUtils.unassignNodes(keyspace_, CassandraUtils.GROUPMANAGERS_CF);
        
        ArrayList<GroupManagerDescription> groupManagers = repository_.getGroupManagerDescriptions(0);
        
        assertEquals(0, groupManagers.size());
        
        
    }
    
    
    /**
     * 
     * 2 GM descriptions with 10 monitoring values are inserted.
     * We retrieved these descriptions with 2 monitoring values (timestamp 9l and 8l)
     * 
     * 
     */
    public void testGetGroupManagerDescriptionsWithoutMonitoring() 
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
            repository_.addGroupManagerDescription(groupManagerDescription);
        }
        
    }
    
}
