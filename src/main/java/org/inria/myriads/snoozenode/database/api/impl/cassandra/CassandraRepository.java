package org.inria.myriads.snoozenode.database.api.impl.cassandra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
    private Keyspace keyspace_;
    
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
                getGroupManagerDescriptionsOnly(
                        groupManagerId,
                        1,
                        true,
                        numberOfBacklogEntries,
                        new ArrayList<String>());
        
        if ((groupManagers == null) || groupManagers.size() != 1)
        {
            log_.error("Return value is not correct");
            return null;
        }
        GroupManagerDescription groupManager = groupManagers.get(0);
        if (!groupManager.getId().equals(groupManagerId))
        {
            log_.debug("The group manager id from cassandra doesn't match the needed group manager id.");
            log_.debug(groupManagerId + " != " + groupManager.getId());
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
    
    /**
     * 
     * Gets local controller description.
     * 
     * @param localControllerId                 The local controller description.
     * @param numberOfHostMonitoringEntries     number of monitoring entries (host).
     * @param withVirtualMachines               with virtualmachines.
     * @param numberOfMonitoringEntries         number of monitoring entries (vms)
     * @return  The local controller description.
     */
    protected LocalControllerDescription getLocalControllerDescriptionCassandra(
            String localControllerId,
            int numberOfHostMonitoringEntries,
            boolean withVirtualMachines,
            int numberOfMonitoringEntries
            )
    {
        LocalControllerDescription localController = 
                getLocalControllerDescriptionOnly(localControllerId, numberOfMonitoringEntries);
        
        if (localController == null || !localController.getId().equals(localControllerId))
        {
            return null;
        }
        
        if (withVirtualMachines)
        {
            fillWithVirtualMachines(localController, numberOfMonitoringEntries);
        }
        
        return localController;
    }
    
    /**
     * 
     * Gets the localcontroller only (without virtual machines).
     * 
     * @param localControllerId             The local controller Id.
     * @param numberOfMonitoringEntries     Number of monitoring entries.
     * @return  The local controller description.
     */
    protected LocalControllerDescription getLocalControllerDescriptionOnly(
            String localControllerId, int numberOfMonitoringEntries)
    {
        Guard.check(localControllerId, numberOfMonitoringEntries);       
        
       
        ArrayList<LocalControllerDescription> localControllers = 
                getLocalControllerDescriptionsOnly(null, localControllerId, 1, numberOfMonitoringEntries, false, true);
        
        if ((localControllers == null) || localControllers.size() != 1)
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
     * @param row                           the cassandra row
     * @return  the deserialized local controller description.
     */
    protected LocalControllerDescription getLocalControllerDescription(Row<String, String, String> row)
    {
        log_.debug("Deserialize row from cassandra cluster from row id" + row.getKey());
        LocalControllerDescription localControllerDescription = new LocalControllerDescription();
        
        ColumnSlice<String, String> columns = row.getColumnSlice();
 
        LocalControllerStatus status = LocalControllerStatus.valueOf(columns.getColumnByName("status").getValue());

        String hostname = columns.getColumnByName("hostname").getValue();
        boolean isAssigned = 
                columns.getColumnByName("isAssigned").getValue().equals(CassandraUtils.stringTrue) ? true : false;
        
        
        JsonSerializer localControllerLocationSerializer = new JsonSerializer(LocalControllerLocation.class);
        
        LocalControllerLocation location = (LocalControllerLocation) localControllerLocationSerializer
                .fromString(columns.getColumnByName("localControllerLocation").getValue());
        
        JsonSerializer hypervisorSettingsSerializer = new JsonSerializer(HypervisorSettings.class);
        HypervisorSettings hypervisorSettings = (HypervisorSettings) hypervisorSettingsSerializer
                .fromString(columns.getColumnByName("hypervisorSettings").getValue());
        
        JsonSerializer totalCapacitySerializer = new JsonSerializer(ArrayList.class);
        @SuppressWarnings("unchecked")
        ArrayList<Double> totalCapacity = (ArrayList<Double>) totalCapacitySerializer
        .fromString(columns.getColumnByName("totalCapacity").getValue());
        
        JsonSerializer controlDataAddressSerializer = new JsonSerializer(NetworkAddress.class);
        NetworkAddress controlDataAddress = (NetworkAddress) controlDataAddressSerializer
                .fromString(columns.getColumnByName("controlDataAddress").getValue());
        
        JsonSerializer wakeupSettingsSerializer = new JsonSerializer(WakeupSettings.class);
        WakeupSettings wakeupSettings = (WakeupSettings) wakeupSettingsSerializer
                .fromString(columns.getColumnByName("wakeupSettings").getValue());
        
        
        
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
     * @param groupManagerDescription       The group manager description to fill.
     * @param numberOfBacklogEntries        The number of monitoring entries to use.    
     */
    protected void fillGroupManagerSummaryInformation(
            GroupManagerDescription groupManagerDescription, int numberOfBacklogEntries)
    {
        log_.debug("Gets the monitoring datas from the cassandra cluster");
        SliceQuery<String, Long, Object> query = HFactory.createSliceQuery(keyspace_, StringSerializer.get(),
                LongSerializer.get(), new JsonSerializer(GroupManagerSummaryInformation.class))
                .setKey(groupManagerDescription.getId()).setColumnFamily(CassandraUtils.GROUPMANAGERS_MONITORING_CF)
                .setRange(null, null , true, numberOfBacklogEntries);
        
        QueryResult<ColumnSlice<Long, Object>> columns = query.execute();
        
        for (HColumn<Long, Object> col : columns.get().getColumns())
        {
            GroupManagerSummaryInformation summary = (GroupManagerSummaryInformation) col.getValue();
            groupManagerDescription.getSummaryInformation().put(summary.getTimeStamp(), summary);
            log_.debug("gets monitoring data for timestamp" + summary.getTimeStamp());
        }
    }
    
    /**
     * 
     * Gets the groupManagerDescription from a row.
     * 
     * @param row       The cassandra row.
     * @return  The deserialized groupmanager. 
     */
    protected GroupManagerDescription getGroupManagerDescription(Row<String, String, String> row) 
    {
        log_.debug("Deserialize row from cassandra cluster from row id" + row.getKey());
        GroupManagerDescription groupManagerDescription = new GroupManagerDescription();
        ColumnSlice<String, String> columns = row.getColumnSlice();
           
        String hostname = columns.getColumnByName("hostname").getValue();
        boolean isAssigned = 
                columns.getColumnByName("isAssigned").getValue().equals(CassandraUtils.stringTrue) ? true : false;
        
        JsonSerializer heartbeatAddressSerializer = new JsonSerializer(NetworkAddress.class);
        NetworkAddress heartbeatAddress = (NetworkAddress) heartbeatAddressSerializer
                .fromString(columns.getColumnByName("heartbeatAddress").getValue());
        
        JsonSerializer listenSettingsSerializer = new JsonSerializer(ListenSettings.class);
        ListenSettings listenSettings = (ListenSettings) listenSettingsSerializer
                .fromString(columns.getColumnByName("listenSettings").getValue());
        
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
     * @param isActiveOnly                  True if only active requested.
     * @param numberOfMonitoringEntries     The number of monitoring entries
     */
    protected void fillWithLocalControllers(
            GroupManagerDescription groupManager, 
            boolean isActiveOnly,
            int numberOfMonitoringEntries)
    {
        ArrayList<LocalControllerDescription> localControllers = 
                getLocalControllerDescriptionsOnly(
                        groupManager.getId(),
                        null ,
                        -1,
                        numberOfMonitoringEntries,
                        isActiveOnly,
                        true);
        
        groupManager.setLocalControllersFromArray(localControllers);
        
    }
    
    /**
     * 
     * Fills the hashmap with virtualmachines.
     * 
     * @param groupManagerId                The group manager id.
     * @param localControllers              The localcontrollers.
     * @param numberOfMonitoringEntries     The number of monitoring entries.
     */
    protected void fillWithVirtualMachines(
            String groupManagerId,
            ArrayList<LocalControllerDescription> localControllers,
            int numberOfMonitoringEntries)
    {
        if (localControllers.size() == 0)
        {
            log_.debug("No Local controllers assigned to this group manager");
            return;
        }
        // arraylist to hashmap
        HashMap<String, LocalControllerDescription> localControllersMap =
                new HashMap<String, LocalControllerDescription>();
        for (LocalControllerDescription localController : localControllers)
        {
            localControllersMap.put(localController.getId(), localController);
        }
        
        ArrayList<VirtualMachineMetaData> virtualMachines = 
                getVirtualMachineDescriptionsOnly(groupManagerId, null, null, -1, numberOfMonitoringEntries, true);
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            String localControllerId = virtualMachine.getVirtualMachineLocation().getLocalControllerId();
            String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            if (!localControllersMap.containsKey(localControllerId))
            {
                log_.error(
                        String.format("groupmanager %s and doesn't manage localcontroller %s",
                                groupManagerId,
                                localControllerId
                                ));
                continue;
            }
            log_.debug(
                    String.format("Add virtual machine %s to group manager %s description",
                            virtualMachineId,
                            groupManagerId));
            localControllersMap.get(localControllerId)
                .getVirtualMachineMetaData()
                .put(virtualMachineId, virtualMachine);
        }
    }
    
    /**
     * 
     * Fills the group manager description with the associated virtual machines.
     * 
     * 
     * @param groupManager                  GroupManager description to fill.
     * @param numberOfMonitoringEntries     Number of Monitoring entries.
     */
    protected void fillWithVirtualMachines(GroupManagerDescription groupManager, int numberOfMonitoringEntries)
    {
       fillWithVirtualMachines(groupManager.getId(), groupManager.getLocalControllers(), numberOfMonitoringEntries);
    }
    
    
    /**
     * Fill the localcontroller assigned to a groupmanager with virtual machines.
     * 
     * @param groupManagerId                The groupManagerId.
     * @param localControllers              The localcontrollers to fill.
     * @param numberOfMonitoringEntries     The number of monitoring entries.
     */
    private void fillWithVirtualMachines(
            String groupManagerId,
            HashMap<String, LocalControllerDescription> localControllers,
            int numberOfMonitoringEntries)
    {
        if (localControllers.size() == 0)
        {
            log_.debug("No Local controllers assigned to this group manager");
            return;
        }
 
        ArrayList<VirtualMachineMetaData> virtualMachines = 
                getVirtualMachineDescriptionsOnly(groupManagerId, null, null, -1, numberOfMonitoringEntries, true);
        
        for (VirtualMachineMetaData virtualMachine : virtualMachines)
        {
            String localControllerId = virtualMachine.getVirtualMachineLocation().getLocalControllerId();
            String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            if (!localControllers.containsKey(localControllerId))
            {
                log_.error(String.format("Groupmanager %s which doesn't manage localcontroller %s", 
                        groupManagerId,
                        localControllerId
                        ));
                continue;
            }
            
            log_.debug(
                    String.format(
                            "Add virtual machine %s to group manager %s description",
                            virtualMachineId,
                            groupManagerId));
            
            localControllers.get(localControllerId).getVirtualMachineMetaData().put(virtualMachineId, virtualMachine);
        }
    }


    /**
     * 
     * Fills the localcontroller with its virtual machines.
     * 
     * @param localController               The localcontroller description.
     * @param numberOfMonitoringEntries     Number of monitoring entries.
     */
    protected void fillWithVirtualMachines(LocalControllerDescription localController, int numberOfMonitoringEntries)
    {
       String localControllerId = localController.getId();
       ArrayList<VirtualMachineMetaData> virtualMachines =
               getVirtualMachineDescriptionsOnly(null, localControllerId, null, -1, numberOfMonitoringEntries, true);
       for (VirtualMachineMetaData virtualMachine: virtualMachines)
       {
           String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
           log_.debug(
                   String.format(
                           "Add virtual machine %s to localController %s description",
                           virtualMachineId,
                           localControllerId));
           
           localController.getVirtualMachineMetaData().put(virtualMachineId, virtualMachine);
       }
    }
    
    /**
     * 
     * Gets the localcontroller descriptions from the cassandra cluster.
     * 
     * @param groupManagerId                The groupmanager id.
     * @param numberOfMonitoringEntries     The number of monitoring entries.
     * @param isActiveOnly                  True if only active requested.  
     * @param withVirtualMachines           True if localcontrollers should be filled with its virtualmachines
     * @return  the list of localcontrollers.
     */
    protected ArrayList<LocalControllerDescription> getLocalControllerDescriptionsCassandra(
            String groupManagerId,
            int numberOfMonitoringEntries,
            boolean isActiveOnly,
            boolean withVirtualMachines)
    {
        
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
     * @return  The list of localcontroller description.
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
        if (groupManagerId != null && !groupManagerId.equals(""))
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
            
            localControllers.add(localController);
        }

        return localControllers;  
    }
   
    /**
     * 
     * Fills the virtual machine meta data with monitoring datas.
     * 
     * @param virtualMachine                The virtual machine meta data to fill
     * @param numberOfMonitoringEntries     the number of monitoring entries to fecth.
     */
    protected void fillVirtualMachineMonitoringData(
            VirtualMachineMetaData virtualMachine,
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
            VirtualMachineMonitoringData monitoring = (VirtualMachineMonitoringData) col.getValue();
            virtualMachine.getUsedCapacity().put(monitoring.getTimeStamp(), monitoring);
            log_.debug("gets monitoring data for timestamp " + monitoring.getTimeStamp());
        }
    }
    /**
     * 
     * Gets the virtual machine meta data from a cassandra row.
     * 
     * @param row       cassandra row.
     * @return  the deserialized virtual machine meta data.
     */
    protected VirtualMachineMetaData getVirtualMachineDescriptionOnly(Row<String, String, String> row)
    {
        log_.debug("Deserialize virtual machine row from cassandra cluster from rwo id" + row.getKey());
        VirtualMachineMetaData virtualMachineMetaData = new VirtualMachineMetaData();
        ColumnSlice<String, String> columns = row.getColumnSlice();
        
        boolean isAssigned = 
                columns.getColumnByName("isAssigned").getValue().equals(CassandraUtils.stringTrue) ? true : false;
        String ipAddress = columns.getColumnByName("ipAddress").getValue();
        String xmlRepresentation = columns.getColumnByName("xmlRepresentation").getValue();
        VirtualMachineStatus status = VirtualMachineStatus.valueOf(columns.getColumnByName("status").getValue());
        VirtualMachineErrorCode errorCode = 
                VirtualMachineErrorCode.valueOf(columns.getColumnByName("errorCode").getValue());
        JsonSerializer locationSerializer = new JsonSerializer(VirtualMachineLocation.class);
        VirtualMachineLocation location = (VirtualMachineLocation) locationSerializer
                .fromString(columns.getColumnByName("location").getValue());
        JsonSerializer requestedCapacitySerializer = new JsonSerializer(ArrayList.class);
        @SuppressWarnings("unchecked")
        ArrayList<Double> requestedCapacity = (ArrayList<Double>) requestedCapacitySerializer
        .fromString(columns.getColumnByName("requestedCapacity").getValue());
        
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
     * @return  true iff the group manager has been dropped.
     */
    protected boolean dropGroupManager(String groupManagerId, boolean withLocalControllers, boolean withVirtualMachines)
    {
        GroupManagerDescription groupManager = getGroupManagerDescriptionCassandra(
                groupManagerId,
                0,
                true,
                true,
                true,
                0);
        
        if (groupManager == null)
        {
            log_.debug("Unable to find the group manager " + groupManagerId);
            return false;
        }
        boolean isGroupManagerDropped = 
                CassandraUtils.drop(keyspace_, Arrays.asList(groupManager.getId()), CassandraUtils.GROUPMANAGERS_CF);
        if (!isGroupManagerDropped)
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
            for (LocalControllerDescription localController : localControllers.values())
            {
                localControllerToRemove.add(localController.getId());
                mappingToRemove.add(localController.getControlDataAddress().toString());
                if (withVirtualMachines)
                {
                    virtualMachineToRemove.addAll(localController.getVirtualMachineMetaData().keySet());
                }

            }
          // don't drop but unassign
          boolean isLocalControllerDropped = 
                  CassandraUtils.unassignNodes(keyspace_, localControllerToRemove, CassandraUtils.LOCALCONTROLLERS_CF);
          if (!isLocalControllerDropped)
          {
              log_.error("Unable to remove the assigned localcontrollers");
              return false;
          }
          
          // don't drop but unassign
          boolean isVirtualMachineDropped = 
                  CassandraUtils.unassignNodes(keyspace_, virtualMachineToRemove, CassandraUtils.VIRTUALMACHINES_CF);
          if (!isVirtualMachineDropped)
          {
              log_.error("Unable to remove the virtual machines");
              return false;
          }
          
        }
        
        return true;
    }
                    
     
    /**
     * 
     *  Add (serialize) virtual machine to cassandra. 
     * 
     * @param virtualMachineMetaData    The virtual machine meta data to serialize.
     * @return   True iff everything is ok.
     */
    protected boolean addVirtualMachineCassandra(VirtualMachineMetaData virtualMachineMetaData)
    {
        try
        {
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
        catch (Exception exception)
        {
            log_.error("Unable to add to the repository : " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * 
     * Add (serialize) a local controller to cassandra.
     * 
     * @param groupManagerId    The group manager Id.
     * @param description       The local controller description.  
     * @return  true iff everything is ok.
     */
    protected boolean addLocalControllerDescriptionCassandra(
            String groupManagerId, LocalControllerDescription description)
    {
        int ttl = 600000000;
        
        try
        {
            String id = description.getId();
            String hostname = description.getHostname();
            HypervisorSettings hypervisorSettings = description.getHypervisorSettings();
            ArrayList<Double> totalCapacity = description.getTotalCapacity();
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
        catch (Exception exception)
        {
            log_.error("Unable to add to the repository : " + exception.getMessage());
            return false;
        }
        return true;
    }
    
    
    /**
     * 
     * Add (serialize) a group manager to cassandra.
     * 
     * @param description       The group manager description.
     * @param isGroupLeader     True iff it is the group leader.
     * @param isAssigned        True iff it is assigned
     * @return  True iff is ok.
     */
    protected boolean addGroupManagerDescriptionCassandra(
            GroupManagerDescription description, boolean isGroupLeader, boolean isAssigned)
    {
        int ttl = 600000000;
        
        Mutator<String> mutator = HFactory.createMutator(keyspace_, StringSerializer.get());
        try
        {
            String id = description.getId();
            String hostname = description.getHostname();
            ListenSettings listenSettings = description.getListenSettings();
            NetworkAddress heartbeatAddress = description.getHeartbeatAddress();
            
            mutator.addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("hostname", hostname, ttl, StringSerializer.get(), StringSerializer.get()))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("listenSettings", listenSettings, ttl,  StringSerializer.get(), new JsonSerializer(ListenSettings.class)))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("heartbeatAddress", heartbeatAddress, ttl,  StringSerializer.get(), new JsonSerializer(NetworkAddress.class)))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("isAssigned", isAssigned, ttl, StringSerializer.get(),   new BooleanSerializer()))
                   .addInsertion(id, CassandraUtils.GROUPMANAGERS_CF, HFactory.createColumn("isGroupLeader", isGroupLeader, ttl, StringSerializer.get(),  new BooleanSerializer()));
            
            MutationResult result = mutator.execute();
            log_.debug(String.format("Insertion done in %d", result.getExecutionTimeMicro()));
            
            
        }
        catch (Exception exception)
        {
            log_.error("Unable to add to the repository : " + exception.getMessage());
            return false;    
        }
        return true;
    }
    
    
    /**
     * 
     * Get a virtual machine.
     * 
     * @param virtualMachineId              The virtual machine id.
     * @param numberOfMonitoringEntries     The number of monitoring entries to fetch.
     * @return  The virtual machine meta data.
     */
    protected VirtualMachineMetaData getVirtualMachineMetaDataCassandra(
            String virtualMachineId, int numberOfMonitoringEntries)
    {
        return getVirtualMachineDescriptionOnly(virtualMachineId, numberOfMonitoringEntries);
        // no children to fecth.
    }
    

    /**
     * 
     * Gets the groupmanager descriptions.
     * 
     * @param firstGroupManagerId       First groupmanager to fetch.
     * @param limit                     Limit.  
     * @param assignedOnly              True will return assigned group manager only.
     * @param numberOfBacklogEntries    Number of Back log entries to fetch.
     * @param toExclude                 id to exclude.
     * @return  List of group manager description.
     */
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

        log_.debug(String.format("Returning %d groupmanagers", groupManagers.size()));
        return groupManagers;
    }
    
    /**
     * 
     * Gets the virtual machine descriptions.
     * 
     * @param virtualMachineId              The virtualMachine Id.
     * @param numberOfMonitoringEntries     The number of monitoring entries to fecth.
     * @return  The virtual machine meta data.
     */
    protected VirtualMachineMetaData getVirtualMachineDescriptionOnly(
            String virtualMachineId, int numberOfMonitoringEntries)
    {
        Guard.check(virtualMachineId, numberOfMonitoringEntries);       
        
        ArrayList<VirtualMachineMetaData> virtualMachines = 
                getVirtualMachineDescriptionsOnly(null, null, virtualMachineId, 1, numberOfMonitoringEntries, true);
        
        
        if ((virtualMachines == null) || virtualMachines.size() != 1)
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
    
    /**
     * 
     * Gets virtual machine metadatas from cassandra cluster. 
     * 
     * @param groupManagerId                The groupmanager id.
     * @param localControllerId             The localcontroller id.
     * @param virtualMachineStart           The first virtual machine to fecth        
     * @param limit                         Limit.
     * @param numberOfMonitoringEntries     Number Of monitoring entries to fetch.
     * @param onlyAssigned                  True if only assigned requested.
     * @return  The virtual machine meta datas list.
     */
    protected ArrayList<VirtualMachineMetaData> getVirtualMachineDescriptionsOnly(
            String groupManagerId, 
            String localControllerId,
            String virtualMachineStart,
            int limit,
            int numberOfMonitoringEntries,
            boolean onlyAssigned
            )
    {
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

        return virtualMachines;  
    }
    
    /**
     * 
     * Gets the local controller id from the mapping column family.
     * 
     * @param contactInformation        the network address
     * @return  the local controller id.
     */
    protected String getLocalControllerId(NetworkAddress contactInformation)
    {
        HColumnFamily<String, String> mappingColumnFamily =
                new HColumnFamilyImpl<String, String>(
                        getKeyspace(),
                        CassandraUtils.LOCALCONTROLLERS_MAPPING_CF,
                        StringSerializer.get(),
                        StringSerializer.get());
        
        mappingColumnFamily.addKey(contactInformation.toString());
        mappingColumnFamily.addColumnName("id");
        
        String localControllerId = mappingColumnFamily.getValue("id", StringSerializer.get());
        if (localControllerId == null)
        {
            log_.debug("no id - address mapping exists for this local Controller");
            return null;
        }
        
        return localControllerId;
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
    public Keyspace getKeyspace()
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
