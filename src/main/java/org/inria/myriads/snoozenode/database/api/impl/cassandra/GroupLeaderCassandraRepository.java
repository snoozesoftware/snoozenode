package org.inria.myriads.snoozenode.database.api.impl.cassandra;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.cassandra.service.HColumnFamilyImpl;
import me.prettyprint.hector.api.HColumnFamily;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.SliceQuery;


import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.AssignedGroupManager;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.CassandraUtils;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * 
 * GroupLeader cassandra repository.
 * 
 * @author msimonin
 *
 */
public class GroupLeaderCassandraRepository extends CassandraRepository implements GroupLeaderRepository
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupLeaderCassandraRepository.class);
    
    
    /** Pool of Ips. */
    private List<String> ipAddress_;

    /** TTL. */
    private int ttl_;

    /** Group leader description.*/
    private GroupManagerDescription groupLeaderDescription_;

    
    /**
     * 
     * Constructor.
     * 
     * @param groupLeaderDescription    The group manager description.
     * @param virtualMachineSubnets     The virtual machine subnets.
     * @param maxCapacity               The max capacity.
     * @param hosts                     The cassandra hosts to connect to.
     */
    public GroupLeaderCassandraRepository(
            GroupManagerDescription groupLeaderDescription,
            String[] virtualMachineSubnets,
            int maxCapacity,
            String hosts
            ) 
    {        
        super(hosts);
        unassignNodes();
        ipAddress_ = generateAddressPool(virtualMachineSubnets);
        populateAddressPool(ipAddress_);
        ttl_ = maxCapacity;
        groupLeaderDescription_ = groupLeaderDescription;
        addGroupManagerDescription(groupLeaderDescription, true, true);
        log_.debug("Connected to cassandra");
    }

    /**
     * Unassign all nodes.
     */
    protected void unassignNodes()
    {
        CassandraUtils.unassignNodes(getKeyspace(), CassandraUtils.GROUPMANAGERS_CF);
        CassandraUtils.unassignNodes(getKeyspace(), CassandraUtils.LOCALCONTROLLERS_CF);
        CassandraUtils.unassignNodes(getKeyspace(), CassandraUtils.VIRTUALMACHINES_CF);
    }
    
    
    
    
    /**
     * 
     * Add group manager/leader description.
     *  
     * @param description       The description.
     * @param isGroupLeader     True if it is the groupLeader.
     * @param isAssigned        True if is assigned.
     * @return                  True iff everything is ok.             
     */
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
        // T ODO : batch this.
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
     * Adds a group manager description. 
     * 
     * @param description   The group manager description
     * @return              true if added, false otherwise
     */
    public boolean addGroupManagerDescription(GroupManagerDescription description)
    {
       return addGroupManagerDescription(description, false, true);
    }

    /**
     * Returns the group manager descriptions.
     * 
     * @param numberOfBacklogEntries    The number of backlog entries
     * @return                          The group manager descriptions
     */
    public ArrayList<GroupManagerDescription> getGroupManagerDescriptions(
            int numberOfBacklogEntries) 
    {
        log_.debug("Gets all the group managers");
        
        ArrayList<GroupManagerDescription> groupManagers = new ArrayList<GroupManagerDescription>();
        
        groupManagers = getGroupManagerDescriptionsOnly(
                null, 
                -1, 
                true, 
                numberOfBacklogEntries, 
                Arrays.asList(groupLeaderDescription_.getId()));
        
        return groupManagers;

    }

    
    /**
     * Returns the group manager description.
     * 
     * @param groupManagerId            The group manager id
     * @param numberOfBacklogEntries    The number of backlog entries
     * @return                          The group manager description
     */
    public GroupManagerDescription getGroupManagerDescription(String groupManagerId, int numberOfBacklogEntries) 
    {
       return getGroupManagerDescriptionOnly(groupManagerId, numberOfBacklogEntries);
    }

    
    /** 
     * Adds group manager data.
     * 
     * @param groupManagerId            The group manager identifier
     * @param summary                   The group manager summary information
     */
    public void addGroupManagerSummaryInformation(String groupManagerId,
            GroupManagerSummaryInformation summary) 
    {
        log_.debug(String.format("Adding summary information for groupmanager %s in the database", groupManagerId));
        
        StringSerializer stringSerializer = new StringSerializer();
        Mutator<String> mutator = HFactory.createMutator(getKeyspace(), stringSerializer);
        

        try
        {            
            mutator.addInsertion(groupManagerId, CassandraUtils.GROUPMANAGERS_MONITORING_CF, HFactory.createColumn(
                    summary.getTimeStamp(), 
                    summary,
                    ttl_,
                    new LongSerializer(), 
                    new JsonSerializer(GroupManagerSummaryInformation.class)));
            MutationResult result = mutator.execute();
            log_.debug(String.format("Insertion done in %d", result.getExecutionTimeMicro()));
        }
        catch (Exception exception)
        {
            log_.error("Unable to add groupmanager summary to the repository : " + exception.getMessage());
            exception.printStackTrace();
            
        }
    }

    /** 
     * Drops a group manager. 
     * 
     * @param groupManagerId       The group manager identifier
     * @return                     true if everything ok, false otherwise
     */
    public boolean dropGroupManager(String groupManagerId) 
    {
        log_.debug(String.format("Remove group manager %s from the cassandra cluster", groupManagerId));
        try
        {            
            dropGroupManager(groupManagerId, true, true);
        }
        catch (Exception e)
        {
            log_.error(String.format("Unable to remove group manager %s from the cassandra cluster", groupManagerId));
            return false;
        }
        return true;
    }

    
    /**
     * Adds the IP address.
     * 
     * @param ipAddress     The ip address
     * @return              true if everything ok, false otherwise
     */
    public boolean addIpAddress(String ipAddress) 
    {
        boolean isAdded = CassandraUtils.addStringColumn(
                getKeyspace(),
                CassandraUtils.IPS_ROW_KEY, 
                CassandraUtils.IPSPOOL_CF,
                ipAddress,
                ""); 
        return isAdded;
    }

    /**
     * Removes IP address from the pool.
     * 
     * @param ipAddress     The ip address
     * @return              true if everything ok, false otherwise
     */
    public boolean removeIpAddress(String ipAddress) 
    {
        log_.debug(String.format("Remove ip %s from ips pool", ipAddress));
        try
        {
            StringSerializer stringSerializer = new StringSerializer();
            Mutator<String> mutator = HFactory.createMutator(getKeyspace(), stringSerializer);
            mutator.delete("0", CassandraUtils.IPSPOOL_CF, ipAddress, StringSerializer.get());
            mutator.execute();
        }
        catch (Exception e)
        {
            log_.error(String.format("Unable to remove ip  %s from the ips pool", ipAddress));
            return false;
        }
        return true;
    }

    /**
     * Get the next free IP address.
     * 
     * @return     The next free ip address
     */
    public String getFreeIpAddress() 
    {
        SliceQuery<String, String, String> query = HFactory.createSliceQuery(getKeyspace(), StringSerializer.get(),
                StringSerializer.get(), StringSerializer.get()).
                setColumnFamily(CassandraUtils.IPSPOOL_CF).
                setKey("0").
                setRange("", "", false, 1);
        
        ColumnSliceIterator<String, String, String> iterator = 
                new ColumnSliceIterator<String, String, String>(query, null, "\uFFFF", false);       
        
        while (iterator.hasNext())
        {
            HColumn<String, String> column = iterator.next();
            return column.getName();
        }
        return null;
    }

    
    /**
     * 
     * Returns the local controllers list.
     * 
     * @return  The local controllers list (unused).
     */
    public ArrayList<LocalControllerDescription> getLocalControllerList() 
    {
        
        return null;
    }

    /**
     * 
     * Gets the group manager assigned to the localcontroller identified by its contact information.
     * 
     * @param contactInformation        the contact address/port of the local controller.
     * @return                          The assigned group manager or null if none is found.
     */
    public AssignedGroupManager getAssignedGroupManager(
            NetworkAddress contactInformation) 
    {
        //look in the localcontroller mapping column family (key is contactInformation)
        //look in the localcontroller column family (key is the uuid)
        try
        {
            
            String localControllerId = getLocalControllerId(contactInformation);
            log_.debug("Found a previous local controller with this contact information");
            if (localControllerId == null)
            {
                log_.debug("no id - address mapping exists for this local Controller");
            }
            
            
            HColumnFamily<String, String> localControllerColumnFamily = new HColumnFamilyImpl<String, String>(
                            getKeyspace(), 
                            CassandraUtils.LOCALCONTROLLERS_CF, 
                            StringSerializer.get(), 
                            StringSerializer.get());
                localControllerColumnFamily.addKey(localControllerId);
                localControllerColumnFamily.addColumnName("groupmanager")
                .addColumnName("isAssigned")
                .addColumnName("id");
                
                String groupManagerId  = localControllerColumnFamily.getValue("groupManager", StringSerializer.get());
                boolean isAssigned = localControllerColumnFamily.getValue("isAssigned", BooleanSerializer.get());
               
                AssignedGroupManager assignedGroupManager = null;
                if (isAssigned && groupManagerId != null)
                {
                    GroupManagerDescription assignedGroupManagerDescription = 
                            getGroupManagerDescription(groupManagerId, 0);
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

    
    /**
     * Given a local controller location updates the location with the proper groupmanager.
     * @param location          The location.
     * @return                  True if everything is ok.
     */
    public boolean updateLocation(VirtualMachineLocation location)
    {
        String localControllerId = location.getLocalControllerId();
        AssignedGroupManager lookup = getAssignedGroupManager(localControllerId);
        if (lookup == null)
        {
            return false;
        }
        
        location.setGroupManagerId(lookup.getGroupManager().getId());
        location.setGroupManagerControlDataAddress(
                lookup.getGroupManager().getListenSettings().getControlDataAddress());
        return true;
    }

    /**
     * 
     * Gets the group manager assigned to the localcontroller identified by its contact information.
     * 
     * @param localControllerId         The local controller id.
     * @return                          The assigned group manager or null if none is found.
     */
    private AssignedGroupManager getAssignedGroupManager(
            String localControllerId) 
    {
        return null;
        
    }
    
    /**
     * 
     * Gets the localController description.
     * 
     * @param localControllerId     The local controller id.
     * @return                      The local controller description.
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
     * @param ipAddress     The ip Address to populate.
     */
    protected void populateAddressPool(List<String> ipAddress)
    {
        // check if 
        boolean isAlreadyPopulated = CassandraUtils.checkForRow(
                getKeyspace(), 
                CassandraUtils.IPSPOOL_CF,
                CassandraUtils.IPS_ROW_KEY);
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
}
