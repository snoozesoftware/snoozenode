package org.inria.myriads.snoozenode.database.api.impl.cassandra;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;



import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.cassandra.service.HColumnFamilyImpl;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HColumnFamily;
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


import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.ListenSettings;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.AssignedGroupManager;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class GroupLeaderCassandraRepository extends CassandraRepository implements GroupLeaderRepository
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupLeaderCassandraRepository.class);
    
    
    /** Pool of Ips. */
    private List<String> ipAddress_;

    /** maxCapacity ?.*/
    private int maxCapacity_;

    /** TTL. */
    private int ttl_;

    /** Leader Description.*/
    private GroupManagerDescription groupLeader_;
    
    /**
     * @throws IOException 
     * @throws JsonMappingException 
     * @throws JsonParseException 
     * 
     */
    public GroupLeaderCassandraRepository(
            GroupManagerDescription groupLeaderDescription,
            String[] virtualMachineSubnets,
            int maxCapacity,
            String hosts
            ) 
    {        
        super(hosts);
        // Truncate all column family
        // Another solution would be to unassign every entry.
        unassignNodes();
        //clear();
        ipAddress_ = generateAddressPool(virtualMachineSubnets);
        populateAddressPool(ipAddress_);
        maxCapacity_ = maxCapacity;
        ttl_ = 60 ;
        groupLeader_ = groupLeaderDescription;
        addGroupManagerDescription(groupLeaderDescription, true, true);
        log_.debug("Connected to cassandra");
    }

    /**
     * Unassign all nodes.
     */
    protected void unassignNodes()
    {
        CassandraUtils.unassignNodes(keyspace_, CassandraUtils.GROUPMANAGERS_CF);
        CassandraUtils.unassignNodes(keyspace_, CassandraUtils.LOCALCONTROLLERS_CF);
        CassandraUtils.unassignNodes(keyspace_, CassandraUtils.VIRTUALMACHINES_CF);
    }
    
   
        
  

    /**
     * 
     * Empty Construtor.
     * 
     */
    public GroupLeaderCassandraRepository()
    {
        super("localhost:9160");
        groupLeader_ = new GroupManagerDescription();
        ttl_ = 600;
    }
    
    
    private boolean addGroupManagerDescription(GroupManagerDescription description, 
                                               boolean isGroupLeader, 
                                               boolean isAssigned
            )
    {
        log_.debug("Adding a new GroupManager decription");
        boolean isGroupManagerAdded = addGroupManagerDescriptionCassandra(description, isGroupLeader, isAssigned);
        if (!isGroupManagerAdded)
        {
            log_.error("Unable to add the groupmanager %s to the repository");
        }
        
        // TODO : batch this.
        // add associated local controller
        log_.debug("Adding associated localController for groupManager " + description.getId());
        for (LocalControllerDescription localController : description.getLocalControllers().values())
        {
            addLocalControllerDescriptionCassandra(description.getId(), localController);
            //add associated virtualMachine
            log_.debug("Adding associated virtualMachine for groupManager " + description.getId());
            for (VirtualMachineMetaData virtualMachine : localController.getVirtualMachineMetaData().values())
            {
                addVirtualMachineCassandra(virtualMachine);
            }
        }
        
        return true;
    }
     
    /**
     * 
     * Adds the groupmanager to the repository.
     * 
     * 
     */
    public boolean addGroupManagerDescription(GroupManagerDescription description)
    {
       return addGroupManagerDescription(description, false, true);
    }

    public ArrayList<GroupManagerDescription> getGroupManagerDescriptions(
            int numberOfBacklogEntries) 
    {
        log_.debug("Gets all the group managers");
        
        ArrayList<GroupManagerDescription> groupManagers = new ArrayList<GroupManagerDescription>();
        
        int row_count = 100;

        String last_key = null;

        //retrieve only assigned one an not gl...
        while (true) {
            RowQueryIterator rowQueryIterator = new RowQueryIterator(
                    keyspace_, CassandraUtils.GROUPMANAGERS_CF,
                    null, // start
                    null, // end
                    row_count); // rows to fetch 

            @SuppressWarnings("unchecked")
            Iterator<Row<String, String, String>> rowsIterator = rowQueryIterator.iterator();
            
            if (last_key != null && rowsIterator != null) rowsIterator.next();   

            while (rowsIterator.hasNext()) {
              Row<String, String, String> row = rowsIterator.next();
              last_key = row.getKey();
              
              if (row.getColumnSlice().getColumns().isEmpty()) {
                continue;
              }
              
              GroupManagerDescription groupManager = getGroupManagerDescription(row);
              if (groupManager == null || groupManager.getId().equals(groupLeader_.getId()))
              {
                  //skip the group leader.
                  continue;
              }
              
              if (numberOfBacklogEntries > 0)
              {
                  fillGroupManagerSummaryInformation(groupManager, numberOfBacklogEntries);
              }
              groupManagers.add(groupManager);
            }

            if (rowQueryIterator.getCount() < row_count)
                break;
        }
        return groupManagers;
    }

    
    

    public GroupManagerDescription getGroupManagerDescription(String groupManagerId, int numberOfBacklogEntries) 
    {
        return getGroupManagerDescriptionCassandra(groupManagerId, numberOfBacklogEntries,false,false,false,0);
    }

    

    public void addGroupManagerSummaryInformation(String groupManagerId,
            GroupManagerSummaryInformation summary) 
    {
        log_.debug(String.format("Adding summary information for groupmanager %s in the database",groupManagerId));
        
        // check if the gm exist ? 
        //        GroupManagerDescription groupManager = getGroupManagerDescription(groupManagerId,0);
        //        if (groupManager == null)
        //        {
        //            log_.error("No groupmanager stored with this id ... dropping summary");
        //            return;
        //        }
        
        
        StringSerializer stringSerializer = new StringSerializer();
        Mutator<String> mutator = HFactory.createMutator(keyspace_, stringSerializer);
        try{            
           
            
            mutator.addInsertion(groupManagerId, CassandraUtils.GROUPMANAGERS_MONITORING_CF, HFactory.createColumn(
                    summary.getTimeStamp(), 
                    summary,
                    ttl_,
                    new LongSerializer(), 
                    new JsonSerializer(GroupManagerSummaryInformation.class))) ;
            MutationResult result = mutator.execute();
            log_.debug(String.format("Insertion done in %d", result.getExecutionTimeMicro()));
        }
        catch(Exception exception)
        {
            log_.error("Unable to add groupmanager summary to the repository : " + exception.getMessage());
            exception.printStackTrace();
            
        }
    }

    public boolean dropGroupManager(String groupManagerId) 
    {
        log_.debug(String.format("Remove group manager %s from the cassandra cluster", groupManagerId));
        try
        {            
            dropGroupManager(groupManagerId,true,true);
        }
        catch(Exception e)
        {
            log_.error(String.format("Unable to remove group manager %s from the cassandra cluster", groupManagerId));
            return false;
        }
        return true;
    }

    public boolean addIpAddress(String ipAddress) 
    {
        boolean isAdded = CassandraUtils.addStringColumn(keyspace_,CassandraUtils.IPS_ROW_KEY, CassandraUtils.IPSPOOL_CF,ipAddress,""); 
        return isAdded;
    }

    public boolean removeIpAddress(String ipAddress) 
    {
        log_.debug(String.format("Remove ip %s from ips pool", ipAddress));
        try
        {
            StringSerializer stringSerializer = new StringSerializer();
            Mutator<String> mutator = HFactory.createMutator(keyspace_, stringSerializer);
            mutator.delete("0", CassandraUtils.IPSPOOL_CF, ipAddress, StringSerializer.get());
            mutator.execute();
        }
        catch(Exception e)
        {
            log_.error(String.format("Unable to remove ip  %s from the ips pool", ipAddress));
            return false;
        }
        return true;
    }

    public String getFreeIpAddress() 
    {
        SliceQuery<String, String, String> query = HFactory.createSliceQuery(keyspace_, StringSerializer.get(),
                StringSerializer.get(), StringSerializer.get()).
                setColumnFamily(CassandraUtils.IPSPOOL_CF).
                setKey("0").
                setRange("", "", false, 1)
                ;
        
        ColumnSliceIterator<String, String, String> iterator = 
                new ColumnSliceIterator<String, String, String>(query, null, "\uFFFF", false);       
        
        while (iterator.hasNext())
        {
            HColumn<String, String> column = iterator.next();
            return column.getName();
        }
        return null;
    }

    public ArrayList<LocalControllerDescription> getLocalControllerList() 
    {
        
        return null;
    }

    /**
     * Lookup
     */
    public AssignedGroupManager getAssignedGroupManager(
            NetworkAddress contactInformation) 
    {
        //look in the localcontroller mapping column family (key is contactInformation)
        //look in the localcontroller column family (key is the uuid)
        try
        {

            StringSerializer stringSerializer = new StringSerializer();
            
            HColumnFamily<String, String> mappingColumnFamily =
                    new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_MAPPING_CF, stringSerializer, stringSerializer);
            mappingColumnFamily.addKey(contactInformation.toString());
            mappingColumnFamily.addColumnName("id");
            
            String localControllerId = mappingColumnFamily.getValue("id", stringSerializer);
            if (localControllerId == null)
            {
                log_.warn("no id - address mapping exists for this local Controller");
                return null;
            }
            // we got the corresponding lc id.
            
            HColumnFamily<String, String> localControllerColumnFamily =
                    new HColumnFamilyImpl<String, String>(keyspace_, CassandraUtils.LOCALCONTROLLERS_CF, stringSerializer, stringSerializer);
                localControllerColumnFamily.addKey(localControllerId);
                localControllerColumnFamily.addColumnName("groupmanager")
                .addColumnName("isAssigned")
                .addColumnName("id");
                
                String groupManagerId  = localControllerColumnFamily.getValue("groupmanager", StringSerializer.get());
                boolean isAssigned = localControllerColumnFamily.getValue("isAssigned", BooleanSerializer.get());
               
                AssignedGroupManager assignedGroupManager = null ;
                if (isAssigned && groupManagerId != null)
                {
                    GroupManagerDescription assignedGroupManagerDescription = getGroupManagerDescription(groupManagerId, 0);
                    if (assignedGroupManagerDescription != null)
                    {
                        log_.debug("Found a previous assigned group manager");
                        assignedGroupManager = new AssignedGroupManager();
                        assignedGroupManager.setGroupManager(assignedGroupManagerDescription);
                        assignedGroupManager.setLocalControllerId(localControllerId);
                    }
                }
                else
                {
                    log_.debug("No assigned to a groupManager found");
                }
                return assignedGroupManager;
        }
        catch (Exception exception)
        {
            log_.warn("Lookup failed" + exception.getMessage());
            return null;
        }
    }

    public boolean updateLocation(VirtualMachineLocation location) 
    {
        String localControllerId = location.getLocalControllerId();
        AssignedGroupManager lookup = getAssignedGroupManager(localControllerId);
        if (lookup == null)
        {
            return false;
        }
        
        location.setGroupManagerId(lookup.getGroupManager().getId());
        location.setGroupManagerControlDataAddress(lookup.getGroupManager().getListenSettings().getControlDataAddress());
        return true;
    }

    private AssignedGroupManager getAssignedGroupManager(
            String localControllerId) 
    {
        return null;
        
    }
    
    /**
     * 
     * Should be nice if localControllerId = controlDataAddress.toString (fast lookup on column family)..
     * 
     */
    public LocalControllerDescription getLocalControllerDescription(String localControllerId) 
    {
        return null;
    }

    /**
     * Generates the address pool.
     * 
     * @param virtualMachineSubnets     The virtual machine subnet
     * @return                          The list of IP addresses
     */
    protected List<String> generateAddressPool(String[] virtualMachineSubnets)
    {
        log_.debug("Generating address pool");
        List<String> addressPool = new ArrayList<String>();
        for (String virtualMachineSubnet : virtualMachineSubnets)
        {
            SubnetUtils subnetUtils = new SubnetUtils(virtualMachineSubnet);
            SubnetInfo subnetInfo = subnetUtils.getInfo(); 
            addressPool.addAll(Arrays.asList(subnetInfo.getAllAddresses()));
        }
        
        return addressPool;
    }
    
    /**
     * 
     * Populates all the ips. 
     * 
     * TODO : batch this.
     * 
     * @param ipAddress
     */
    protected void populateAddressPool(List<String> ipAddress) 
    {
        // check if 
        boolean isAlreadyPopulated = CassandraUtils.checkForRow(keyspace_, CassandraUtils.IPSPOOL_CF,CassandraUtils.IPS_ROW_KEY);
        if (isAlreadyPopulated)
        {
            log_.debug("Ips pool has been already populated...nothing to do");
            return;
        }
        log_.debug("Generating Ips");
        
        for (String address : ipAddress)
        {
            addIpAddress(address);
        }
    }
    
    /**
     *
     * Clear to repository.
     * 
     */
    protected void clear()
    {
        cluster_.truncate("snooze", CassandraUtils.GROUPMANAGERS_CF);
        cluster_.truncate("snooze", CassandraUtils.LOCALCONTROLLERS_CF);
        cluster_.truncate("snooze", CassandraUtils.LOCALCONTROLLERS_MAPPING_CF);
        cluster_.truncate("snooze", CassandraUtils.VIRTUALMACHINES_CF);
    }

}
