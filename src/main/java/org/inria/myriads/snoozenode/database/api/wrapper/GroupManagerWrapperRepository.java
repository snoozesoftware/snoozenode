package org.inria.myriads.snoozenode.database.api.wrapper;

import java.util.ArrayList;
import java.util.List;

import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozenode.configurator.database.DatabaseSettings;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;
import org.inria.myriads.snoozenode.database.DatabaseFactory;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.localcontroller.monitoring.transport.AggregatedVirtualMachineData;
import org.inria.myriads.snoozenode.message.SystemMessage;
import org.inria.myriads.snoozenode.message.SystemMessageType;
import org.inria.myriads.snoozenode.util.ExternalNotifierUtils;
import org.inria.snoozenode.external.notifier.ExternalNotificationType;
import org.inria.snoozenode.external.notifier.ExternalNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Wrapper for the group manager class.
 * 
 * @author msimonin
 *
 */
public class GroupManagerWrapperRepository implements GroupManagerRepository
{

    /** Logger.*/
    private static final Logger log_ = LoggerFactory.getLogger(GroupManagerWrapperRepository.class);
    
    /** The repository. */
    private GroupManagerRepository repository_;
    
    /**External notifier.*/
    private ExternalNotifier externalNotifier_; 
    
    /**
     * 
     * Constructor.
     * 
     * @param groupManager                  The group manager description
     * @param maxCapacity                   The max capacity.
     * @param interval                      The interval.
     * @param settings                      The settings.
     * @param externalNotifierSettings      The external notifier settings.
     * @param externalNotifier              The external notifier.
     */
    public GroupManagerWrapperRepository(
            GroupManagerDescription groupManager, 
            int maxCapacity, 
            int interval,
            DatabaseSettings settings,
            ExternalNotifierSettings externalNotifierSettings,
            ExternalNotifier externalNotifier
            ) 
    {
        log_.debug("Initializing the group manager  wrapper repository");
        externalNotifier_ = externalNotifier;
        repository_ = DatabaseFactory.newGroupManagerRepository(groupManager, maxCapacity, interval, settings);
    }

            
    @Override
    public String getGroupManagerId()
    {
        return repository_.getGroupManagerId();
    }

    @Override
    public ArrayList<LocalControllerDescription> getLocalControllerDescriptions(int numberOfMonitoringEntries,
            boolean isActiveOnly, boolean withVirtualMachines)
    {
        return repository_.getLocalControllerDescriptions(numberOfMonitoringEntries, isActiveOnly, withVirtualMachines);
    }

    @Override
    public NetworkAddress getLocalControllerControlDataAddress(VirtualMachineLocation location)
    {
        return repository_.getLocalControllerControlDataAddress(location);
    }

    @Override
    public LocalControllerDescription getLocalControllerDescription(String localControllerId,
            int numberOfMonitoringEntries, boolean withVirtualMachines)
    {
        return repository_.getLocalControllerDescription(
                localControllerId,
                numberOfMonitoringEntries, 
                withVirtualMachines);
    }

    @Override
    public boolean addLocalControllerDescription(LocalControllerDescription description)
    {
        
        boolean isAdded = repository_.addLocalControllerDescription(description);
        if (isAdded)
        {
            ExternalNotifierUtils.send(
                    externalNotifier_,
                    ExternalNotificationType.SYSTEM,
                    new SystemMessage(SystemMessageType.LC_JOIN, description),
                    "groupmanager." + getGroupManagerId());
        }
        
        return isAdded;
    }

    @Override
    public boolean dropLocalController(String localControllerId, boolean forceDelete)
    {
        boolean isDropped = repository_.dropLocalController(localControllerId, forceDelete);
        if (isDropped)
        {
            ExternalNotifierUtils.send(
                    externalNotifier_,
                    ExternalNotificationType.SYSTEM,
                    new SystemMessage(SystemMessageType.LC_FAILED, localControllerId),
                    "groupmanager." + getGroupManagerId());
        }
        
        return isDropped;
    }

    @Override
    public void fillGroupManagerDescription(GroupManagerDescription groupManagerDescription)
    {
        repository_.fillGroupManagerDescription(groupManagerDescription);
    }

    @Override
    public void addAggregatedMonitoringData(String localControllerId, List<AggregatedVirtualMachineData> aggregatedData)
    {
        repository_.addAggregatedMonitoringData(localControllerId, aggregatedData);

    }

    @Override
    public ArrayList<String> getLegacyIpAddresses()
    { 
        return repository_.getLegacyIpAddresses();
    }

    @Override
    public boolean dropVirtualMachineData(VirtualMachineLocation location)
    {
        return repository_.dropVirtualMachineData(location);
    }

    @Override
    public VirtualMachineMetaData getVirtualMachineMetaData(VirtualMachineLocation location,
            int numberOfMonitoringEntries)
    {
        return repository_.getVirtualMachineMetaData(location, numberOfMonitoringEntries);
    }

    @Override
    public boolean changeVirtualMachineStatus(VirtualMachineLocation location, VirtualMachineStatus status)
    {
        return repository_.changeVirtualMachineStatus(location, status);
    }

    @Override
    public boolean checkVirtualMachineStatus(VirtualMachineLocation location, VirtualMachineStatus status)
    {
        return repository_.checkVirtualMachineStatus(location, status);
    }

    @Override
    public boolean hasVirtualMachine(VirtualMachineLocation location)
    {
        return repository_.hasVirtualMachine(location);
    }

    @Override
    public boolean addVirtualMachine(VirtualMachineMetaData virtualMachineMetaData)
    {
        return repository_.addVirtualMachine(virtualMachineMetaData);
    }

    @Override
    public void clean()
    {
        repository_.clean();
    }

    @Override
    public String searchVirtualMachine(String virtualMachineId)
    {
        return repository_.searchVirtualMachine(virtualMachineId);
    }

    @Override
    public boolean updateVirtualMachineLocation(VirtualMachineLocation oldVirtualMachineLocation,
            VirtualMachineLocation newVirtualMachineLocation)
    {
        return repository_.updateVirtualMachineLocation(oldVirtualMachineLocation, newVirtualMachineLocation);
    }

    @Override
    public boolean changeLocalControllerStatus(String localControllerId, LocalControllerStatus status)
    {
        return repository_.changeLocalControllerStatus(localControllerId, status);
    }

    @Override
    public String hasLocalController(NetworkAddress localControllerAddress)
    {
        return repository_.hasLocalController(localControllerAddress);
    }

    @Override
    public boolean updateVirtualMachineMetaData(VirtualMachineMetaData virtualMachine)
    {
        return repository_.updateVirtualMachineMetaData(virtualMachine);
    }


    @Override
    public GroupManagerDescription getGroupManager()
    {
        return repository_.getGroupManager();
    }


    @Override
    public ArrayList<LocalControllerDescription> getLocalControllerDescriptionForDataTransporter()
    {
        return repository_.getLocalControllerDescriptionForDataTransporter();
    }

}
