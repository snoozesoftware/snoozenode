package org.inria.myriads.snoozenode.database.api.wrapper;

import java.util.ArrayList;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.groupmanager.summary.GroupManagerSummaryInformation;
import org.inria.myriads.snoozecommon.communication.localcontroller.AssignedGroupManager;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.database.DatabaseFactory;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.message.SystemMessage;
import org.inria.myriads.snoozenode.message.SystemMessageType;
import org.inria.myriads.snoozenode.util.ExternalNotifierUtils;
import org.inria.snoozenode.external.notifier.ExternalNotificationType;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Wrapper class for repository.
 * 
 * @author msimonin
 *
 */
public class GroupLeaderWrapperRepository implements GroupLeaderRepository
{
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(GroupLeaderWrapperRepository.class);
    
    
    /** Repository. */
    private GroupLeaderRepository repository_;
    
    /** External Notifier.*/
    private ExternalNotifier externalNotifier_;
    
    
    /**
     * 
     * Constructor.
     * 
     * @param groupLeaderDescription    The groupLeader description.
     * @param virtualMachineSubnets     The virtual machine subnets.
     * @param settings                  The database ettings
     * @param externalNotifier          The external notifier.
     */
    public GroupLeaderWrapperRepository(GroupManagerDescription groupLeaderDescription,
            String[] virtualMachineSubnets,
            DatabaseSettings settings,
            ExternalNotifier externalNotifier)
    {
        log_.debug("Initializing the group leader memory repository");
        //call to factory
        repository_ = DatabaseFactory.newGroupLeaderRepository(
                groupLeaderDescription, 
                virtualMachineSubnets,
                settings);
        
        externalNotifier_ = externalNotifier;
        
        ExternalNotifierUtils.send(
                externalNotifier_,
                ExternalNotificationType.SYSTEM, 
                new SystemMessage(SystemMessageType.GL_JOIN, groupLeaderDescription),
                "groupleader");        

    }

    @Override
    public boolean addGroupManagerDescription(GroupManagerDescription description)
    {
        boolean added = repository_.addGroupManagerDescription(description);
        

        if (added)
        {
            ExternalNotifierUtils.send(
                    externalNotifier_,
                    ExternalNotificationType.SYSTEM,
                    new SystemMessage(SystemMessageType.GM_JOIN, description),
                    "groupleader");            
        }
        
        return added;
    }

    @Override
    public ArrayList<GroupManagerDescription> getGroupManagerDescriptions(int numberOfBacklogEntries)
    {
        return repository_.getGroupManagerDescriptions(numberOfBacklogEntries);
    }

    @Override
    public GroupManagerDescription getGroupManagerDescription(String groupManagerId, int numberOfBacklogEntries)
    {
        return repository_.getGroupManagerDescription(groupManagerId, numberOfBacklogEntries);
    }

    @Override
    public void addGroupManagerSummaryInformation(String groupManagerId, GroupManagerSummaryInformation summary)
    {
        repository_.addGroupManagerSummaryInformation(groupManagerId, summary);    
    }

    @Override
    public boolean dropGroupManager(String groupManagerId)
    {
        boolean isDropped =  repository_.dropGroupManager(groupManagerId);
        
        if (isDropped)
        {
            ExternalNotifierUtils.send(
                externalNotifier_,
                ExternalNotificationType.SYSTEM, 
                new SystemMessage(SystemMessageType.GM_FAILED, 
                        groupManagerId), 
                "groupleader");
        }
        
        return isDropped;
    }

    @Override
    public boolean addIpAddress(String ipAddress)
    {
        return repository_.addIpAddress(ipAddress);
    }

    @Override
    public boolean removeIpAddress(String ipAddress)
    {
        return repository_.removeIpAddress(ipAddress);
    }

    @Override
    public String getFreeIpAddress()
    {
        return repository_.getFreeIpAddress();
    }

    @Override
    public ArrayList<LocalControllerDescription> getLocalControllerList()
    {
        return repository_.getLocalControllerList();
    }

    @Override
    public AssignedGroupManager getAssignedGroupManager(NetworkAddress contactInformation)
    {
        return repository_.getAssignedGroupManager(contactInformation);
    }

    @Override
    public boolean updateLocation(VirtualMachineLocation location)
    {
        return repository_.updateLocation(location);
    }

    @Override
    public LocalControllerDescription getLocalControllerDescription(String localControllerId)
    {
        return repository_.getLocalControllerDescription(localControllerId);
    }

}
