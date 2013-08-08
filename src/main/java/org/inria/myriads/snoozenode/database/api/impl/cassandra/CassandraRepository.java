package org.inria.myriads.snoozenode.database.api.impl.cassandra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.ListenSettings;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.localcontroller.hypervisor.HypervisorSettings;
import org.inria.myriads.snoozecommon.communication.localcontroller.wakeup.WakeupSettings;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.CassandraUtils;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.JsonSerializer;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.RowQueryIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraRepository
{
    /** Logger. */
    protected static final Logger log_ = LoggerFactory.getLogger(CassandraRepository.class);
    
    /** Cassandra Cluster. */
    protected Cluster cluster_;
    
    /** Cassandra Keyspace.*/
    protected static Keyspace keyspace_;

    /**
     * Constructor. 
     */
    public CassandraRepository(String hosts)
    {
        cluster_ = HFactory.getOrCreateCluster(CassandraUtils.CLUSTER,new CassandraHostConfigurator(hosts));
        keyspace_ = HFactory.createKeyspace(CassandraUtils.KEYSPACE, cluster_);
    }

    
    
    protected GroupManagerDescription getGroupManagerDescriptionCassandra(String groupManagerId, 
            int numberOfbacklogEntries, 
            boolean withLocalControllers,
            boolean isActiveOnly,
            boolean withVirtualMachines,
            int numberOfMonitoringEntries
            )
    {
        GroupManagerDescription groupManagerDescription = getGroupManagerDescriptionOnly(groupManagerId, numberOfbacklogEntries);
        if (groupManagerDescription==null)
        {
            return null;
        }
        
        if (withLocalControllers)
        {
            fillWithLocalControllers(groupManagerDescription, isActiveOnly, numberOfMonitoringEntries);
        }
        
        if (withVirtualMachines)
        {
            fillWithVirtualMachines(groupManagerDescription, numberOfMonitoringEntries);
        }
        
        return groupManagerDescription;
    }
    
    protected LocalControllerDescription getLocalControllerDescriptionCassandra(
            String localControllerId,
            int numberOfHostMonitoringEntries,
            boolean withVirtualMachines,
            int numberOfMonitoringEntries
            )
    {
        LocalControllerDescription localController = getLocalControllerDescriptionOnly(localControllerId, numberOfMonitoringEntries);
        if (localController == null)
        {
            return null;
        }
        
        if (withVirtualMachines)
        {
            fillWithVirtualMachines(localController, numberOfMonitoringEntries);
        }
        
        return localController ; 
    }
    
    /**
     * TODO call to getLocalControllerDescriptionSonly
     * 
     * @param localControllerId
     * @param numberOfMonitoringEntries
     * @return
     */
    protected LocalControllerDescription getLocalControllerDescriptionOnly(String localControllerId, int numberOfMonitoringEntries)
    {
        Guard.check(localControllerId, numberOfMonitoringEntries);       
        log_.debug(String.format("Getting local controller description for %s", localControllerId));
        try
        {
            
            
            RowQueryIterator rowQueryIterator = new RowQueryIterator(
                    keyspace_, CassandraUtils.LOCALCONTROLLERS_CF,
                    localControllerId,
                    localControllerId,
                    1);  
            
            @SuppressWarnings("unchecked")
            Iterator<Row<String, String, String>> rowsIterator = rowQueryIterator.iterator();
            if (!rowsIterator.hasNext())
            {
                log_.debug("NOT FOUND");
                return null;
            }
            
            Row<String, String, String> row = (Row<String, String, String>) rowsIterator.next();
            log_.debug("found matching row with id " + row.getKey());
            LocalControllerDescription retrievedDescription  ;

            if ( ! row.getKey().equals(localControllerId))
            {
                return null;
            }
            
            retrievedDescription = getLocalControllerDescription(row, false);
                
            return retrievedDescription;
        }
        catch(Exception exception)
        {
            log_.error("Error while getting localcontroller description" + exception.getMessage()); 
            return null;    
        }
    }
        
    /**
     * 
     * Gets local controller from a cassandra row.
     * 
     * @param row
     * @param numberOfMonitoringEntries
     * @param isActiveOnly
     * @param withVirtualMachines
     * @return
     */
    protected LocalControllerDescription getLocalControllerDescription(Row<String, String, String> row,  boolean isActiveOnly)
    {
        log_.debug("Deserialize row from cassandra cluster from row id" + row.getKey());
        LocalControllerDescription localControllerDescription = new LocalControllerDescription();
        
        ColumnSlice<String, String> columns = row.getColumnSlice();
 
        LocalControllerStatus status = LocalControllerStatus.valueOf(columns.getColumnByName("status").getValue());
        if (status != LocalControllerStatus.ACTIVE && isActiveOnly)
        {
            return null;
        }
        String hostname = columns.getColumnByName("hostname").getValue();
        
        JsonSerializer hypervisorSettingsSerializer = new JsonSerializer(HypervisorSettings.class);
        HypervisorSettings hypervisorSettings = (HypervisorSettings) hypervisorSettingsSerializer.fromString(columns.getColumnByName("hypervisorSettings").getValue());
        
        JsonSerializer totalCapacitySerializer = new JsonSerializer(ArrayList.class);
        @SuppressWarnings("unchecked")
        ArrayList<Double> totalCapacity = (ArrayList<Double>) totalCapacitySerializer.fromString(columns.getColumnByName("totalCapacity").getValue());
        
        JsonSerializer controlDataAddressSerializer = new JsonSerializer(NetworkAddress.class);
        NetworkAddress controlDataAddress = (NetworkAddress) controlDataAddressSerializer.fromString(columns.getColumnByName("controlDataAddress").getValue());
        
        JsonSerializer wakeupSettingsSerializer = new JsonSerializer(WakeupSettings.class);
        WakeupSettings wakeupSettings = (WakeupSettings) wakeupSettingsSerializer.fromString(columns.getColumnByName("wakeupSettings").getValue());
        
        
        
        localControllerDescription.setId(row.getKey());
        localControllerDescription.setHostname(hostname);
        localControllerDescription.setHypervisorSettings(hypervisorSettings);
        localControllerDescription.setTotalCapacity(totalCapacity);
        localControllerDescription.setControlDataAddress(controlDataAddress);
        localControllerDescription.setWakeupSettings(wakeupSettings);
        localControllerDescription.setStatus(status);
        
        return localControllerDescription;
        
    }
    /**
     * 
     * Gets the groupManager description.
     * 
     * @param groupManagerId
     * @param numberOfBacklogEntries
     * @return
     */
    protected GroupManagerDescription getGroupManagerDescriptionOnly(String groupManagerId, int numberOfBacklogEntries) 
    {
        Guard.check(groupManagerId, numberOfBacklogEntries);
        try{
            log_.debug(String.format("Gets groupmanager description %s",groupManagerId));
            RowQueryIterator rowQueryIterator = new RowQueryIterator(
                    keyspace_, CassandraUtils.GROUPMANAGERS_CF,
                    groupManagerId, // start
                    groupManagerId, // end
                    1); // rows to fecth 
            
            @SuppressWarnings("unchecked")
            Iterator<Row<String, String, String>> rowsIterator = rowQueryIterator.iterator();
            if (!rowsIterator.hasNext())
            {
                log_.debug("NOT FOUND");
                return null;
            }
            
            Row<String, String, String> row = (Row<String, String, String>) rowsIterator.next();
            log_.debug("found matching row with id" + row.getKey());
            GroupManagerDescription retrievedDescription  ;

            // mandatory !
            if ( ! row.getKey().equals(groupManagerId))
            {
                return null;
            }
            
            retrievedDescription = getGroupManagerDescription(row);        
            // Now get the monitoring datas...
            if (numberOfBacklogEntries > 0)
            {
                fillGroupManagerSummaryInformation(retrievedDescription, numberOfBacklogEntries);
            }
            log_.debug("Returning the groupManagerDescription");
            return retrievedDescription;
        }
        catch(Exception e)
        {
            log_.debug(e.getMessage());
            e.printStackTrace();

        }
        return null;
    }
    
    
    /**
     * 
     * Fills group manager description with summary information.
     * 
     * @param groupManagerDescription
     * @param numberOfBacklogEntries
     */
    protected void fillGroupManagerSummaryInformation(GroupManagerDescription groupManagerDescription, int numberOfBacklogEntries) 
    {
        log_.debug("Gets the monitoring datas from the cassandra cluster");
        SliceQuery<String, Long, Object> query = HFactory.createSliceQuery(keyspace_, StringSerializer.get(),
                LongSerializer.get(), new JsonSerializer(GroupManagerSummaryInformation.class))
                .setKey(groupManagerDescription.getId()).setColumnFamily(CassandraUtils.GROUPMANAGERS_MONITORING_CF)
                .setRange(null, null , true, numberOfBacklogEntries);
        
        QueryResult<ColumnSlice<Long, Object>> columns = query.execute();
        
        for (HColumn<Long, Object> col : columns.get().getColumns())
        {
            GroupManagerSummaryInformation summary = (GroupManagerSummaryInformation) col.getValue() ;
            groupManagerDescription.getSummaryInformation().put(summary.getTimeStamp(), summary);
            log_.debug("gets monitoring data for timestamp" + summary.getTimeStamp());
        }
    }
    
    /**
     * 
     * Gets the groupManagerDescription from a row.
     * 
     * @param row
     * @return
     */
    protected GroupManagerDescription getGroupManagerDescription(Row<String, String, String> row) 
    {
        log_.debug("Deserialize row from cassandra cluster from row id" + row.getKey());
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        ColumnSlice<String, String> columns = row.getColumnSlice();
        
//        String isAssigned = columns.getColumnByName("isAssigned").getValue();
//        if (isAssigned.equals(CassandraUtils.stringFalse))
//        {
//            return null;
//        }
        String hostname = columns.getColumnByName("hostname").getValue();
        
        JsonSerializer heartbeatAddressSerializer = new JsonSerializer(NetworkAddress.class);
        NetworkAddress heartbeatAddress = (NetworkAddress) heartbeatAddressSerializer.fromString(columns.getColumnByName("heartbeatAddress").getValue());
        
        JsonSerializer listenSettingsSerializer = new JsonSerializer(ListenSettings.class);
        ListenSettings listenSettings = (ListenSettings) listenSettingsSerializer.fromString(columns.getColumnByName("listenSettings").getValue());
        
        groupManagerDescription.setId(row.getKey());
        groupManagerDescription.setHostname(hostname);
        groupManagerDescription.setHeartbeatAddress(heartbeatAddress);  
        groupManagerDescription.setListenSettings(listenSettings);
        
        return groupManagerDescription;
    }
    /**
     * 
     * Fill the group manager description with the associated localcontrollers.
     * 
     * @param groupManager                  The group manager description to fill
     * @param numberOfMonitoringEntries     The number of monitoring entries
     */
    protected void fillWithLocalControllers(GroupManagerDescription groupManager, boolean isActiveOnly, int numberOfMonitoringEntries)
    {
        // Gets All Local Controllers
        HashMap<String, LocalControllerDescription> localControllers = 
                getLocalControllerDescriptionsOnly(groupManager.getId(),  null ,-1, numberOfMonitoringEntries, isActiveOnly, true);
        
        groupManager.setLocalControllers(localControllers);
        
    }
    
    /**
     * 
     * Fills the hashmap with virtualmachines.
     * 
     * @param groupManagerId
     * @param localControllers
     * @param numberOfMonitoringEntries
     */
    protected void fillWithVirtualMachines(String groupManagerId, HashMap<String,LocalControllerDescription> localControllers, int numberOfMonitoringEntries)
    {
        if (localControllers.size()==0)
        {
            log_.debug("No Local controllers assigned to this group manager");
            return;
        }
        
        HashMap<String, VirtualMachineMetaData> virtualMachines = getVirtualMachines("groupManager", groupManagerId, numberOfMonitoringEntries);
        for (VirtualMachineMetaData virtualMachine : virtualMachines.values())
        {
            String localControllerId = virtualMachine.getVirtualMachineLocation().getLocalControllerId();
            String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            if (!localControllers.containsKey(localControllerId))
            {
                log_.error(String.format("virtual machine %s is assigned to groupmanager %s which doesn't manage localcontroller %s", 
                        virtualMachineId,
                        groupManagerId,
                        localControllerId
                        ));
                continue;
            }
            log_.debug(String.format("Add virtual machine %s to group manager %s description", virtualMachineId, groupManagerId));
            localControllers.get(localControllerId).getVirtualMachineMetaData().put(virtualMachineId, virtualMachine);
        }
    }
    ;
    
    /**
     * 
     * Fills the group manager description with the associated virtual machines.
     * 
     * 
     * @param groupManager
     * @param numberOfMonitoringEntries
     */
    protected void fillWithVirtualMachines(GroupManagerDescription groupManager, int numberOfMonitoringEntries)
    {
       fillWithVirtualMachines(groupManager.getId(), groupManager.getLocalControllers(), numberOfMonitoringEntries);
    }
    
    /**
     * 
     * Fills the group manager description with the associated virtual machines.
     * 
     * 
     * @param groupManager
     * @param numberOfMonitoringEntries
     */
    protected void fillWithVirtualMachines(LocalControllerDescription localController, int numberOfMonitoringEntries)
    {
       
       
       String localControllerId = localController.getId();
       
       HashMap<String, VirtualMachineMetaData> virtualMachines = getVirtualMachines("localController", localControllerId, numberOfMonitoringEntries);
       
       for (Entry<String, VirtualMachineMetaData> entry: virtualMachines.entrySet())
       {
           String virtualMachineId = entry.getKey();
           VirtualMachineMetaData virtualMachine = entry.getValue();
           log_.debug(String.format("Add virtual machine %s to localController %s description", virtualMachineId, localControllerId));
           localController.getVirtualMachineMetaData().put(virtualMachineId, virtualMachine);
       }
    }
    
    protected ArrayList<LocalControllerDescription> getLocalControllerDescriptionsCassandra(String groupManagerId,
            int numberOfMonitoringEntries, boolean isActiveOnly, boolean withVirtualMachines)
    {
        HashMap<String, LocalControllerDescription> localControllerDescriptions = getLocalControllerDescriptionsOnly(
                groupManagerId,
                null,
                -1,
                numberOfMonitoringEntries,
                isActiveOnly,
                true
                );
        
        if (withVirtualMachines)
        {
            fillWithVirtualMachines(groupManagerId, localControllerDescriptions, numberOfMonitoringEntries);
        }
        
       ArrayList<LocalControllerDescription> localControllers = new ArrayList<LocalControllerDescription>();
       localControllers.addAll(localControllerDescriptions.values());
       
       return localControllers;
    }
    
    /**
     * 
     * Returns the local controller descriptions of a given group manager.
     * 
     * @param groupManagerId                The group manager Id
     * @param localControllerStart          The local Controller start key (for range)
     * @param limit                         The total number to fetch
     * @param numberOfMonitoringEntries     Number of monitoring entries to fecth
     * @param isActiveOnly                  Only gets ACTIVE localController
     * @param onlyAssigned                  Only gets Assigned localcontroller
     * @return
     */
    protected HashMap<String, LocalControllerDescription> getLocalControllerDescriptionsOnly(
            String groupManagerId, 
            String localControllerStart,
            int limit,
            int numberOfMonitoringEntries,
            boolean isActiveOnly,
            boolean onlyAssigned
            )
    {
        String lastKey = localControllerStart;        
        boolean unlimited = false;
        int rowCount = 2*limit;
        int toLimit = limit;
        if (limit<=0)
        {
            log_.debug("Gets All the groupmanagers") ;
            unlimited = true;
            rowCount = 100;
        }
       
        boolean terminated = false;
        int iteration = 0; 
        HashMap<String, LocalControllerDescription> localControllers = new HashMap<String, LocalControllerDescription>();
        
        while(true)
        {
            
            QueryResult<OrderedRows<String, String, String>> result = null;
            if ((groupManagerId == null || groupManagerId.equals("")) && !isActiveOnly && !onlyAssigned)
            {

                log_.debug("Ranged query to fetch the localcontrollers");
                RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory
                        .createRangeSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())
                        .setColumnFamily(CassandraUtils.LOCALCONTROLLERS_CF)
                        .setKeys(lastKey, null)
                        .setRange(null, null, false, 100)
                        .setRowCount(rowCount);
                
                 result = rangeSlicesQuery.execute();
            }
            else
            {
                log_.debug("Indexed query to fecth the localcontrollers");
                IndexedSlicesQuery<String, String, String> indexedSlicesQuery =  HFactory.createIndexedSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
                indexedSlicesQuery.setColumnFamily(CassandraUtils.LOCALCONTROLLERS_CF);
                indexedSlicesQuery.setStartKey(lastKey);
                indexedSlicesQuery.setRange(null, null, false, 100);
                
                if (groupManagerId != null && !groupManagerId.equals(""))
                {
                    indexedSlicesQuery.addEqualsExpression("groupManager", groupManagerId);
                }
                if (onlyAssigned)
                {
                    indexedSlicesQuery.addEqualsExpression("isAssigned", CassandraUtils.stringTrue);
                }
                
                if (isActiveOnly)
                {
                    indexedSlicesQuery.addEqualsExpression("status", String.valueOf(LocalControllerStatus.ACTIVE));
                }
                
                result = indexedSlicesQuery.execute();
                
            }
            
            OrderedRows<String, String, String> rows = result.get() ;
            
            Iterator<Row<String, String, String>> rowsIterator = rows.iterator();
            

            if (!rowsIterator.hasNext())
            {
                log_.debug(String.format("Group manager %s has no local controller", groupManagerId));
                
                return localControllers;
            }
            
            //go one step further (lastKey has been already fetched in the previous iteration)
            if (iteration > 0 && rowsIterator != null) rowsIterator.next();
            
            while(rowsIterator.hasNext() & !terminated)
            {
                
                Row<String, String, String> row = rowsIterator.next();
                lastKey = row.getKey();
                log_.debug("LastKey = " + lastKey);
                
                if (row.getColumnSlice().getColumns().isEmpty()) 
                {
                    //skip tombstone
                    continue;
                }
                
                LocalControllerDescription retrievedDescription;
                retrievedDescription = getLocalControllerDescription(row, isActiveOnly);
                if (retrievedDescription != null)
                {
                    localControllers.put(row.getKey(), retrievedDescription);
                    toLimit -- ;
                    if (!unlimited &&  toLimit <=0)
                    {
                        terminated = true;
                    }
                }
            }
            // no more local controller to fetch
            if (terminated)
            {
                log_.debug("Got all the localcontrollers");
                break;
            }
            
            // we cannot fetch more
            if (rows.getCount() < rowCount  )
            {
                log_.debug("No more groupmanager to fetch");
                break;
            }
            
            iteration++;
        }
        return localControllers;  
    }
    
    /**
     * 
     * Gets the virtual machines associated to a given group manager or local controller.
     * 
     * 
     * @param hostId
     * @param numberOfMonitoringEntries
     * @return
     */
    protected HashMap<String, VirtualMachineMetaData> getVirtualMachines(String index, String hostId,
            int numberOfMonitoringEntries)
    {
        log_.debug(String.format("Getting all virtual machine for host %s ", hostId));
        HashMap<String, VirtualMachineMetaData> virtualMachines = new HashMap<String, VirtualMachineMetaData>();
        
        // index query on virtualmachines.
        IndexedSlicesQuery<String, String, String> indexedSlicesQuery =  HFactory.createIndexedSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
        indexedSlicesQuery.setColumnFamily(CassandraUtils.VIRTUALMACHINES_CF);
        indexedSlicesQuery.setRange(null, null, false, 100);
        indexedSlicesQuery.addEqualsExpression(index, hostId);
        
        QueryResult<OrderedRows<String, String, String>> queryResult = indexedSlicesQuery.execute();
        OrderedRows<String, String, String> rows = queryResult.get() ;
        
        Iterator<Row<String, String, String>> rowsIterator = rows.iterator();
        if (!rowsIterator.hasNext())
        {
            log_.debug("no virtual machines found for group manager " +  hostId);
            return virtualMachines;
        }
        while(rowsIterator.hasNext())
        {
            Row<String, String, String> row = (Row<String, String, String>) rowsIterator.next();
            log_.debug("found matching row with id " + row.getKey());
            
            
            VirtualMachineMetaData retrievedVirtualMachine ;
         
            retrievedVirtualMachine = getVirtualMachineMetaData(row);
            
            if (numberOfMonitoringEntries > 0)
            {
                fillVirtualMachineMonitoringData(retrievedVirtualMachine, numberOfMonitoringEntries);
            }
            log_.debug("Adding vm monitoring data");
            virtualMachines.put(row.getKey(), retrievedVirtualMachine);
        }
        return virtualMachines;
    }
    
    /**
     * 
     * Fills the virtual machine meta data with monitoring datas.
     * 
     * @param virtualMachine
     * @param numberOfMonitoringEntries
     */
    protected void fillVirtualMachineMonitoringData(VirtualMachineMetaData virtualMachine,
            int numberOfMonitoringEntries)
    {
        log_.debug("Gets the monitoring datas from the cassandra cluster");
        SliceQuery<String, Long, Object> query = HFactory.createSliceQuery(keyspace_, StringSerializer.get(),
                LongSerializer.get(), new JsonSerializer(VirtualMachineMonitoringData.class))
                .setKey(virtualMachine.getVirtualMachineLocation().getVirtualMachineId())
                .setColumnFamily(CassandraUtils.VIRTUALMACHINES_MONITORING_CF)
                .setRange(null, null , true, numberOfMonitoringEntries);
        
        QueryResult<ColumnSlice<Long, Object>> columns = query.execute();
        
        for (HColumn<Long, Object> col : columns.get().getColumns())
        {
            VirtualMachineMonitoringData monitoring = (VirtualMachineMonitoringData) col.getValue() ;
            virtualMachine.getUsedCapacity().put(monitoring.getTimeStamp(), monitoring);
            log_.debug("gets monitoring data for timestamp " + monitoring.getTimeStamp());
        }
    }
    /**
     * 
     * Gets the virtual machine meta data from a cassandra row.
     * 
     * @param row
     * @return
     */
    protected VirtualMachineMetaData getVirtualMachineMetaData(Row<String, String, String> row)
    {
        log_.debug("Deserialize virtual machine row from cassandra cluster from rwo id" + row.getKey());
        VirtualMachineMetaData virtualMachineMetaData = new VirtualMachineMetaData();
        ColumnSlice<String, String> columns = row.getColumnSlice();
        
        String ipAddress = columns.getColumnByName("ipAddress").getValue();
        String xmlRepresentation = columns.getColumnByName("xmlRepresentation").getValue();
        VirtualMachineStatus status = VirtualMachineStatus.valueOf(columns.getColumnByName("status").getValue());
        VirtualMachineErrorCode errorCode = VirtualMachineErrorCode.valueOf(columns.getColumnByName("errorCode").getValue());
        JsonSerializer locationSerializer = new JsonSerializer(VirtualMachineLocation.class);
        VirtualMachineLocation location = (VirtualMachineLocation) locationSerializer.fromString((columns.getColumnByName("location").getValue()));
        JsonSerializer requestedCapacitySerializer = new JsonSerializer(ArrayList.class);
        @SuppressWarnings("unchecked")
        ArrayList<Double> requestedCapacity = (ArrayList<Double>) requestedCapacitySerializer.fromString(columns.getColumnByName("requestedCapacity").getValue());
        
        virtualMachineMetaData.setIpAddress(ipAddress);
        virtualMachineMetaData.setXmlRepresentation(xmlRepresentation);
        virtualMachineMetaData.setStatus(status);
        virtualMachineMetaData.setErrorCode(errorCode);
        virtualMachineMetaData.setVirtualMachineLocation(location);
        virtualMachineMetaData.setRequestedCapacity(requestedCapacity);
        
        log_.debug("Returning the deserialized metadata");
        return virtualMachineMetaData;
    }
    
    
    
    
    
    /**
     * 
     * Drop a group manager from the cassandra cluster.
     * 
     * @param groupManagerId            The group manager id to drop
     * @param withLocalControllers      True if assigned LocalController must be dropped
     * @param withVirtualMachines       True if assigned virtualMachines must be dropped
     * @return
     */
    protected boolean dropGroupManager(String groupManagerId, boolean withLocalControllers, boolean withVirtualMachines)
    {
        GroupManagerDescription groupManager = getGroupManagerDescriptionCassandra(
                groupManagerId,
                0,
                true, // with lc
                true, // active only (passive local controller must be reassigned)
                true, // with vms
                0);
        
        if (groupManager == null)
        {
            log_.debug("Unable to find the group manager " + groupManagerId);
            return false;
        }
        boolean isGroupManagerDropped = CassandraUtils.drop(keyspace_, Arrays.asList(groupManager.getId()), CassandraUtils.GROUPMANAGERS_CF);
        if (! isGroupManagerDropped)
        {
            log_.error("Unable to remove the groupmanager " + groupManagerId);
            return false;
        }
        
        if (withLocalControllers)
        {
            List<String> localControllerToRemove = new ArrayList<String>();
            List<String> virtualMachineToRemove = new ArrayList<String>();
            List<String> mappingToRemove = new ArrayList<String>();
            HashMap<String, LocalControllerDescription> localControllers = groupManager.getLocalControllers();
            for(LocalControllerDescription localController : localControllers.values())
            {
                localControllerToRemove.add(localController.getId());
                mappingToRemove.add(localController.getControlDataAddress().toString());
                if (withVirtualMachines)
                {
                    virtualMachineToRemove.addAll(localController.getVirtualMachineMetaData().keySet());
                }

            }
          //boolean isLocalControllerDropped = drop(localControllerToRemove, CassandraUtils.LOCALCONTROLLERS_CF);
          boolean isLocalControllerDropped = CassandraUtils.unassignNodes(keyspace_, localControllerToRemove, CassandraUtils.LOCALCONTROLLERS_CF);
          if (! isLocalControllerDropped)
          {
              log_.error("Unable to remove the assigned localcontrollers");
              return false;
          }
          
          // should we remove the mapping  ?
//          boolean isMappingDropped = drop(mappingToRemove, CassandraUtils.LOCALCONTROLLERS_MAPPING_CF);
//          if (!isMappingDropped)
//          {
//              log_.error("Unable to remove the mapping for the assigned localcontroller");
//              return false;
//          }
          
          //boolean isVirtualMachineDropped = drop(virtualMachineToRemove, CassandraUtils.VIRTUALMACHINES_CF);
          boolean isVirtualMachineDropped = CassandraUtils.unassignNodes(keyspace_, virtualMachineToRemove, CassandraUtils.VIRTUALMACHINES_CF);
          if (!isVirtualMachineDropped )
          {
              log_.error("Unable to remove the virtual machines");
              return false;
          }
          
        }
        
        return true;
    }
                    
     
    
    protected boolean addVirtualMachineCassandra(VirtualMachineMetaData virtualMachineMetaData)
    {
        try{
            String ipAddress = virtualMachineMetaData.getIpAddress();
            String xmlRepresentation = virtualMachineMetaData.getXmlRepresentation();
            String status = String.valueOf(virtualMachineMetaData.getStatus());
            String errorCode = String.valueOf(virtualMachineMetaData.getErrorCode());
            VirtualMachineLocation location = virtualMachineMetaData.getVirtualMachineLocation();
            ArrayList<Double> requestedCapacity = virtualMachineMetaData.getRequestedCapacity();
            
            Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
            mutator.addInsertion(location.getVirtualMachineId(), CassandraUtils.VIRTUALMACHINES_CF, HFactory.createStringColumn("ipAddress", ipAddress))
                   .addInsertion(location.getVirtualMachineId(), CassandraUtils.VIRTUALMACHINES_CF, HFactory.createStringColumn("xmlRepresentation", xmlRepresentation))
                   .addInsertion(location.getVirtualMachineId(), CassandraUtils.VIRTUALMACHINES_CF, HFactory.createStringColumn("status", status))
                   .addInsertion(location.getVirtualMachineId(), CassandraUtils.VIRTUALMACHINES_CF, HFactory.createStringColumn("errorCode", errorCode))
                   .addInsertion(location.getVirtualMachineId(), CassandraUtils.VIRTUALMACHINES_CF, HFactory.createColumn("location", location, StringSerializer.get(), new JsonSerializer(VirtualMachineLocation.class)))
                   .addInsertion(location.getVirtualMachineId(), CassandraUtils.VIRTUALMACHINES_CF, HFactory.createStringColumn("groupManager", location.getGroupManagerId()))
                   .addInsertion(location.getVirtualMachineId(), CassandraUtils.VIRTUALMACHINES_CF, HFactory.createStringColumn("localController", location.getLocalControllerId()))
                   .addInsertion(location.getVirtualMachineId(), CassandraUtils.VIRTUALMACHINES_CF, HFactory.createColumn("requestedCapacity", requestedCapacity, StringSerializer.get(), new JsonSerializer(ArrayList.class)));
    
            log_.debug("executing mutation");         
            MutationResult result = mutator.execute();
            log_.debug(String.format("Insertion done in %d", result.getExecutionTimeMicro()));
            
        }
        catch(Exception exception)
        {
            log_.error("Unable to add to the repository : " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
        return true;
    }
    
    protected boolean addLocalControllerDescriptionCassandra(String groupManagerId, LocalControllerDescription description)
    {
        int ttl = 600000000 ;
        
        try{
            String id = description.getId();
            String hostname = description.getHostname();
            HypervisorSettings hypervisorSettings = description.getHypervisorSettings();
            ArrayList<Double> totalCapacity = description.getTotalCapacity() ;
            WakeupSettings wakeupSettings = description.getWakeupSettings();
            LocalControllerStatus status = description.getStatus();
            NetworkAddress controlDataAddress = description.getControlDataAddress();
            
            Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
            mutator.addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("hostname", hostname, ttl, StringSerializer.get(), StringSerializer.get()))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("controlDataAddress", controlDataAddress, ttl, StringSerializer.get(), new JsonSerializer(NetworkAddress.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("hypervisorSettings", hypervisorSettings, ttl, StringSerializer.get(), new JsonSerializer(HypervisorSettings.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("totalCapacity", totalCapacity, ttl, StringSerializer.get(), new JsonSerializer(ArrayList.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("wakeupSettings", wakeupSettings, ttl, StringSerializer.get(), new JsonSerializer(WakeupSettings.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("isAssigned", true, ttl, StringSerializer.get(), BooleanSerializer.get()))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("groupManager",  groupManagerId, ttl, StringSerializer.get(), StringSerializer.get()))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("status", String.valueOf(status), ttl, StringSerializer.get(), StringSerializer.get()))
            //mapping add
                    .addInsertion(controlDataAddress.toString(), CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, HFactory.createColumn("id", id, ttl, StringSerializer.get(), StringSerializer.get()));
            log_.debug("executing mutation");         
            MutationResult result = mutator.execute();
            log_.debug(String.format("Insertion done in %d", result.getExecutionTimeMicro()));
            
        }
        catch(Exception exception)
        {
            log_.error("Unable to add to the repository : " + exception.getMessage());
            return false;
        }
        return true;
    }
    
    
    protected boolean addGroupManagerDescriptionCassandra(GroupManagerDescription description, boolean isGroupLeader, boolean isAssigned)
    {
        int ttl = 600000000 ;         
        
        Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
        try{
            
            
            String id = description.getId();
            String hostname = description.getHostname();
            ListenSettings listenSettings = description.getListenSettings();
            NetworkAddress heartbeatAddress = description.getHeartbeatAddress();
            
            mutator.addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("hostname", hostname, ttl, StringSerializer.get(), StringSerializer.get()))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("listenSettings", listenSettings, ttl,  StringSerializer.get(), new JsonSerializer(ListenSettings.class)))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("heartbeatAddress", heartbeatAddress, ttl,  StringSerializer.get(), new JsonSerializer(NetworkAddress.class)))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("isAssigned", isAssigned, ttl, StringSerializer.get(),   new BooleanSerializer()))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("isGroupLeader", isGroupLeader, ttl, StringSerializer.get(),  new BooleanSerializer()))
            ;
            
            MutationResult result = mutator.execute();
            log_.debug(String.format("Insertion done in %d", result.getExecutionTimeMicro()));
            
            
        }
        catch(Exception exception)
        {
            log_.error("Unable to add to the repository : " + exception.getMessage());
            return false;    
        }
        return true;
    }
    
    
    protected VirtualMachineMetaData getVirtualMachineMetaDataCassandra(String virtualMachineId, int numberOfMonitoringEntries)
    {
        log_.debug("Getting virtual machine meta data for " + virtualMachineId);
        try
        {
            RowQueryIterator rowQueryIterator = new RowQueryIterator(
                    keyspace_, CassandraUtils.VIRTUALMACHINES_CF,
                    virtualMachineId, 
                    virtualMachineId, 
                    1);  
            @SuppressWarnings("unchecked")
            Iterator<Row<String, String, String>> rowsIterator = rowQueryIterator.iterator();
            if (!rowsIterator.hasNext())
            {
                log_.debug("NOT FOUND");
                return null;
            }
            Row<String, String, String> row = (Row<String, String, String>) rowsIterator.next();
            log_.debug("found matching row with id" + row.getKey());
            
            
            VirtualMachineMetaData retrievedVirtualMachine;
         
            if (!row.getKey().equals(virtualMachineId))
            {
                return null;
            }
            
            retrievedVirtualMachine = getVirtualMachineMetaData(row);
            
            if (numberOfMonitoringEntries > 0)
            {
                fillVirtualMachineMonitoringData(retrievedVirtualMachine, numberOfMonitoringEntries);
            }
            log_.debug("Returning the virtual machine meta data for " + virtualMachineId);
            return retrievedVirtualMachine;
        }
        catch (Exception exception)
        {
            log_.error("unable to get the virtual machine meta data for " + virtualMachineId);
        }
        
        return null;
    }
    

    protected ArrayList<GroupManagerDescription> getGroupManagerDescriptionsOnly(String firstGroupManagerId, int limit, boolean assignedOnly, int numberOfBacklogEntries)
    {
        log_.debug(String.format("Getting %s groupmanagers", limit));
        ArrayList<GroupManagerDescription> groupManagers = new ArrayList<GroupManagerDescription>();
        
        boolean unlimited = false;
        int rowCount = 2*limit;
        int toLimit = limit;
        String lastKey = firstGroupManagerId;
        boolean terminated = false;
        if (limit<=0)
        {
            log_.debug("Gets All the groupmanagers") ;
            unlimited = true;
            rowCount = 100;
        }
        int iteration = 0 ;
        while(true)
        {
            
            QueryResult<OrderedRows<String, String, String>> result = null;
            if(!assignedOnly)
            {
                log_.debug(String.format("Getting groupmanagers with options : " +
                		"start : %s" +
                		"limit : %d", 
                		lastKey,
                		limit
                        ));
                
                RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory
                        .createRangeSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())
                        .setColumnFamily(CassandraUtils.GROUPMANAGERS_CF)
                        .setKeys(lastKey, null)
                        .setRange(null, null, false, 100)
                        .setRowCount(rowCount);
                
                result = rangeSlicesQuery.execute();
            }
            else
            {
                IndexedSlicesQuery<String, String, String> indexedSlicesQuery = HFactory
                        .createIndexedSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())
                        .setColumnFamily(CassandraUtils.GROUPMANAGERS_CF)
                        .setRange(lastKey, null, false, rowCount)
                        .addEqualsExpression("isAssigned", CassandraUtils.stringTrue);
                
                result = indexedSlicesQuery.execute();
                
            }
            
            OrderedRows<String, String, String> rows = result.get();
            Iterator<Row<String, String, String>> rowsIterator = rows.iterator();    
            
            // go one step further (lastKey has been already fetched in the previous iteration)
            if (iteration > 0 && rowsIterator != null) rowsIterator.next();
            
            while (rowsIterator.hasNext() && !terminated) 
            {
              Row<String, String, String> row = rowsIterator.next();
              lastKey = row.getKey();
              
              if (row.getColumnSlice().getColumns().isEmpty()) 
              {
                  continue;
              }
              
              GroupManagerDescription groupManager = getGroupManagerDescription(row);
              if (numberOfBacklogEntries > 0)
              {
                  fillGroupManagerSummaryInformation(groupManager, numberOfBacklogEntries);
              }
              log_.debug("Found a new GroupManager" + toLimit);
              groupManagers.add(groupManager);
              
              toLimit --; 
              if (!unlimited && toLimit <= 0)
              {
                  terminated = true;
              }    
            }
            
            // we have all the lines (if not unlimited)
            if (!unlimited && toLimit <= 0)
            {
                log_.debug(String.format("All the %d groupmanagers have been fetch", limit));
                break;
            }
            // we cannot fetch more
            if (rows.getCount() < rowCount  )
            {
                log_.debug("No more groupmanager to fetch");
                break;
            }
            
            iteration ++;
        }
        log_.debug(String.format("Returning %d groupmanagers", groupManagers.size()));
        return groupManagers;
    }
    
    /**
    *
    * Clear to repository.
    * 
    */
   protected void clear()
   {
       cluster_.truncate(keyspace_.getKeyspaceName(), CassandraUtils.GROUPMANAGERS_CF);
       cluster_.truncate(keyspace_.getKeyspaceName(), CassandraUtils.LOCALCONTROLLERS_CF);
       cluster_.truncate(keyspace_.getKeyspaceName(), CassandraUtils.LOCALCONTROLLERS_MAPPING_CF);
       cluster_.truncate(keyspace_.getKeyspaceName(), CassandraUtils.VIRTUALMACHINES_CF);
       cluster_.truncate(keyspace_.getKeyspaceName(), CassandraUtils.IPSPOOL_CF);
   }
}
