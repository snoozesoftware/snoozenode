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
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.datastructure.LRUCache;
import org.inria.myriads.snoozecommon.globals.Globals;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.CassandraUtils;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.JsonSerializer;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author msimonin
 *
 */
public class TestGroupManagerCassandraRepository extends TestCase 
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(TestGroupManagerCassandraRepository.class);
    

    
    private GroupManagerCassandraRepository repository_;
    private Cluster cluster_;
    private Keyspace keyspace_;



    private HashMap<String, VirtualMachineMetaData> virtualMachines; 

    @Override
    protected void setUp() throws Exception {
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
        
        GroupManagerDescription groupManager = new GroupManagerDescription();
        groupManager.setId("1234");
        repository_ = new GroupManagerCassandraRepository(groupManager, 10, "localhost:9160");
        //add the gm to the repo (cache management)
        repository_.addGroupManagerDescriptionCassandra(groupManager, false, true);
        
        repository_.clear();
    }
    

    @Override
    protected void tearDown() throws Exception 
    {
//        repository_.clear();
    }
    
    /**
     * Add a local controller description.
     */
    public void testAddLocalControllerDescription() 
    {
        LocalControllerDescription localControllerDescription = new LocalControllerDescription();
        
        localControllerDescription.setId("9876");
        localControllerDescription.setHostname("mafalda");
        localControllerDescription.getControlDataAddress().setAddress("127.0.0.1");
        localControllerDescription.getControlDataAddress().setPort(5000);
        localControllerDescription.getTotalCapacity().add(1d);
        
        // Add
        repository_.addLocalControllerDescription(localControllerDescription);
        
        // Check the database
        HColumnFamily<String, String> columnFamily =
                new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_CF, StringSerializer.get(), StringSerializer.get());
            columnFamily.addKey("9876");
            columnFamily.addColumnName("hostname")
            .addColumnName("isAssigned")
            .addColumnName("controlDataAddress")
            .addColumnName("totalCapacity")
            .addColumnName("groupManager")
            .addColumnName("hypervisorSettings")
            .addColumnName("wakeupSettings")
          ;
        ArrayList<Double> totalCapacity = (ArrayList<Double>) columnFamily.getValue("totalCapacity", new JsonSerializer(ArrayList.class));
        boolean isAssigned = columnFamily.getValue("isAssigned", BooleanSerializer.get());
        NetworkAddress controlDataAddress = (NetworkAddress) columnFamily.getValue("controlDataAddress", new JsonSerializer(NetworkAddress.class));
        HypervisorSettings hypervisorSettings = (HypervisorSettings) columnFamily.getValue("hypervisorSettings", new JsonSerializer(HypervisorSettings.class));
        
        assertEquals("1234", columnFamily.getString("groupManager"));
        assertTrue(isAssigned);
        assertEquals(localControllerDescription.getControlDataAddress().getAddress(),controlDataAddress.getAddress());
        assertEquals(localControllerDescription.getControlDataAddress().getPort(),controlDataAddress.getPort());
        assertEquals(localControllerDescription.getTotalCapacity(), totalCapacity);
        assertEquals(localControllerDescription.getHypervisorSettings().getDriver(), hypervisorSettings.getDriver()); //bla bla
        
        
        columnFamily =
                new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, StringSerializer.get(), StringSerializer.get());
            columnFamily.addKey(controlDataAddress.toString());
            columnFamily.addColumnName("id");
            
        assertEquals("9876", columnFamily.getString("id"));
          
        
    }
    
    /**
     * Gets a Local controller description.
     */
    public void testGetLocalControllerDescriptionFound()
    {
        
        for (int i=0; i<10; i++)
        {
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId("lc" + String.valueOf(i));
            localControllerDescription.setHostname("mafalda");
            localControllerDescription.getControlDataAddress().setAddress("127.0.0.1");
            localControllerDescription.getControlDataAddress().setPort(5000);
            localControllerDescription.getTotalCapacity().add(1d);
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescription(localControllerDescription);
        }
        
        // not found
        LocalControllerDescription retrievedDescription = repository_.getLocalControllerDescription("lc5", 0, false);
        assertNotNull(retrievedDescription);
        assertEquals("lc5", retrievedDescription.getId());
        
    }
    
    /**
     * Gets a Local controller description.
     * 
     * test : not found
     */
    public void testGetLocalControllerDescriptionNotFound()
    {
        LocalControllerDescription localControllerDescription = new LocalControllerDescription();
        for (int i=0; i<10; i++)
        {
            localControllerDescription.setId("lc" + String.valueOf(i));
            localControllerDescription.setHostname("mafalda");
            localControllerDescription.getControlDataAddress().setAddress("127.0.0.1");
            localControllerDescription.getControlDataAddress().setPort(5000);
            localControllerDescription.getTotalCapacity().add(1d);
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescription(localControllerDescription);
        }
        
        // not found
        LocalControllerDescription retrievedDescription = repository_.getLocalControllerDescription("lc10", 0, false);
        assertNull(retrievedDescription);        
    }
    
    /**
     * Gets a Local controller description.
     * 
     * test : not found (empty rep)
     */
    public void testGetLocalControllerDescriptionFromEmptyRepository()
    {
        cluster_.truncate("snooze", CassandraUtils.LOCALCONTROLLERS_CF);
        LocalControllerDescription retrievedDescription = repository_.getLocalControllerDescription("9876", 0, false);
        assertNull(retrievedDescription);
    }
    
    /**
     * Gets Local controller descriptions.
     * 
     * test : 
     *       all (active + passive + ...)
     *       10 localcontrollers
     */
    public void testGetLocalControllerDescriptionsAll()
    {
        for(int i=0; i<10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescription(localControllerDescription);
        }
        
        ArrayList<LocalControllerDescription> localControllers = repository_.getLocalControllerDescriptions(0, false, false);        
        assertEquals(10,localControllers.size());
        for (LocalControllerDescription localController : localControllers)
        {
            assertTrue(localController.getIsAssigned());
        }
    }
    
    /**
     * Gets Local controller descriptions.
     * 
     * test : 
     *       all (active + passive + ...)
     *       10 localcontrollers + 2 vms each + 10 monitoring datas each
     *       5 monitoring datas requested
     */
    public void testGetLocalControllerDescriptionsAllWithVMs()
    {
        for(int i=0; i<10; i++)
        {
            String localControllerId = "lc"+String.valueOf(i);
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(localControllerId);
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            
            HashMap<String, VirtualMachineMetaData> virtualMachines = new HashMap<String, VirtualMachineMetaData>();
            
            ArrayList<AggregatedVirtualMachineData> aggregatedDatas = new ArrayList<AggregatedVirtualMachineData>();
            
            for (int j=0; j<2 ; j++)
            {
                VirtualMachineMetaData   virtualMachine = new VirtualMachineMetaData();
                String virtualMachineId =  "vm"+i+"-"+j;
                virtualMachine.getVirtualMachineLocation().setVirtualMachineId(virtualMachineId);
                virtualMachine.getVirtualMachineLocation().setGroupManagerId("gm"+String.valueOf(i));
                virtualMachine.getVirtualMachineLocation().setLocalControllerId(localControllerId);
                ArrayList<Double> requestedCapacity = new ArrayList<Double>();
                requestedCapacity.add(2d);
                virtualMachine.setRequestedCapacity(requestedCapacity);
                
                //monitoring datas
                List<VirtualMachineMonitoringData> monitoring = new ArrayList<VirtualMachineMonitoringData>();
                for (int k=0; k<10; k++)
                {
                    ArrayList<Double> usedCapacity = new ArrayList<Double>();
                    usedCapacity.add(1.5d);
                    VirtualMachineMonitoringData monitoringData = new VirtualMachineMonitoringData();
                    monitoringData.setTimeStamp(k);
                    monitoringData.setUsedCapacity(usedCapacity);
                    monitoring.add(monitoringData);
                }//monitoring
                
                AggregatedVirtualMachineData aggregatedData = new AggregatedVirtualMachineData(virtualMachineId, monitoring);
                aggregatedDatas.add(aggregatedData);

                virtualMachines.put(virtualMachineId, virtualMachine);
            }
            // add monitoring ;
            localControllerDescription.setVirtualMachineMetaData(virtualMachines);
            
            repository_.addLocalControllerDescription(localControllerDescription);
            repository_.addAggregatedMonitoringData(localControllerId, aggregatedDatas);
        }
        
        ArrayList<LocalControllerDescription> localControllers = repository_.getLocalControllerDescriptions(5, false, true);
        
        assertEquals(10,localControllers.size());
        //check vms.
        for (LocalControllerDescription localController : localControllers)
        {
            //check the size of assigned vms.
            assertEquals(2,localController.getVirtualMachineMetaData().size());
            virtualMachines = localController.getVirtualMachineMetaData();
            //check the number of retrieved monitoring entries.
            for (VirtualMachineMetaData virtualMachine : virtualMachines.values())
            {
                assertEquals(5, virtualMachine.getUsedCapacity().size());
            }
        }
        
        
    }
    
    /**
     * Gets Local controller descriptions.
     * 
     * test : 
     *       only passive
     *       10 localcontrollers : 50% passive
     */
    public void testGetLocalControllerDescriptionsOnlyPassive()
    {
        for(int i=0; i<10; i++)
        {
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.PASSIVE);
            if (i%2==0)
            {
                localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);    
            }
            
            repository_.addLocalControllerDescription(localControllerDescription);
        }
        
        ArrayList<LocalControllerDescription> localControllers = repository_.getLocalControllerDescriptions(0, true, false);
       
        assertEquals(5,localControllers.size());
    }
    
    /**
     * Gets a Local controller description.
     * Two vm assigned (test-vm) with 10 monitoring datas.
     * Gets 5 monitoring datas.
     */
    public void testGetLocalControllerDescriptionFoundWithVms()
    {
        LocalControllerDescription localControllerDescription = new LocalControllerDescription();
        
        localControllerDescription.setId("lc1");
        localControllerDescription.setHostname("mafalda");
        localControllerDescription.getControlDataAddress().setAddress("127.0.0.1");
        localControllerDescription.getControlDataAddress().setPort(5000);
        localControllerDescription.getTotalCapacity().add(1d);
        localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
        
        repository_.addLocalControllerDescription(localControllerDescription);
        
        VirtualMachineMetaData virtualMachine = new VirtualMachineMetaData();
        virtualMachine.getVirtualMachineLocation().setVirtualMachineId("test-vm");
        virtualMachine.getVirtualMachineLocation().setGroupManagerId("gm1");
        virtualMachine.getVirtualMachineLocation().setLocalControllerId("lc1");
        ArrayList<Double> requestedCapacity = new ArrayList<Double>();
        requestedCapacity.add(2d);
        virtualMachine.setRequestedCapacity(requestedCapacity);
        log_.debug("XML: "+virtualMachine.getXmlRepresentation());
        
        repository_.addVirtualMachine(virtualMachine);
        
        virtualMachine = new VirtualMachineMetaData();
        virtualMachine.getVirtualMachineLocation().setVirtualMachineId("test-vm2");
        virtualMachine.getVirtualMachineLocation().setGroupManagerId("gm1");
        virtualMachine.getVirtualMachineLocation().setLocalControllerId("lc1");
        log_.debug("XML: "+virtualMachine.getXmlRepresentation());
        
        repository_.addVirtualMachine(virtualMachine);
        
        ArrayList<VirtualMachineMonitoringData> monitoringDatas = new ArrayList<VirtualMachineMonitoringData>();
        for (int i=0 ; i<10; i++)
        {
            VirtualMachineMonitoringData monitoringData = new VirtualMachineMonitoringData();
            monitoringData.setTimeStamp(Long.valueOf(i));
            ArrayList<Double> usedCapacity = new ArrayList<Double>();
            usedCapacity.add(7d);
            monitoringData.setUsedCapacity(usedCapacity);
            monitoringDatas.add(monitoringData);
        }
        AggregatedVirtualMachineData aggregatedData = new AggregatedVirtualMachineData("test-vm", monitoringDatas);
        AggregatedVirtualMachineData aggregatedData2 = new AggregatedVirtualMachineData("test-vm2", monitoringDatas);
        
        ArrayList<AggregatedVirtualMachineData> aggregatedDatas = new ArrayList<AggregatedVirtualMachineData>();
        aggregatedDatas.add(aggregatedData);
        aggregatedDatas.add(aggregatedData2);
        
        repository_.addAggregatedMonitoringData("lc1", aggregatedDatas);
        
        
        LocalControllerDescription retrievedDescription = repository_.getLocalControllerDescription("lc1", 5, true);
        
        assertEquals("lc1", retrievedDescription.getId());
        assertEquals(localControllerDescription.getHostname(), retrievedDescription.getHostname());
        assertEquals(localControllerDescription.getTotalCapacity(),retrievedDescription.getTotalCapacity());
        assertEquals(2,retrievedDescription.getVirtualMachineMetaData().size());
        assertEquals(5,retrievedDescription.getVirtualMachineMetaData().get("test-vm").getUsedCapacity().size());
        assertEquals(1, retrievedDescription.getVirtualMachineMetaData().get("test-vm").getRequestedCapacity().size());
    }
    
    
    /**
     * Gets a Local controller description.
     * Two vm assigned (test-vm) with 10 monitoring datas.
     * Gets 0 monitoring data.
     */
    public void testGetLocalControllerDescriptionFoundWithVmsAndNoMonitoringRequested()
    {
        LocalControllerDescription localControllerDescription = new LocalControllerDescription();
        
        localControllerDescription.setId("lc1");
        localControllerDescription.setHostname("mafalda");
        localControllerDescription.getControlDataAddress().setAddress("127.0.0.1");
        localControllerDescription.getControlDataAddress().setPort(5000);
        localControllerDescription.getTotalCapacity().add(1d);
        localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
        
        repository_.addLocalControllerDescription(localControllerDescription);
        
        VirtualMachineMetaData virtualMachine = new VirtualMachineMetaData();
        virtualMachine.getVirtualMachineLocation().setVirtualMachineId("test-vm");
        virtualMachine.getVirtualMachineLocation().setGroupManagerId("gm1");
        virtualMachine.getVirtualMachineLocation().setLocalControllerId("lc1");
        log_.debug("XML: "+virtualMachine.getXmlRepresentation());
        repository_.addVirtualMachine(virtualMachine);
        
        virtualMachine = new VirtualMachineMetaData();
        virtualMachine.getVirtualMachineLocation().setVirtualMachineId("test-vm2");
        virtualMachine.getVirtualMachineLocation().setGroupManagerId("gm1");
        virtualMachine.getVirtualMachineLocation().setLocalControllerId("lc1");
        log_.debug("XML: "+virtualMachine.getXmlRepresentation());
        
        repository_.addVirtualMachine(virtualMachine);
        
        ArrayList<VirtualMachineMonitoringData> monitoringDatas = new ArrayList<VirtualMachineMonitoringData>();
        for (int i=0 ; i<10; i++)
        {
            VirtualMachineMonitoringData monitoringData = new VirtualMachineMonitoringData();
            monitoringData.setTimeStamp(Long.valueOf(i));
            ArrayList<Double> usedCapacity = new ArrayList<Double>();
            usedCapacity.add(7d);
            monitoringData.setUsedCapacity(usedCapacity);
            monitoringDatas.add(monitoringData);
        }
        AggregatedVirtualMachineData aggregatedData = new AggregatedVirtualMachineData("test-vm", monitoringDatas);
        AggregatedVirtualMachineData aggregatedData2 = new AggregatedVirtualMachineData("test-vm2", monitoringDatas);
        
        ArrayList<AggregatedVirtualMachineData> aggregatedDatas = new ArrayList<AggregatedVirtualMachineData>();
        aggregatedDatas.add(aggregatedData);
        aggregatedDatas.add(aggregatedData2);
        
        repository_.addAggregatedMonitoringData("lc1", aggregatedDatas);
        
        
        LocalControllerDescription retrievedDescription = repository_.getLocalControllerDescription("lc1", 0, true);
        
        assertEquals("lc1", retrievedDescription.getId());
        assertEquals(localControllerDescription.getHostname(), retrievedDescription.getHostname());
        assertEquals(localControllerDescription.getTotalCapacity(),retrievedDescription.getTotalCapacity());
        assertEquals(2,retrievedDescription.getVirtualMachineMetaData().size());
        assertEquals(0,retrievedDescription.getVirtualMachineMetaData().get("test-vm").getUsedCapacity().size());
        assertEquals(0,retrievedDescription.getVirtualMachineMetaData().get("test-vm2").getUsedCapacity().size());
    }
    
    
    
    /**
     * 
     * Drops Local controller.
     * 
     * test : one lc with 2 vms
     * 
     */
    public void testDropLocalControllerWithVirtualMachines()
    {

        LocalControllerDescription localControllerDescription = new LocalControllerDescription();
        
        localControllerDescription.setId("lc1");
        localControllerDescription.setHostname("mafalda");
        localControllerDescription.getControlDataAddress().setAddress("127.0.0.1");
        localControllerDescription.getControlDataAddress().setPort(5000);
        localControllerDescription.getTotalCapacity().add(1d);
        localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
        
        log_.debug("Adding lc1");
        repository_.addLocalControllerDescription(localControllerDescription);
        
        VirtualMachineMetaData virtualMachine = new VirtualMachineMetaData();
        virtualMachine.getVirtualMachineLocation().setVirtualMachineId("test-vm");
        virtualMachine.getVirtualMachineLocation().setGroupManagerId("gm1");
        virtualMachine.getVirtualMachineLocation().setLocalControllerId("lc1");
        virtualMachine.setIpAddress("10.0.0.1");
        
        log_.debug("Adding test-vm assigned to lc1");
        repository_.addVirtualMachine(virtualMachine);
        
        virtualMachine = new VirtualMachineMetaData();
        virtualMachine.getVirtualMachineLocation().setVirtualMachineId("test-vm2");
        virtualMachine.getVirtualMachineLocation().setGroupManagerId("gm1");
        virtualMachine.getVirtualMachineLocation().setLocalControllerId("lc1");
        virtualMachine.setIpAddress("10.0.0.2");
        
        log_.debug("XML: "+virtualMachine.getXmlRepresentation());
        
        repository_.addVirtualMachine(virtualMachine);
        
        repository_.dropLocalController("lc1", false);
        
        //only checks if the vms are gone
        // Check the database
        // Get the description 
        
        HColumnFamily<String, String> columnFamily =
                new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.VIRTUALMACHINES_CF, StringSerializer.get(), StringSerializer.get());
            columnFamily.addKey("test-vm");
            columnFamily.addColumnName("ipAddress");

       String ipAddress = columnFamily.getString("ipAddress");
       assertNull(ipAddress);
    }
    /**
     * Drops Local controller.
     * 
     * test : 
     *       drop 
     *       2 localcontrollers : 50% passive
     */
    public void testDropLocalControllerActiveNotForce()
    {
        for(int i=0; i<2; i++)
        {
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.PASSIVE);
            localControllerDescription.getControlDataAddress().setPort(i);
            localControllerDescription.getControlDataAddress().setAddress("10.0.0.1");
            if (i%2==0)
            {
                localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);    
            }
            repository_.addLocalControllerDescription(localControllerDescription);
        }
        
        repository_.dropLocalController("0", false);

        // Check the database
        // Get the description 
        
        HColumnFamily<String, String> columnFamily =
                new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_CF, StringSerializer.get(), StringSerializer.get());
            columnFamily.addKey("0");
            columnFamily.addColumnName("hostname");

       String hostname = columnFamily.getString("hostname");
       assertNull(hostname);
       
       //check the mapping
       HColumnFamily<String, String> mappingFamily =
               new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, StringSerializer.get(), StringSerializer.get());
       mappingFamily.addKey("10.0.0.1:0");
       mappingFamily.addColumnName("id");
  
       String id = mappingFamily.getString("id");
       assertNull(id);
    }
    
    /**
     * Drops Local controller.
     * 
     * test : 
     *       drop 
     *       2 localcontrollers : 50% passive
     */
    public void testDropLocalControllerPassiveForce()
    {
        for(int i=0; i<2; i++)
        {
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.PASSIVE);
            localControllerDescription.getControlDataAddress().setPort(i);
            localControllerDescription.getControlDataAddress().setAddress("10.0.0.1");
            if (i%2==0)
            {
                localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);    
            }
            repository_.addLocalControllerDescription(localControllerDescription);
        }
        
        repository_.dropLocalController("1", true);

        // Check the database
        // Get the description 
        
        HColumnFamily<String, String> columnFamily =
                new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_CF, StringSerializer.get(), StringSerializer.get());
            columnFamily.addKey("1");
            columnFamily.addColumnName("hostname");

       String hostname = columnFamily.getString("hostname");
       assertNull(hostname);
       
       //check the mapping
       HColumnFamily<String, String> mappingFamily =
               new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, StringSerializer.get(), StringSerializer.get());
       mappingFamily.addKey("10.0.0.1:1");
       mappingFamily.addColumnName("id");
  
       String id = mappingFamily.getString("id");
       assertNull(id);
    }
    
    
    /**
     * Drops Local controller.
     * 
     * test : 
     *       drop 
     *       2 localcontrollers : 50% passive
     */
    public void testDropLocalControllerActiveForce()
    {
        for(int i=0; i<2; i++)
        {
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.PASSIVE);
            localControllerDescription.getControlDataAddress().setPort(i);
            localControllerDescription.getControlDataAddress().setAddress("10.0.0.1");
            if (i%2==0)
            {
                localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);    
            }
            repository_.addLocalControllerDescription(localControllerDescription);
        }
        
        repository_.dropLocalController("0", true);

        // Check the database
        HColumnFamily<String, String> columnFamily =
                new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_CF, StringSerializer.get(), StringSerializer.get());
        columnFamily.addKey("0");
        columnFamily.addColumnName("hostname");

       String hostname = columnFamily.getString("hostname");
       assertNull(hostname);
       
       //check the mapping
       HColumnFamily<String, String> mappingFamily =
               new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, StringSerializer.get(), StringSerializer.get());
       mappingFamily.addKey("10.0.0.1:0");
       mappingFamily.addColumnName("id");
  
       String id = mappingFamily.getString("id");
       assertNull(id);
    }
    
    
    /**
     * Drops Local controller.
     * 
     * test : 
     *       drop 
     *       2 localcontrollers : 50% passive
     */
    public void testDropLocalControllerPassiveNotForce()
    {
        for(int i=0; i<2; i++)
        {
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.PASSIVE);
            localControllerDescription.getControlDataAddress().setPort(i);
            localControllerDescription.getControlDataAddress().setAddress("10.0.0.1");
            if (i%2==0)
            {
                localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);    
            }
            repository_.addLocalControllerDescription(localControllerDescription);
        }
        
        repository_.dropLocalController("1", false);

        // Check the database
        // Get the description 
        
        HColumnFamily<String, String> columnFamily =
                new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_CF, StringSerializer.get(), StringSerializer.get());
            columnFamily.addKey("1");
            columnFamily.addColumnName("hostname");

       String hostname = columnFamily.getString("hostname");
       assertNotNull(hostname);
       
     //check the mapping
       HColumnFamily<String, String> mappingFamily =
               new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, StringSerializer.get(), StringSerializer.get());
       mappingFamily.addKey("10.0.0.1:1");
       mappingFamily.addColumnName("id");
  
       String id = mappingFamily.getString("id");
       assertNotNull(id);
       
    }
    
    public void testChangeLocalControllerStatus()
    {
        LocalControllerDescription localControllerDescription = new LocalControllerDescription();
        
        localControllerDescription.setId(String.valueOf(0));
        localControllerDescription.setStatus(LocalControllerStatus.PASSIVE);
        localControllerDescription.getControlDataAddress().setPort(0);
        localControllerDescription.getControlDataAddress().setAddress("10.0.0.1");
        
        repository_.addLocalControllerDescription(localControllerDescription);
        repository_.changeLocalControllerStatus("0", LocalControllerStatus.ACTIVE);
        LocalControllerDescription retrievedDescription = repository_.getLocalControllerDescription("0", 0, false);
        
        assertEquals(LocalControllerStatus.ACTIVE, retrievedDescription.getStatus());
        
    }
    
    
    /**
     * Adds a virtual machine.
     */
    public void testAddVirtualMachine()
    {
        VirtualMachineMetaData virtualMachine = new VirtualMachineMetaData();
        virtualMachine.getVirtualMachineLocation().setVirtualMachineId("test-vm");
        virtualMachine.getVirtualMachineLocation().setGroupManagerId("gm1");
        virtualMachine.getVirtualMachineLocation().setLocalControllerId("lc1");
        repository_.addVirtualMachine(virtualMachine);
        
        
     // Check the database
        HColumnFamily<String, String> columnFamily =
                new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.VIRTUALMACHINES_CF, StringSerializer.get(), StringSerializer.get());
            columnFamily.addKey("test-vm");
            columnFamily.addColumnName("ipAddress")
            .addColumnName("xmlRepresentation")
            .addColumnName("status")
            .addColumnName("errorCode")
            .addColumnName("groupManager")
            .addColumnName("localController");
           
       
       String ipAddress = columnFamily.getString("ipAddress");
       assertEquals(Globals.DEFAULT_INITIALIZATION, ipAddress);
       
       String xmlRepresentation = columnFamily.getString("xmlRepresentation");
       assertEquals(Globals.DEFAULT_INITIALIZATION, xmlRepresentation);
       
       VirtualMachineStatus status = VirtualMachineStatus.valueOf(columnFamily.getString("status"));
       assertEquals(VirtualMachineStatus.UNKNOWN, status);
       
       VirtualMachineErrorCode errorCode = VirtualMachineErrorCode.valueOf(columnFamily.getString("errorCode"));
       assertEquals(VirtualMachineErrorCode.UNKNOWN, errorCode);
       
       String groupManager = columnFamily.getString("groupManager");
       assertEquals("gm1", groupManager);
       
       String localController = columnFamily.getString("localController");
       assertEquals("lc1", localController);
       
    }
    
    
    public void testAddAggregatedMonitoringData ()
    {
        LocalControllerDescription localControllerDescription = new LocalControllerDescription();
        
        localControllerDescription.setId("lc1");
        localControllerDescription.setHostname("mafalda");
        localControllerDescription.getControlDataAddress().setAddress("127.0.0.1");
        localControllerDescription.getControlDataAddress().setPort(5000);
        localControllerDescription.getTotalCapacity().add(1d);
        
        // Add
        repository_.addLocalControllerDescription(localControllerDescription);
        
        
        ArrayList<VirtualMachineMonitoringData> monitoringDatas = new ArrayList<VirtualMachineMonitoringData>();
        for (int i=0 ; i<10; i++)
        {
            VirtualMachineMonitoringData monitoringData = new VirtualMachineMonitoringData();
            monitoringData.setTimeStamp(Long.valueOf(i));
            ArrayList<Double> usedCapacity = new ArrayList<Double>();
            usedCapacity.add(7d);
            monitoringData.setUsedCapacity(usedCapacity);
            monitoringDatas.add(monitoringData);
        }
        AggregatedVirtualMachineData aggregatedData = new AggregatedVirtualMachineData("test-vm", monitoringDatas);
        
        ArrayList<AggregatedVirtualMachineData> aggregatedDatas = new ArrayList<AggregatedVirtualMachineData>();
        aggregatedDatas.add(aggregatedData);
        
        repository_.addAggregatedMonitoringData("lc1", aggregatedDatas);
        
        // check the repository
        
        SliceQuery<String, Long, Object> query = HFactory.createSliceQuery(keyspace_, StringSerializer.get(),
                LongSerializer.get(), new JsonSerializer(VirtualMachineMonitoringData.class)).
                setKey("test-vm")
                .setColumnFamily(CassandraUtils.VIRTUALMACHINES_MONITORING_CF)
                .setRange(null, null , true, 10);
        
        QueryResult<ColumnSlice<Long, Object>> columns = query.execute();
        
        // artificial meta data just to get helper methods. 
        VirtualMachineMetaData retrievedDescription = new VirtualMachineMetaData();
        for (HColumn<Long, Object> col : columns.get().getColumns())
        {
            VirtualMachineMonitoringData summary = (VirtualMachineMonitoringData) col.getValue() ;
            retrievedDescription.getUsedCapacity().put(summary.getTimeStamp(), summary);
        }
        
        assertNotNull(retrievedDescription.getUsedCapacity());
        assertEquals(10, retrievedDescription.getUsedCapacity().size());
        for (int i=0; i<10; i++)
        {
            assertTrue(retrievedDescription.getUsedCapacity().containsKey(Long.valueOf(i)));
        }
    }
    
    /**
     * Gets Local controller descriptions.
     * 
     * test : 
     *       all (active + passive + ...)
     *       10 localcontrollers unassigned
     */
    public void testGetLocalControllerDescriptionsUnassigned()
    {
        for(int i=0; i<10; i++)
        {
            
            LocalControllerDescription localControllerDescription = new LocalControllerDescription();
            localControllerDescription.setId(String.valueOf(i));
            localControllerDescription.setStatus(LocalControllerStatus.ACTIVE);
            repository_.addLocalControllerDescription(localControllerDescription);
        }
        
        CassandraUtils.unassignNodes(keyspace_, CassandraUtils.LOCALCONTROLLERS_CF);
        ArrayList<LocalControllerDescription> localControllers = repository_.getLocalControllerDescriptions(0, false, false);
        
        assertEquals(0,localControllers.size());
    }
    
}
