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
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
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
    
    protected LocalControllerDescription getLocalControllerDescriptionOnly(String localControllerId, int numberOfMonitoringEntries)
    {
        Guard.check(localControllerId, numberOfMonitoringEntries);       
        log_.debug(String.format("Getting local controller description for %s", localControllerId));
        try
        {
            
            
            RowQueryIterator rowQueryIterator = new RowQueryIterator(
                    keyspace_, CassandraUtils.LOCALCONTROLLERS_CF,
                    localControllerId, // start
                    localControllerId, // end
                    1); // rows to fecth 
            
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
        log_.debug("Deserialize row from cassandra cluster from rwo id" + row.getKey());
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        ColumnSlice<String, String> columns = row.getColumnSlice();
        
  
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
                getLocalControllerDescriptionsOnly(groupManager.getId(), numberOfMonitoringEntries, isActiveOnly);
        
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
                numberOfMonitoringEntries,
                isActiveOnly
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
     * @param numberOfMonitoringEntries     Number of monitoring entries to fecth
     * @param isActiveOnly                  Only gets ACTIVE localController
     * @param withVirtualMachines           Gets the virtual machines associated.
     * @return
     */
    protected HashMap<String, LocalControllerDescription> getLocalControllerDescriptionsOnly(
            String groupManagerId, 
            int numberOfMonitoringEntries,
            boolean isActiveOnly
            )
    {
        
        HashMap<String, LocalControllerDescription> localControllers = new HashMap<String, LocalControllerDescription>();
        
        IndexedSlicesQuery<String, String, String> indexedSlicesQuery =  HFactory.createIndexedSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
        indexedSlicesQuery.setColumnFamily(CassandraUtils.LOCALCONTROLLERS_CF);
        indexedSlicesQuery.setRange(null, null, false, 100);
        indexedSlicesQuery.addEqualsExpression("groupManager", groupManagerId);
        if (isActiveOnly)
        {
            indexedSlicesQuery.addEqualsExpression("status", String.valueOf(LocalControllerStatus.ACTIVE));
        }
        
        QueryResult<OrderedRows<String, String, String>> queryResult = indexedSlicesQuery.execute();
        OrderedRows<String, String, String> rows = queryResult.get() ;
        
        Iterator<Row<String, String, String>> rowsIterator = rows.iterator();
        if (!rowsIterator.hasNext())
        {
            log_.debug(String.format("Group manager %s has no local controller", groupManagerId));
            
            return localControllers;
        }
        while(rowsIterator.hasNext())
        {
            Row<String, String, String> row = (Row<String, String, String>) rowsIterator.next();
            LocalControllerDescription retrievedDescription;
            retrievedDescription = getLocalControllerDescription(row, isActiveOnly);
            if (retrievedDescription != null)
            {
                localControllers.put(row.getKey(), retrievedDescription);
            }
        }
        return localControllers;  
    }
    
    /**
     * 
     * Gets the virtual machines associated to a given group manager.
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
    
    protected boolean dropLocalController(String localControllerId, boolean forceDelete, boolean withVirtualMachines)
    {
        LocalControllerDescription localController = getLocalControllerDescriptionCassandra(localControllerId,0,true,0);
        
        if (localController==null)
        {
            log_.debug("unable to find the local controller " + localControllerId);
            return false;
        }
        
        if (localController.getStatus()==LocalControllerStatus.PASSIVE && !forceDelete)
        {
            log_.debug("This local controller is in PASSIVE mode! Will not delete!");
            return false;
        }

        
        boolean isLocalControllerDropped = drop(Arrays.asList(localController.getId()), CassandraUtils.LOCALCONTROLLERS_CF);
        if (! isLocalControllerDropped)
        {
            log_.error("unable to remove the local controller " + localController.getId());
            return false;
        }
        
        boolean isMappingDropped = drop(Arrays.asList(localController.getControlDataAddress().toString()), CassandraUtils.LOCALCONTROLLERS_MAPPING_CF);
        if (!isMappingDropped)
        {
            log_.error("Unable to remove the mapping for the assigned localcontroller");
            return false;
        }
        
        ArrayList<String> virtualMachineToRemove = new ArrayList<String>();
        virtualMachineToRemove.addAll(localController.getVirtualMachineMetaData().keySet());
        boolean isVirtualMachineDropped = drop(virtualMachineToRemove, CassandraUtils.VIRTUALMACHINES_CF);
        if (!isVirtualMachineDropped )
        {
            log_.error("Unable to remove the virtual machines");
            return false;
        }
        
        return true;
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
        boolean isGroupManagerDropped = drop(Arrays.asList(groupManager.getId()), CassandraUtils.GROUPMANAGERS_CF);
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
          boolean isLocalControllerDropped = drop(localControllerToRemove, CassandraUtils.LOCALCONTROLLERS_CF);
          if (! isLocalControllerDropped)
          {
              log_.error("Unable to remove the assigned localcontrollers");
              return false;
          }
          
          boolean isMappingDropped = drop(mappingToRemove, CassandraUtils.LOCALCONTROLLERS_MAPPING_CF);
          if (!isMappingDropped)
          {
              log_.error("Unable to remove the mapping for the assigned localcontroller");
              return false;
          }
          
          boolean isVirtualMachineDropped = drop(virtualMachineToRemove, CassandraUtils.VIRTUALMACHINES_CF);
          if (!isVirtualMachineDropped )
          {
              log_.error("Unable to remove the virtual machines");
              return false;
          }
          
        }
        
        return true;
    }
                    
    /**
     * 
     * Drop list of keys from a column family.
     * 
     * @param list                  List of keys
     * @param columnFamily          Column family to remove from
     */
    protected boolean drop(List<String> list, String columnFamily)
    {
        
        log_.debug(String.format("Removing %d keys from %s", list.size(), columnFamily));
        try
        {
            Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
            for(String rowId : list)
            {
                mutator.addDeletion(rowId, columnFamily);
            }
            mutator.execute();
        }
        catch(Exception exception)
        {
            log_.error(String.format("Unable to remove the keys from %s", columnFamily));
            return false;
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
        try{
            String id = description.getId();
            String hostname = description.getHostname();
            HypervisorSettings hypervisorSettings = description.getHypervisorSettings();
            ArrayList<Double> totalCapacity = description.getTotalCapacity() ;
            WakeupSettings wakeupSettings = description.getWakeupSettings();
            LocalControllerStatus status = description.getStatus();
            NetworkAddress controlDataAddress = description.getControlDataAddress();
            
            Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
            mutator.addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createStringColumn("hostname", hostname))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("controlDataAddress", controlDataAddress, StringSerializer.get(), new JsonSerializer(NetworkAddress.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("hypervisorSettings", hypervisorSettings, StringSerializer.get(), new JsonSerializer(HypervisorSettings.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("totalCapacity", totalCapacity, StringSerializer.get(), new JsonSerializer(ArrayList.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("wakeupSettings", wakeupSettings, StringSerializer.get(), new JsonSerializer(WakeupSettings.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("isAssigned", true, StringSerializer.get(), BooleanSerializer.get()))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createStringColumn("groupManager", groupManagerId))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createStringColumn("status", String.valueOf(status)))
            //mapping add
                    .addInsertion(controlDataAddress.toString(), CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, HFactory.createStringColumn("id", id));
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
        StringSerializer stringSerializer = new StringSerializer();
        Mutator<String> mutator = HFactory.createMutator(keyspace_, stringSerializer);
        try{
            
            
            String id = description.getId();
            String hostname = description.getHostname();
            ListenSettings listenSettings = description.getListenSettings();
            NetworkAddress heartbeatAddress = description.getHeartbeatAddress();
            
            mutator.addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("hostname", hostname, StringSerializer.get(), stringSerializer.get()))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("listenSettings", listenSettings,  StringSerializer.get(), new JsonSerializer(ListenSettings.class)))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("heartbeatAddress", heartbeatAddress, StringSerializer.get(), new JsonSerializer(NetworkAddress.class)))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("isAssigned", isAssigned, StringSerializer.get(),   new BooleanSerializer()))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("isGroupLeader", isGroupLeader, StringSerializer.get(),  new BooleanSerializer()));
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
    
}
