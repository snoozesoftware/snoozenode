package org.inria.myriads.snoozenode.database.api.impl.cassandra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.ListenSettings;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerLocation;
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
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.RowIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Cassandra Repository.
 * 
 * @author msimonin
 *
 */
public class CassandraRepository
{
    /** Logger. */
    protected static final Logger log_ = LoggerFactory.getLogger(CassandraRepository.class);
    
    /** Cassandra Keyspace.*/
    private static Keyspace keyspace_;
    
    /** Cassandra Cluster. */
    private Cluster cluster_;
    
    /**
     * Constructor.
     * 
     *  @param hosts    List of hosts to connect to.
     *  
     */
    public CassandraRepository(String hosts)
    {
        cluster_ = HFactory.getOrCreateCluster(CassandraUtils.CLUSTER, new CassandraHostConfigurator(hosts));
        keyspace_ = HFactory.createKeyspace(CassandraUtils.KEYSPACE, cluster_);
    }

    
    /**
     * 
     * Gets the groupManager description.
     * Without localcontrollers and virtual machines.
     * 
     * @param groupManagerId            The groupmanager id.
     * @param numberOfBacklogEntries    The number of wanted monitoring values.
     * @return      The groupmanager description or null
     */
    protected GroupManagerDescription getGroupManagerDescriptionOnly(String groupManagerId, int numberOfBacklogEntries) 
    {
        Guard.check(groupManagerId, numberOfBacklogEntries);
        
        ArrayList<GroupManagerDescription> groupManagers = 
                getGroupManagerDescriptionsOnly(groupManagerId, 1, true, numberOfBacklogEntries, new ArrayList<String>());
        if ((groupManagers == null) || groupManagers.size() != 1)
        {
            log_.error("Return value is not correct");
            return null;
        }
        GroupManagerDescription groupManager = groupManagers.get(0);
        if (!groupManager.getId().equals(groupManagerId))
        {
            return null;
        }
        return groupManager;
    }
    
