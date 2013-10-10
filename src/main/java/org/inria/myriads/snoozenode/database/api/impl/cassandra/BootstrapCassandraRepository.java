package org.inria.myriads.snoozenode.database.api.impl.cassandra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerList;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerLocation;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.ClientMigrationRequestSimple;
import org.inria.myriads.snoozecommon.communication.virtualcluster.migration.MigrationRequest;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozenode.database.api.BootstrapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 
 * Bootstrap Cassandra repository.
 * 
 * @author msimonin
 *
 */
public class BootstrapCassandraRepository extends CassandraRepository implements BootstrapRepository
{
    
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(BootstrapCassandraRepository.class);
    
    /**
     * 
     * Constructor.
     * 
     * @param hosts         List of cassandra hosts to connect to.
     */
    public BootstrapCassandraRepository(String hosts)
    {
        super(hosts);
        log_.debug("Bootstrap cassandra repository initialized");
    }


    @Override
    public VirtualMachineMetaData getVirtualMachineMetaData(
            String virtualMachineId,
            int numberOfMonitoringEntries,
            GroupManagerDescription groupLeader)
    {
        VirtualMachineMetaData virtualMachine = 
                getVirtualMachineMetaDataCassandra(virtualMachineId, numberOfMonitoringEntries);
        return virtualMachine;
    }
    
    @Override
    public  List<GroupManagerDescription> getGroupManagerDescriptions(
            String firstGroupManagerId,
            int limit,
            int numberOfBacklogEntries,
            GroupManagerDescription groupLeader)
    {
        String groupLeaderId = groupLeader.getId();
        return getGroupManagerDescriptionsOnly(
                firstGroupManagerId,
                limit,
                false,
                numberOfBacklogEntries,
                Arrays.asList(groupLeaderId));
    }


    @Override
    public LocalControllerList getLocalControllerList()
    {   
        ArrayList<LocalControllerDescription> localControllers =
                getLocalControllerDescriptionsOnly(null, null, -1, 0, false, false);
        
        //
        LocalControllerList localControllerList = new LocalControllerList(localControllers);
        return localControllerList;
    }


    @Override
    public List<LocalControllerDescription> getLocalControllerDescriptions(
            String groupManagerId, 
            String firstLocalControllerId,
            int limit, 
            int numberOfBacklogEntries,
            GroupManagerDescription groupLeader
            )
    {
        
        ArrayList<LocalControllerDescription> localControllers = 
                getLocalControllerDescriptionsOnly(
                        groupManagerId,
                        firstLocalControllerId,
                        limit,
                        numberOfBacklogEntries,
                        false,
                        false);

        return localControllers;
    }


    @Override
    public ArrayList<VirtualMachineMetaData> getVirtualMachineDescriptions(
            String groupManagerId, 
            String localControllerId,
            String startVirtualMachine,
            int limit,
            int numberOfBacklogEntries,
            GroupManagerDescription groupLeader
            )
    {

        ArrayList<VirtualMachineMetaData> virtualMachines =
                getVirtualMachineDescriptionsOnly(
                        groupManagerId, 
                        localControllerId, 
                        startVirtualMachine, 
                        limit, 
                        numberOfBacklogEntries, 
                        false);
        
        return virtualMachines;
    }


    @Override
    public MigrationRequest createMigrationRequest(ClientMigrationRequestSimple migrationRequest)
    {
        String virtualMachineId = migrationRequest.getVirtualMachineId();
        String localControllerId = migrationRequest.getLocalControllerId();
        
        MigrationRequest internalMigrationRequest = new MigrationRequest();
        // get source
        VirtualMachineMetaData virtualMachine = getVirtualMachineDescriptionOnly(virtualMachineId, 0);
        if (virtualMachine == null)
        {   
            log_.debug("Unable to create the migration request : virtual machine not found");
            return null;
        }
        VirtualMachineLocation sourceLocation =  virtualMachine.getVirtualMachineLocation();
        internalMigrationRequest.setSourceVirtualMachineLocation(sourceLocation);
        // get destination
        LocalControllerDescription localController = this.getLocalControllerDescriptionOnly(localControllerId, 0);
        if (localController == null)
        {
            log_.debug("Unable to create the migration request : localcontroller not found");
            return null;
        }
        LocalControllerLocation location = localController.getLocation();
        VirtualMachineLocation destinationLocation = new VirtualMachineLocation();
        destinationLocation.setVirtualMachineId(virtualMachineId);
        destinationLocation.setLocalControllerId(localControllerId);
        destinationLocation.setLocalControllerControlDataAddress(localController.getControlDataAddress());
        destinationLocation.setGroupManagerId(location.getGroupManagerId());
        destinationLocation.setGroupManagerControlDataAddress(location.getGroupManagerControlDataAddress());
        
        internalMigrationRequest.setDestinationVirtualMachineLocation(destinationLocation);
        internalMigrationRequest.setDestinationHypervisorSettings(localController.getHypervisorSettings());
        
        return internalMigrationRequest;
        
    }


    @Override
    public GroupManagerDescription getGroupManagerDescription(
            String groupManagerId, 
            GroupManagerDescription groupLeaderDescription)
    {
        return this.getGroupManagerDescriptionOnly(groupManagerId, 0);
    }
   

}
