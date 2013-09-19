package org.inria.myriads.snoozenode.database.api.impl.cassandra;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;



import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.monitoring.VirtualMachineMonitoringData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.CassandraUtils;
import org.inria.myriads.snoozenode.database.api.impl.cassandra.utils.JsonSerializer;
import org.inria.myriads.snoozenode.database.api.impl.memory.GroupManagerMemoryRepository;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Group Manager Cassandra repository.
 * 
 * @author msimonin
 *
 */
public class GroupManagerCassandraRepository extends CassandraRepository implements GroupManagerRepository
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerCassandraRepository.class);
    
   /** List for the legacy IP addresses. */
   private List<String> legacyIpAddresses_;
    
   /** GroupManagerDescription Cache. */
   private GroupManagerMemoryRepository groupManagerCache_;
   
   /** Time to live (monitoring info).*/
   private int ttl_;
   

    /**
     * 
     * Constructor.
     * 
     * @param groupManager  The group manager description.
     * @param ttl           The ttl of column (for monitoring)
     * @param hosts         The cassandra hosts to connect to.
     */
    public GroupManagerCassandraRepository(GroupManagerDescription groupManager, int ttl, String hosts)
    {
        super(hosts);
        log_.debug("Initializing the group manager memory repository");
        legacyIpAddresses_ = new ArrayList<String>();
        groupManagerCache_ = new GroupManagerMemoryRepository(groupManager, 0);
        ttl_ = ttl;
    }


    @Override
    public String getGroupManagerId()
    {
       
       return groupManagerCache_.getGroupManagerId();
    }
    
    
    @Override
    public ArrayList<LocalControllerDescription> getLocalControllerDescriptions(int numberOfMonitoringEntries,
            boolean isActiveOnly, boolean withVirtualMachines)
    {
        log_.debug("Gets all localcontrollers");    

        ArrayList<LocalControllerDescription> localControllers = new ArrayList<LocalControllerDescription>();
        // fetch from cassandra since we only need associated ones. (should be the same)
        localControllers = getLocalControllerDescriptionsCassandra(
                getGroupManagerId(), 
                numberOfMonitoringEntries, 
                isActiveOnly,
                withVirtualMachines);
        
        if (numberOfMonitoringEntries > 0)
        {
            for (LocalControllerDescription localController : localControllers)
            {
                for (VirtualMachineMetaData virtualMachine : localController.getVirtualMachineMetaData().values())
                {
                    fillVirtualMachineMonitoringData(virtualMachine, numberOfMonitoringEntries);
                }
            }
        }
        return localControllers;   
    }
    


    @Override
    public boolean addLocalControllerDescription(LocalControllerDescription description)
    {   

        log_.debug("Adding localController Description to the cassandra cluster");
             
        boolean isAdded = addLocalControllerDescriptionCassandra(groupManagerCache_.getGroupManagerId(), description);
        // add vms
        if (!isAdded)
        {
            log_.debug("Failed to add local controller description");
            return false;
        }
        
        boolean isUpdated = updateVirtualMachineAssignments(description);
        if (!isUpdated)
        {
            log_.debug("Failed to update the virtual machine assignment set!");
            return false;
        }
                     
        //update_cache();
        description.setIsAssigned(true);
        groupManagerCache_.addLocalControllerDescription(description);
        
        log_.debug("Local controller description added successfully !!");
        
        return true;
    }
    
    /**
     * Updates the virtual machine assignment set.
     * (When a new local controller join)
     *
     * @param localController   The local controller description
     * @return                  true if everything ok, false otherwise
     */
    private boolean updateVirtualMachineAssignments(LocalControllerDescription localController) 
    {
        log_.debug(String.format("Starting to update the virtual machine assignment set for local controller: %s",
                                 localController.getId()));
        
        Map<String, VirtualMachineMetaData> metaData = localController.getVirtualMachineMetaData();
        if (metaData == null)
        {
            log_.debug("No meta data available on this local controller!");
            return false;
        }
        
        for (VirtualMachineMetaData entry : metaData.values())
        {
            // change virtualMachineLocation
            entry.getVirtualMachineLocation().setLocalControllerId(localController.getId());
            entry.getVirtualMachineLocation().setGroupManagerId(getGroupManagerId());
            //batch this...
            boolean isAdded = addVirtualMachine(entry);
            if (!isAdded)
            {
                log_.debug("Failed to add virtual machine meta data!");
                return false;
            }
        }
        
        return true;
    }



    @Override
    public boolean dropLocalController(String localControllerId, boolean forceDelete)
    {
        Guard.check(localControllerId);
        log_.debug(String.format("Removing local controller: %s, force: %s", localControllerId, forceDelete));
        
        // drop local controller and its associated virtualMachines.
        boolean isDropped = dropLocalController(localControllerId, forceDelete, true);
        if (!isDropped)
        {
            log_.error("Unable to drop the local controller");
            return false;
        }
        
        //update_cache();
        isDropped = groupManagerCache_.dropLocalController(localControllerId, forceDelete);
        return isDropped;
    }
    
    /**
     * 
     * Drops a local controller with or without its virtualmachines.
     * 
     * @param localControllerId         The local controller id.
     * @param forceDelete               For delete (passive local controller).
     * @param withVirtualMachines       True if associated virtual machines must be deleted.
     * @return                          True if everything is ok.
     */
    private boolean dropLocalController(String localControllerId, boolean forceDelete, boolean withVirtualMachines)
    {
        LocalControllerDescription localController = 
                getLocalControllerDescriptionCassandra(localControllerId, 0, true, 0);
        
        if (localController == null)
        { 
            log_.debug("unable to find the local controller " + localControllerId);
            return false;
        }
        
        if (localController.getStatus() == LocalControllerStatus.PASSIVE && !forceDelete)
        {
            log_.debug("This local controller is in PASSIVE mode! Will not delete!");
            return false;
        }

        boolean isNetworkReleased = releaseLocalControllerNetworkingInformation(localController);
        if (!isNetworkReleased)
        {
            log_.error("Unable to release the network information");
            return false;
        }
        
        boolean isLocalControllerDropped = 
                CassandraUtils.drop(
                        getKeyspace(), 
                        Arrays.asList(localController.getId()), 
                        CassandraUtils.LOCALCONTROLLERS_CF);
        
        if (!isLocalControllerDropped)
        {
            log_.error("unable to remove the local controller " + localController.getId());
            return false;
        }
        
        boolean isMappingDropped = 
                CassandraUtils.drop(
                        getKeyspace(), 
                        Arrays.asList(localController.getControlDataAddress().toString()), 
                        CassandraUtils.LOCALCONTROLLERS_MAPPING_CF);
        
        if (!isMappingDropped)
        {
            log_.error("Unable to remove the mapping for the assigned localcontroller");
            return false;
        }
        
        ArrayList<String> virtualMachineToRemove = new ArrayList<String>();
        virtualMachineToRemove.addAll(localController.getVirtualMachineMetaData().keySet());
        boolean isVirtualMachineDropped = 
                CassandraUtils.drop(
                        getKeyspace(), 
                        virtualMachineToRemove,
                        CassandraUtils.VIRTUALMACHINES_CF);

        if (!isVirtualMachineDropped)
        {
            log_.error("Unable to remove the virtual machines");
            return false;
        }
        
        return true;
    }
 


    @Override
    public void fillGroupManagerDescription(GroupManagerDescription groupManagerDescription)
    {
        // fill it from the cache.
        groupManagerDescription.setLocalControllers(groupManagerCache_.getLocalControllerDescriptions());
    }
    
    @Override
    public void addAggregatedMonitoringData(String localControllerId, List<AggregatedVirtualMachineData> aggregatedData)
    {
        // done by lc ? 
        Guard.check(aggregatedData);   
        log_.debug(String.format("Adding aggregated virtual machine monitoring data to the database for %d VMs", 
                                 aggregatedData.size()));
        
        LocalControllerDescription description = 
                groupManagerCache_.getLocalControllerDescription(localControllerId, 0, false);
        if (description == null)
        {
            log_.error("Description not found in the cache");
            return;
        }
        
        for (AggregatedVirtualMachineData aggregatedVirtualMachineData : aggregatedData) 
        {
            String virtualMachineId = aggregatedVirtualMachineData.getVirtualMachineId();
            
            VirtualMachineLocation location = new VirtualMachineLocation();
            location.setLocalControllerId(localControllerId);
            location.setVirtualMachineId(virtualMachineId);
         
            List<VirtualMachineMonitoringData> dataList = aggregatedVirtualMachineData.getMonitoringData();
            if (dataList.isEmpty())
            {
                log_.debug("The virtual machine monitoring data list is empty");
                continue;
            }
           
            Mutator<String> mutator = HFactory.createMutator(getKeyspace(), StringSerializer.get());
            try
            {
                for (VirtualMachineMonitoringData virtualMachineData : dataList) 
                {
               
                    log_.debug(String.format("Adding history data %s for virtual machine: %s",
                                             virtualMachineData.getUsedCapacity(),   
                                             virtualMachineId));
                    mutator.addInsertion(virtualMachineId, CassandraUtils.VIRTUALMACHINES_MONITORING_CF, 
                            HFactory.createColumn(
                                    virtualMachineData.getTimeStamp(),
                                    virtualMachineData,
                                    ttl_,
                                    new LongSerializer(),
                                    new JsonSerializer(VirtualMachineMonitoringData.class)
                                    ));
                }
                mutator.execute();
               
            }
            catch (Exception exception)
            {
                log_.error("Unable to add virtualmachine monitoring to the repository " + exception.getMessage());
            }
        }
        
    }
    

   

    @Override
    public ArrayList<String> getLegacyIpAddresses()
    {
        log_.debug(String.format("Returning the current list of legacy IP addresses: %s", 
                legacyIpAddresses_.toString()));
        ArrayList<String> newList = new ArrayList<String>(legacyIpAddresses_);
        legacyIpAddresses_.clear();
        return newList;
    }
    @Override
    public boolean dropVirtualMachineData(VirtualMachineLocation location)
    {
        log_.debug(String.format("Remove virtual machine %s from the cassandra cluster",
                        location.getVirtualMachineId()));
        
        
        VirtualMachineMetaData virtualMachine = getVirtualMachineMetaData(location, 0);
        if (virtualMachine == null)
        {
            log_.debug("No meta information exists for this virtual machine!");
            return true;
        }
        
        String ipAddress = virtualMachine.getIpAddress();
        if (ipAddress == null)
        {
            log_.error("No IP address is assigned to this virtual machine!");
            return false;
        }
        
        boolean isAdded = addLegacyIpAddress(ipAddress);
        if (!isAdded)
        {
            log_.debug("unable add the ip address to released ips");
            return false;
        }
      
        try
        {
            String virtualMachineId = virtualMachine.getVirtualMachineLocation().getVirtualMachineId();
            CassandraUtils.drop(getKeyspace(), Arrays.asList(virtualMachineId), CassandraUtils.VIRTUALMACHINES_CF);
            
        }
        catch (Exception exception)
        {
            log_.debug("Unable to remove virtual machine from the database");
            return false;
        }
        
        //update cache.
        groupManagerCache_.dropVirtualMachineData(location);
        
       return true;
    }
    
    
    /**
     * Adds a legacy ip address to the database.
     * 
     * @param ipAddress     The legacy address to add
     * @return              true if everything ok, false otherwise
     */
    private boolean addLegacyIpAddress(String ipAddress)
    {
        Guard.check(ipAddress);
        
        if (legacyIpAddresses_.contains(ipAddress))
        {
            log_.debug(String.format("IP address %s already exists!", ipAddress));
            return false;
        }
        
        log_.debug(String.format("Legacy IP address %s added", ipAddress));
        legacyIpAddresses_.add(ipAddress);
        return true;
    }

    @Override
    public VirtualMachineMetaData getVirtualMachineMetaData(VirtualMachineLocation location,
            int numberOfMonitoringEntries)
    {
        Guard.check(location, numberOfMonitoringEntries);
        String virtualMachineId = location.getVirtualMachineId();
        return getVirtualMachineMetaDataCassandra(virtualMachineId, numberOfMonitoringEntries);
    }
    
    
    


    @Override
    public boolean changeVirtualMachineStatus(VirtualMachineLocation location, VirtualMachineStatus status)
    {
        Guard.check(location, status);
        String virtualMachineId = location.getVirtualMachineId();
        log_.debug(String.format("Changing virtual machine %s status to %s", virtualMachineId, status));
        
        VirtualMachineMetaData virtualMachine = getVirtualMachineMetaData(location, 0);
        if (virtualMachine == null)
        {
            log_.debug("No virtual machine meta data exists");
            return false;
        }
        
        log_.debug(String.format("Virtual machine %s status changed to %s", virtualMachineId, status));
        virtualMachine.setStatus(status);
        
        addVirtualMachine(virtualMachine);

        // update cache.
        groupManagerCache_.changeVirtualMachineStatus(location, status);
        
        return true;        
    }
    
    @Override
    public boolean checkVirtualMachineStatus(VirtualMachineLocation location, VirtualMachineStatus status)
    {
        Guard.check(location, status);
        VirtualMachineMetaData virtualMachineMetaData = getVirtualMachineMetaData(location, 0);
        if (virtualMachineMetaData == null)
        {
            log_.debug("Unable to get virtual machine meta data!");
            return false;
        }

        VirtualMachineStatus state = virtualMachineMetaData.getStatus();
        if (!state.equals(status))
        {
            log_.debug(String.format("This virtual machine is not in the correct state! Current state: %s", state));
            return false;
        }
        
        return true;
    }
    @Override
    public boolean hasVirtualMachine(VirtualMachineLocation location)
    {
        
        return groupManagerCache_.hasVirtualMachine(location);
    }
    @Override
    public boolean addVirtualMachine(VirtualMachineMetaData virtualMachineMetaData)
    {
        boolean isAdded =  addVirtualMachineCassandra(virtualMachineMetaData);

        // update_cache();
        groupManagerCache_.addVirtualMachine(virtualMachineMetaData);
        
        return isAdded;
    }
    @Override
    public void clean()
    {
        // ?
    }
    @Override
    public String searchVirtualMachine(String virtualMachineId)
    {
        return null;
    }
    @Override
    public boolean updateVirtualMachineLocation(VirtualMachineLocation oldVirtualMachineLocation,
            VirtualMachineLocation newVirtualMachineLocation)
    {
        Guard.check(oldVirtualMachineLocation , newVirtualMachineLocation);
        
        String virtualMachineId = oldVirtualMachineLocation.getVirtualMachineId();
        log_.debug(String.format("Udpating virtual machine %s location", virtualMachineId));
        
        VirtualMachineMetaData virtualMachine = getVirtualMachineMetaData(oldVirtualMachineLocation, 0);
        if (virtualMachine == null)
        {
            log_.debug("No virtual machine meta data exists");
            return false;
        }
        
        log_.debug(String.format("Virtual machine %s locatgion changed", virtualMachineId));
        virtualMachine.setVirtualMachineLocation(newVirtualMachineLocation);
        
        addVirtualMachine(virtualMachine);
        
        // update cache
        groupManagerCache_.updateVirtualMachineLocation(oldVirtualMachineLocation, newVirtualMachineLocation);
        
        return true;       
    }

    @Override
    public boolean changeLocalControllerStatus(String localControllerId, LocalControllerStatus status)
    {   
        Guard.check(localControllerId, status);
        log_.debug(String.format("Changing local controller %s status to %s", localControllerId, status));
        
        LocalControllerDescription localControllerDescription = 
                getLocalControllerDescription(localControllerId, 0, false);
        if (localControllerDescription == null)
        {
            log_.debug("No local controller description exists");
            return false;
        }
        
        log_.debug(String.format("Local controller %s status changed to %s", localControllerId, status));
        localControllerDescription.setStatus(status);
        
        addLocalControllerDescription(localControllerDescription);
        
        // update cache.
        groupManagerCache_.changeLocalControllerStatus(localControllerId, status);
        return true;
    }




    @Override
    public String hasLocalController(NetworkAddress localControllerAddress)
    {
        //unused if gl knows lc (e.g with permanent db)
        return null;
    }
    
    //unused ? 
    @Override
    public boolean updateVirtualMachineMetaData(VirtualMachineMetaData virtualMachine)
    {
        return true;
        //update_cache(inc)
    }


    @Override
    public NetworkAddress getLocalControllerControlDataAddress(VirtualMachineLocation location)
    {
        Guard.check(location);
        
        String virtualMachineId = location.getVirtualMachineId();
        String localControllerId = location.getLocalControllerId();
        log_.debug(String.format("Getting local controller description for virtual machine: %s", virtualMachineId));
        
        LocalControllerDescription localController = getLocalControllerDescription(localControllerId, 0, false);
        if (localController == null)
        {
            log_.debug("The local controller description is NULL");
            return null;
        }
        // skip the check of virtual machine (see memoryRepository)
        
        return localController.getControlDataAddress();
    }


    @Override
    public LocalControllerDescription getLocalControllerDescription(String localControllerId,
            int numberOfMonitoringEntries, boolean withVirtualMachines)
    {

        LocalControllerDescription localController = 
                groupManagerCache_.getLocalControllerDescription(localControllerId, 0, withVirtualMachines);
        
        
        // cassandra request.
        if (withVirtualMachines && numberOfMonitoringEntries > 0)
        {
            for (VirtualMachineMetaData virtualMachine : localController.getVirtualMachineMetaData().values())
            {
                fillVirtualMachineMonitoringData(virtualMachine, numberOfMonitoringEntries);
            }
        }
        
        return localController;
    }
    
    /**
     * 
     * Release networking information.
     * 
     * @param localController       The local controller description.
     * @return                      True if everything is ok.
     */
    protected boolean releaseLocalControllerNetworkingInformation(LocalControllerDescription localController)
    {
        log_.debug("Releasing the local controller networking information");
        
        Map<String, VirtualMachineMetaData> metaData = localController.getVirtualMachineMetaData();
        
        if (metaData == null)
        {
            log_.debug("No virtual machine meta data available on this local controller!");
            return false;
        }
        
        log_.debug(String.format("The size of the virtual machine data map is %s", metaData.size()));       
        for (VirtualMachineMetaData virtualMachine : metaData.values()) 
        {            
            String ipAddress = virtualMachine.getIpAddress();
            if (ipAddress == null)
            {
                log_.debug(String.format("The IP address of virtual machine %s is NULL",
                                         virtualMachine.getVirtualMachineLocation().getVirtualMachineId()));
                continue;
            }
            
            addLegacyIpAddress(ipAddress);
        }
        
    
        return true;
    }


    @Override
    public GroupManagerDescription getGroupManager()
    {
        return groupManagerCache_.getGroupManager();
    }


    @Override
    public ArrayList<LocalControllerDescription> getLocalControllerDescriptionForDataTransporter()
    {
        return new ArrayList<LocalControllerDescription>();
    }
}