    /**
     * 
     * Gets the groupManagerDescription.
     * Fill it with specific details (monitoring, lc, virtualmachines).
     * 
     * @param groupManagerId                The group manager id to retrieve.
     * @param numberOfbacklogEntries        The number of wanted monitoring values.
     * @param withLocalControllers          True if it must fill with associated localcontrollers.
     * @param isActiveOnly                  True if wanted only active local controllers 
     * @param withVirtualMachines           True if it must fill with associated virtual machines.
     * @param numberOfMonitoringEntries     Number of monitoring entries for the virtualmachines
     * 
     * @return      The groupmanager description or null.
     */
    protected GroupManagerDescription getGroupManagerDescriptionCassandra(
            String groupManagerId, 
            int numberOfbacklogEntries, 
            boolean withLocalControllers,
            boolean isActiveOnly,
            boolean withVirtualMachines,
            int numberOfMonitoringEntries
            )
    {
        GroupManagerDescription groupManagerDescription = 
                getGroupManagerDescriptionOnly(groupManagerId, numberOfbacklogEntries);
        
        if (groupManagerDescription == null || !groupManagerDescription.getId().equals(groupManagerId))
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
        if (localController == null || !localController.getId().equals(localControllerId))
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
        
        //HashMap<String, LocalControllerDescription> localControllers = getLocalControllerDescriptionsOnly(null, localControllerId, 1, numberOfMonitoringEntries, false, true);
        ArrayList<LocalControllerDescription> localControllers = getLocalControllerDescriptionsOnly(null, localControllerId, 1, numberOfMonitoringEntries, false, true);
        if ((localControllers == null) || localControllers.size()!=1 ) // || localControllers.get(localControllerId) == null)
        {
            log_.error("Return value is not correct");
            return null;
        }
        LocalControllerDescription localController = localControllers.get(0);
        if (!localController.getId().equals(localControllerId))
        {
            return null;
        }
        return localController;
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
    protected LocalControllerDescription getLocalControllerDescription(Row<String, String, String> row)
    {
        log_.debug("Deserialize row from cassandra cluster from row id" + row.getKey());
        LocalControllerDescription localControllerDescription = new LocalControllerDescription();
        
        ColumnSlice<String, String> columns = row.getColumnSlice();
 
        LocalControllerStatus status = LocalControllerStatus.valueOf(columns.getColumnByName("status").getValue());
//        if (status != LocalControllerStatus.ACTIVE && isActiveOnly)
//        {
//            return null;
//        }
        String hostname = columns.getColumnByName("hostname").getValue();
        boolean isAssigned =columns.getColumnByName("isAssigned").getValue().equals(CassandraUtils.stringTrue)?true:false;
        
        
        JsonSerializer localControllerLocationSerializer = new JsonSerializer(LocalControllerLocation.class);
        
        LocalControllerLocation location = 
                (LocalControllerLocation) localControllerLocationSerializer.fromString(columns.getColumnByName("localControllerLocation").getValue());
        
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
        localControllerDescription.setIsAssigned(isAssigned);
        localControllerDescription.setLocation(location);
        
        
        return localControllerDescription;
        
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
        
//        boolean isLeader =columns.getColumnByName("isGroupLeader").getValue().equals(CassandraUtils.stringTrue)?true:false;
        
        String hostname = columns.getColumnByName("hostname").getValue();
        boolean isAssigned =columns.getColumnByName("isAssigned").getValue().equals(CassandraUtils.stringTrue)?true:false;
        
        JsonSerializer heartbeatAddressSerializer = new JsonSerializer(NetworkAddress.class);
        NetworkAddress heartbeatAddress = (NetworkAddress) heartbeatAddressSerializer.fromString(columns.getColumnByName("heartbeatAddress").getValue());
        
        JsonSerializer listenSettingsSerializer = new JsonSerializer(ListenSettings.class);
        ListenSettings listenSettings = (ListenSettings) listenSettingsSerializer.fromString(columns.getColumnByName("listenSettings").getValue());
        
        groupManagerDescription.setId(row.getKey());
        groupManagerDescription.setHostname(hostname);
        groupManagerDescription.setIsAssigned(isAssigned);
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
        //HashMap<String, LocalControllerDescription> localControllers =
        ArrayList<LocalControllerDescription> localControllers = 
                getLocalControllerDescriptionsOnly(groupManager.getId(),  null ,-1, numberOfMonitoringEntries, isActiveOnly, true);
        
        groupManager.setLocalControllersFromArray(localControllers);
        
    }
    
    /**
     * 
     * Fills the hashmap with virtualmachines.
     * 
     * @param groupManagerId
     * @param localControllers
     * @param numberOfMonitoringEntries
     */
    protected void fillWithVirtualMachines(String groupManagerId, ArrayList<LocalControllerDescription> localControllers, int numberOfMonitoringEntries)
    {
        if (localControllers.size()==0)
        {
            log_.debug("No Local controllers assigned to this group manager");
            return;
        }
        // arraylist to hashmap
        HashMap<String, LocalControllerDescription> localControllersMap = new HashMap<String, LocalControllerDescription>();
        for (LocalControllerDescription localController : localControllers)
        {
            localControllersMap.put(localController.getId(), localController);
        }
        

        //HashMap<String, VirtualMachineMetaData> virtualMachines = getVirtualMachineDescriptionsOnly(groupManagerId, null, null, -1, numberOfMonitoringEntries, true);
        ArrayList<VirtualMachineMetaData> virtualMachines = getVirtualMachineDescriptionsOnly(groupManagerId, null, null, -1, numberOfMonitoringEntries, true);
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            String localControllerId = virtualMachine.getVirtualMachineLocation().getLocalControllerId();
            String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            if (!localControllersMap.containsKey(localControllerId))
            {
                log_.error(String.format("virtual machine %s is assigned to groupmanager %s which doesn't manage localcontroller %s", 
                        virtualMachineId,
                        groupManagerId,
                        localControllerId
                        ));
                continue;
            }
            log_.debug(String.format("Add virtual machine %s to group manager %s description", virtualMachineId, groupManagerId));
            localControllersMap.get(localControllerId).getVirtualMachineMetaData().put(virtualMachineId, virtualMachine);
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
    
    private void fillWithVirtualMachines(String groupManagerId, HashMap<String, LocalControllerDescription> localControllers,
            int numberOfMonitoringEntries)
    {
        if (localControllers.size()==0)
        {
            log_.debug("No Local controllers assigned to this group manager");
            return;
        }
 
        
        

        //HashMap<String, VirtualMachineMetaData> virtualMachines = getVirtualMachineDescriptionsOnly(groupManagerId, null, null, -1, numberOfMonitoringEntries, true);
        ArrayList<VirtualMachineMetaData> virtualMachines = getVirtualMachineDescriptionsOnly(groupManagerId, null, null, -1, numberOfMonitoringEntries, true);
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
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
       
//       HashMap<String, VirtualMachineMetaData> virtualMachines = getVirtualMachines("localController", localControllerId, numberOfMonitoringEntries);
       //HashMap<String, VirtualMachineMetaData> virtualMachines = getVirtualMachineDescriptionsOnly(null, localControllerId, null, -1, numberOfMonitoringEntries, true);
       ArrayList<VirtualMachineMetaData> virtualMachines = getVirtualMachineDescriptionsOnly(null, localControllerId, null, -1, numberOfMonitoringEntries, true);
       for (VirtualMachineMetaData virtualMachine: virtualMachines)
       {
           String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
           log_.debug(String.format("Add virtual machine %s to localController %s description", virtualMachineId, localControllerId));
           localController.getVirtualMachineMetaData().put(virtualMachineId, virtualMachine);
       }
    }
    
    protected ArrayList<LocalControllerDescription> getLocalControllerDescriptionsCassandra(String groupManagerId,
            int numberOfMonitoringEntries, boolean isActiveOnly, boolean withVirtualMachines)
    {
        //HashMap<String, LocalControllerDescription> localControllerDescriptions = 
        
        ArrayList<LocalControllerDescription> localControllersDescriptions = getLocalControllerDescriptionsOnly(
                groupManagerId,
                null,
                -1,
                numberOfMonitoringEntries,
                isActiveOnly,
                true
                );
        
        if (withVirtualMachines)
        {
            fillWithVirtualMachines(groupManagerId, localControllersDescriptions, numberOfMonitoringEntries);
        }
        
       //ArrayList<LocalControllerDescription> localControllers = new ArrayList<LocalControllerDescription>();
       //localControllers.addAll(localControllerDescriptions.values());
       
       return localControllersDescriptions;
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
    protected ArrayList<LocalControllerDescription> getLocalControllerDescriptionsOnly(
            String groupManagerId, 
            String localControllerStart,
            int limit,
            int numberOfMonitoringEntries,
            boolean isActiveOnly,
            boolean onlyAssigned
            )
    {
        
        
        
//        String lastKey = localControllerStart;        
//        boolean unlimited = false;
//        int rowCount = 2*limit;
//        int toLimit = limit;
//        if (limit<=0)
//        {
//            log_.debug("Gets All the localControllers") ;
//            unlimited = true;
//            rowCount = 100;
//        }
//       
//        boolean terminated = false;
//        int iteration = 0; 
//        //HashMap<String, LocalControllerDescription> localControllers = new HashMap<String, LocalControllerDescription>();
        ArrayList<LocalControllerDescription> localControllers = new ArrayList<LocalControllerDescription>();
        
        log_.debug("Getting all localcontrollers with new method");
        RowIterator rowIterator = new RowIterator();
        rowIterator
            .setKeyspace(keyspace_)
            .setColumnFamily(CassandraUtils.LOCALCONTROLLERS_CF)
            .setKeys(localControllerStart, "")
            .setLimit(limit);
      
        
        if (onlyAssigned)
        {
            log_.debug("adding indexes is assigned");
            rowIterator.addEqualsExpression("isAssigned", CassandraUtils.stringTrue);
        }
        if (isActiveOnly)
        {
            log_.debug("adding indexes is active");
            rowIterator.addEqualsExpression("status", String.valueOf(LocalControllerStatus.ACTIVE));
        }
        if (groupManagerId != null && !groupManagerId.equals("") )
        {
            log_.debug("adding indexes groupmanager");
            rowIterator.addEqualsExpression("groupManager", groupManagerId);
        }
        
        rowIterator.execute();
        
        for (Row<String, String, String> row : rowIterator)
        {
            log_.debug("Preparing to deserialize row " + row.getKey());
            LocalControllerDescription localController = getLocalControllerDescription(row);
            if (localController == null)
            {
                continue;
            }
            
//              if (numberOfMonitoringEntries > 0)
//            {
//                fillGroupManagerSummaryInformation(groupManager, numberOfBacklogEntries);
//            }            
            localControllers.add(localController);
        }
//        while(true)
//        {
//            
//            QueryResult<OrderedRows<String, String, String>> result = null;
//            if ((groupManagerId == null || groupManagerId.equals("")) && !isActiveOnly && !onlyAssigned)
//            {
//
//                log_.debug("Ranged query to fetch the localcontrollers");
//                RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory
//                        .createRangeSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())
//                        .setColumnFamily(CassandraUtils.LOCALCONTROLLERS_CF)
//                        .setKeys(lastKey, null)
//                        .setRange(null, null, false, 100)
//                        .setRowCount(rowCount);
//                
//                 result = rangeSlicesQuery.execute();
//            }
//            else
//            {
//                log_.debug("Indexed query to fecth the localcontrollers");
//                IndexedSlicesQuery<String, String, String> indexedSlicesQuery =  HFactory.createIndexedSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
//                indexedSlicesQuery.setColumnFamily(CassandraUtils.LOCALCONTROLLERS_CF);
//                indexedSlicesQuery.setStartKey(lastKey);
//                indexedSlicesQuery.setRange(null, null, false, 100);
//                
//                if (groupManagerId != null && !groupManagerId.equals(""))
//                {
//                    indexedSlicesQuery.addEqualsExpression("groupManager", groupManagerId);
//                }
//                if (onlyAssigned)
//                {
//                    indexedSlicesQuery.addEqualsExpression("isAssigned", CassandraUtils.stringTrue);
//                }
//                
//                if (isActiveOnly)
//                {
//                    indexedSlicesQuery.addEqualsExpression("status", String.valueOf(LocalControllerStatus.ACTIVE));
//                }
//                
//                result = indexedSlicesQuery.execute();
//                
//            }
//            
//            OrderedRows<String, String, String> rows = result.get() ;
//            
//            Iterator<Row<String, String, String>> rowsIterator = rows.iterator();
//            
//
//            if (!rowsIterator.hasNext())
//            {
//                log_.debug(String.format("Group manager %s has no local controller", groupManagerId));
//                
//                return localControllers;
//            }
//            
//            //go one step further (lastKey has been already fetched in the previous iteration)
//            if (iteration > 0 && rowsIterator != null) rowsIterator.next();
//            
//            while(rowsIterator.hasNext() & !terminated)
//            {
//                
//                Row<String, String, String> row = rowsIterator.next();
//                lastKey = row.getKey();
//                log_.debug("LastKey = " + lastKey);
//                
//                if (row.getColumnSlice().getColumns().isEmpty()) 
//                {
//                    //skip tombstone
//                    continue;
//                }
//                
//                LocalControllerDescription retrievedDescription;
//                retrievedDescription = getLocalControllerDescription(row, isActiveOnly);
//                if (retrievedDescription != null)
//                {
//                    //localControllers.put(row.getKey(), retrievedDescription);
//                    localControllers.add(retrievedDescription);
//                    toLimit -- ;
//                    if (!unlimited &&  toLimit <=0)
//                    {
//                        terminated = true;
//                    }
//                }
//            }
//            // no more local controller to fetch
//            if (terminated)
//            {
//                log_.debug("Got all the localcontrollers");
//                break;
//            }
//            
//            // we cannot fetch more
//            if (rows.getCount() < rowCount  )
//            {
//                log_.debug("No more groupmanager to fetch");
//                break;
//            }
//            
//            iteration++;
//        }
        return localControllers;  
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
    protected VirtualMachineMetaData getVirtualMachineDescriptionOnly(Row<String, String, String> row)
    {
        log_.debug("Deserialize virtual machine row from cassandra cluster from rwo id" + row.getKey());
        VirtualMachineMetaData virtualMachineMetaData = new VirtualMachineMetaData();
        ColumnSlice<String, String> columns = row.getColumnSlice();
        
        boolean isAssigned =columns.getColumnByName("isAssigned").getValue().equals(CassandraUtils.stringTrue)?true:false;
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
        virtualMachineMetaData.setIsAssigned(isAssigned);
        
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
                   .addInsertion(location.getVirtualMachineId(), CassandraUtils.VIRTUALMACHINES_CF, HFactory.createColumn("requestedCapacity", requestedCapacity, StringSerializer.get(), new JsonSerializer(ArrayList.class)))
                   .addInsertion(location.getVirtualMachineId(), CassandraUtils.VIRTUALMACHINES_CF, HFactory.createColumn("isAssigned", true, StringSerializer.get(), BooleanSerializer.get()));
    
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
            LocalControllerLocation location = description.getLocation();
            
            Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
            mutator.addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("hostname", hostname, ttl, StringSerializer.get(), StringSerializer.get()))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("controlDataAddress", controlDataAddress, ttl, StringSerializer.get(), new JsonSerializer(NetworkAddress.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("hypervisorSettings", hypervisorSettings, ttl, StringSerializer.get(), new JsonSerializer(HypervisorSettings.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("totalCapacity", totalCapacity, ttl, StringSerializer.get(), new JsonSerializer(ArrayList.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("wakeupSettings", wakeupSettings, ttl, StringSerializer.get(), new JsonSerializer(WakeupSettings.class)))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("isAssigned", true, ttl, StringSerializer.get(), BooleanSerializer.get()))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("groupManager",  groupManagerId, ttl, StringSerializer.get(), StringSerializer.get()))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("status", String.valueOf(status), ttl, StringSerializer.get(), StringSerializer.get()))
                   .addInsertion(id, CassandraUtils.LOCALCONTROLLERS_CF, HFactory.createColumn("localControllerLocation", location, ttl, StringSerializer.get(), new JsonSerializer(LocalControllerLocation.class)))
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
        return getVirtualMachineDescriptionOnly(virtualMachineId, numberOfMonitoringEntries);
        
        // no children to fecth
    }
    

    protected ArrayList<GroupManagerDescription> getGroupManagerDescriptionsOnly(
            String firstGroupManagerId, 
            int limit, 
            boolean assignedOnly, 
            int numberOfBacklogEntries,
            List<String> toExclude
            )
    {
        log_.debug(String.format("Getting %s groupmanagers", limit));
        ArrayList<GroupManagerDescription> groupManagers = new ArrayList<GroupManagerDescription>();
        
        RowIterator rowIterator = new RowIterator();
        rowIterator
            .setKeyspace(keyspace_)
            .setColumnFamily(CassandraUtils.GROUPMANAGERS_CF)
            .setKeys(firstGroupManagerId, "")
            .setLimit(limit);
        
        for (String exclude : toExclude)
        {
            rowIterator.addExcludedRows(exclude);
        }
        
        if (assignedOnly)
        {
            rowIterator.addEqualsExpression("isAssigned", CassandraUtils.stringTrue);
        }
        
        rowIterator.execute();
        
        for (Row<String, String, String> row : rowIterator)
        {
            GroupManagerDescription groupManager = getGroupManagerDescription(row);
            if (groupManager == null)
            {
                continue;
            }
              if (numberOfBacklogEntries > 0)
            {
                fillGroupManagerSummaryInformation(groupManager, numberOfBacklogEntries);
            }            
            groupManagers.add(groupManager);
        }

          
//        boolean unlimited = false;
//        int rowCount = 2*limit;
//        int toLimit = limit;
//        String lastKey = firstGroupManagerId;
//        boolean terminated = false;
//        if (limit<=0)
//        {
//            log_.debug("Gets All the groupmanagers") ;
//            unlimited = true;
//            rowCount = 100;
//        }
//        int iteration = 0 ;
//        while(true)
//        {
//            
//            QueryResult<OrderedRows<String, String, String>> result = null;
//            if(!assignedOnly)
//            {
//                log_.debug(String.format("Getting groupmanagers with options : " +
//                		"start : %s" +
//                		"limit : %d", 
//                		lastKey,
//                		limit
//                        ));
//                
//                RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory
//                        .createRangeSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())
//                        .setColumnFamily(CassandraUtils.GROUPMANAGERS_CF)
//                        .setKeys(lastKey, null)
//                        .setRange(null, null, false, 100)
//                        .setRowCount(rowCount);
//                
//                result = rangeSlicesQuery.execute();
//                
//            }
//            else
//            {
//                log_.debug(String.format("Getting indexed groupmanagers with options : " +
//                        "start : %s" +
//                        "limit : %d", 
//                        lastKey,
//                        limit
//                        ));
//                IndexedSlicesQuery<String, String, String> indexedSlicesQuery = HFactory
//                        .createIndexedSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())
//                        .setColumnFamily(CassandraUtils.GROUPMANAGERS_CF)
//                        .setRange(null, null, false, 100)
//                        .setStartKey(lastKey)
//                        .addEqualsExpression("isAssigned", CassandraUtils.stringTrue);
//                
//                result = indexedSlicesQuery.execute();
//                
//            }
//            
//            OrderedRows<String, String, String> rows = result.get();
//            Iterator<Row<String, String, String>> rowsIterator = rows.iterator();    
//            
//            // go one step further (lastKey has been already fetched in the previous iteration)
//            if (iteration > 0 && rowsIterator != null) rowsIterator.next();
//            
//            while (rowsIterator.hasNext() && !terminated) 
//            {
//              Row<String, String, String> row = rowsIterator.next();
//              lastKey = row.getKey();
//              
//              if (row.getColumnSlice().getColumns().isEmpty()) 
//              {
//                  continue;
//              }
//              
//              GroupManagerDescription groupManager = getGroupManagerDescription(row, withLeader);
//              if (groupManager == null)
//              {
//                  continue;
//              }
//              
//              if (numberOfBacklogEntries > 0)
//              {
//                  fillGroupManagerSummaryInformation(groupManager, numberOfBacklogEntries);
//              }
//              log_.debug("Found a new GroupManager" + toLimit);
//              groupManagers.add(groupManager);
//              
//              toLimit --; 
//              if (!unlimited && toLimit <= 0)
//              {
//                  terminated = true;
//              }    
//            }
//            
//            // we have all the lines (if not unlimited)
//            if (!unlimited && toLimit <= 0)
//            {
//                log_.debug(String.format("All the %d groupmanagers have been fetch", limit));
//                break;
//            }
//            // we cannot fetch more
//            if (rows.getCount() < rowCount  )
//            {
//                log_.debug("No more groupmanager to fetch");
//                break;
//            }
//            
//            iteration ++;
//        }
        log_.debug(String.format("Returning %d groupmanagers", groupManagers.size()));
        return groupManagers;
    }
    
    /**
     * TODO call to getLocalControllerDescriptionSonly
     * 
     * @param localControllerId
     * @param numberOfMonitoringEntries
     * @return
     */
    protected VirtualMachineMetaData getVirtualMachineDescriptionOnly(String virtualMachineId, int numberOfMonitoringEntries)
    {
        Guard.check(virtualMachineId, numberOfMonitoringEntries);       
        
        //HashMap<String, VirtualMachineMetaData> virtualMachines =
        ArrayList<VirtualMachineMetaData> virtualMachines = 
                getVirtualMachineDescriptionsOnly(null, null, virtualMachineId, 1, numberOfMonitoringEntries, true);
        
        
        if ((virtualMachines == null) || virtualMachines.size()!=1 ) //|| virtualMachines.get(virtualMachineId) == null)
        {
            log_.error("Return value is not correct");
            return null;
        }
        VirtualMachineMetaData virtualMachine = virtualMachines.get(0);
        if (!virtualMachine.getVirtualMachineLocation().getVirtualMachineId().equals(virtualMachineId))
        {
            return null;
        }
        
        if (numberOfMonitoringEntries > 0)
        {
            fillVirtualMachineMonitoringData(virtualMachine, numberOfMonitoringEntries);
        }
        
        return virtualMachine;
    }
    
    protected ArrayList<VirtualMachineMetaData> getVirtualMachineDescriptionsOnly(
            String groupManagerId, 
            String localControllerId,
            String virtualMachineStart,
            int limit,
            int numberOfMonitoringEntries,
            boolean onlyAssigned
            )
    {
//        String lastKey = virtualMachineStart;        
//        boolean unlimited = false;
//        int rowCount = 2*limit;
//        int toLimit = limit;
//        if (limit<=0)
//        {
//            log_.debug("Gets All the virtualMachines") ;
//            unlimited = true;
//            rowCount = 100;
//        }
//       
//        boolean terminated = false;
//        int iteration = 0; 
        //HashMap<String, VirtualMachineMetaData> virtualMachines = new HashMap<String, VirtualMachineMetaData>();
        ArrayList<VirtualMachineMetaData> virtualMachines = new ArrayList<VirtualMachineMetaData>();
        
        log_.debug("Getting all localcontrollers with new method");
        RowIterator rowIterator = new RowIterator();
        rowIterator
            .setKeyspace(keyspace_)
            .setColumnFamily(CassandraUtils.VIRTUALMACHINES_CF)
            .setKeys(virtualMachineStart, "")
            .setLimit(limit);
      
        
        if (onlyAssigned)
        {
            log_.debug("adding indexes is assigned");
            rowIterator.addEqualsExpression("isAssigned", CassandraUtils.stringTrue);
        }
    
        if (groupManagerId != null && !groupManagerId.equals("") )
        {
            log_.debug("adding indexes groupmanager");
            rowIterator.addEqualsExpression("groupManager", groupManagerId);
        }
        
        if (localControllerId != null && !localControllerId.equals("") )
        {
            log_.debug("adding indexes groupmanager");
            rowIterator.addEqualsExpression("localController", localControllerId);
        }
        
        
        rowIterator.execute();
        
        for (Row<String, String, String> row : rowIterator)
        {
            log_.debug("Preparing to deserialize row " + row.getKey());
            VirtualMachineMetaData virtualMachine = getVirtualMachineDescriptionOnly(row);
            if (virtualMachine == null)
            {
                continue;
            }
            if (numberOfMonitoringEntries > 0)
            {
                fillVirtualMachineMonitoringData(virtualMachine, numberOfMonitoringEntries);
            }
            virtualMachines.add(virtualMachine);
            
        }
//        while(true)
//        {
//            
//            QueryResult<OrderedRows<String, String, String>> result = null;
//            if ((groupManagerId == null || groupManagerId.equals("")) &&
//               (localControllerId == null || localControllerId.equals(""))&&
//               !onlyAssigned)
//            {
//
//                log_.debug("Ranged query to fetch the virtual machines");
//                RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory
//                        .createRangeSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())
//                        .setColumnFamily(CassandraUtils.VIRTUALMACHINES_CF)
//                        .setKeys(lastKey, null)
//                        .setRange(null, null, false, 100)
//                        .setRowCount(rowCount);
//                
//                 result = rangeSlicesQuery.execute();
//            }
//            else
//            {
//                log_.debug("Indexed query to fecth the virtual machines");
//                IndexedSlicesQuery<String, String, String> indexedSlicesQuery =  HFactory.createIndexedSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
//                indexedSlicesQuery.setColumnFamily(CassandraUtils.VIRTUALMACHINES_CF);
//                indexedSlicesQuery.setStartKey(lastKey);
//                indexedSlicesQuery.setRange(null, null, false, 100);
//                
//                if (localControllerId != null && !localControllerId.equals(""))
//                {
//                    indexedSlicesQuery.addEqualsExpression("localController", localControllerId);
//                }
//                
//                if (groupManagerId != null && !groupManagerId.equals(""))
//                {
//                    indexedSlicesQuery.addEqualsExpression("groupManager", groupManagerId);
//                }
//                if (onlyAssigned)
//                {
//                    indexedSlicesQuery.addEqualsExpression("isAssigned", CassandraUtils.stringTrue);
//                }
//                
//                result = indexedSlicesQuery.execute();
//                
//            }
//            
//            OrderedRows<String, String, String> rows = result.get() ;
//            
//            Iterator<Row<String, String, String>> rowsIterator = rows.iterator();
//            
//
//            if (!rowsIterator.hasNext())
//            {
//                log_.debug("no virtual machine found");
//                
//                return virtualMachines;
//            }
//            
//            //go one step further (lastKey has been already fetched in the previous iteration)
//            if (iteration > 0 && rowsIterator != null) rowsIterator.next();
//            
//            while(rowsIterator.hasNext() & !terminated)
//            {
//                
//                Row<String, String, String> row = rowsIterator.next();
//                lastKey = row.getKey();
//                log_.debug("LastKey = " + lastKey);
//                
//                if (row.getColumnSlice().getColumns().isEmpty()) 
//                {
//                    //skip tombstone
//                    continue;
//                }
//                
//                VirtualMachineMetaData retrievedDescription;
//                retrievedDescription = getVirtualMachineDescriptionOnly(row);
//                
//                if (retrievedDescription != null)
//                {
//                    //virtualMachines.put(row.getKey(), retrievedDescription);
//                    virtualMachines.add(retrievedDescription);
//                    toLimit -- ;
//                    if (!unlimited &&  toLimit <=0)
//                    {
//                        terminated = true;
//                    }
//                }
//            }
//            // no more local controller to fetch
//            if (terminated)
//            {
//                log_.debug("Got all the localcontrollers");
//                break;
//            }
//            
//            // we cannot fetch more
//            if (rows.getCount() < rowCount )
//            {
//                log_.debug("No more groupmanager to fetch");
//                break;
//            }
//            
//            iteration++;
//        }
        return virtualMachines;  
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



    /**
     * @return the keyspace_
     */
    public static Keyspace getKeyspace()
    {
        return keyspace_;
    }



    /**
     * @return the cluster
     */
    public Cluster getCluster()
    {
        return cluster_;
    }
}
