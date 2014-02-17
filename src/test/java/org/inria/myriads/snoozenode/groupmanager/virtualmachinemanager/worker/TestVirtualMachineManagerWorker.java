package org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager.worker;

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineSubmissionRequest;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.database.api.GroupManagerRepository;
import org.inria.myriads.snoozenode.estimator.api.impl.StaticDynamicResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.StateMachine;
import org.inria.myriads.snoozenode.groupmanager.statemachine.api.impl.GroupManagerStateMachine;
import org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.listener.VirtualClusterSubmissionListener;
import org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.worker.VirtualClusterSubmissionWorker;
import org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager.listener.VirtualMachineManagerListener;
import org.inria.myriads.snoozenode.groupmanager.virtualmachinemanager.worker.VirtualMachineSubmissionWorker;
import org.inria.snoozenode.external.notifier.ExternalNotifier;

import static org.easymock.EasyMock.replay;

public class TestVirtualMachineManagerWorker extends TestCase
{
    public void testBuildPlacementPlan()
    {

//        ResourceDemandEstimator estimator = EasyMock.createMock(ResourceDemandEstimator.class);
//        VirtualMachineSubmissionRequest submissionRequest = EasyMock.createMock(VirtualMachineSubmissionRequest.class);
//        GroupManagerRepository repository =  EasyMock.createMock(GroupManagerRepository.class);
//        PlacementPolicy placementPolicy =  EasyMock.createMock(PlacementPolicy.class);
//        GroupManagerStateMachine stateMachine =  EasyMock.createMock(GroupManagerStateMachine.class);
//        VirtualMachineManagerListener listener =  EasyMock.createMock(VirtualMachineManagerListener.class);
//        ExternalNotifier notifier =  EasyMock.createMock(ExternalNotifier.class);
//        
//        ArrayList<VirtualMachineMetaData> virtualMachines  = new ArrayList<VirtualMachineMetaData>();
//        
//        VirtualClusterSubmissionListener submissionListener = 
//                EasyMock.createMock(VirtualClusterSubmissionListener.class);
//        NodeConfiguration nodeConfiguration =  EasyMock.createMock(NodeConfiguration.class);
//        
//        VirtualMachineSubmissionWorker managerWorker = 
//                new VirtualMachineSubmissionWorker(
//                        "task1",
//                        10,
//                        submissionRequest, 
//                        repository,
//                        placementPolicy,
//                        stateMachine,
//                        estimator,
//                        listener,
//                        notifier);
//        
//        PlacementPolicy staticPlacement = EasyMock.createMock(PlacementPolicy.class);
//        PlacementPolicy freePlacement = EasyMock.createMock(PlacementPolicy.class);
//        
//        ArrayList<VirtualMachineMetaData> boundVirtualMachines  = new ArrayList<VirtualMachineMetaData>();
//        ArrayList<VirtualMachineMetaData> freeVirtualMachines  = new ArrayList<VirtualMachineMetaData>();
//        ArrayList<LocalControllerDescription> localControllers = new ArrayList<LocalControllerDescription>();
//        
//        
//        PlacementPlan freePlacementPlan = new PlacementPlan(localControllers, freeVirtualMachines);
//        PlacementPlan boundPlacementPlan = new PlacementPlan(localControllers, boundVirtualMachines);
//        
//        freePlacementPlan.getLocalControllers().add(new LocalControllerDescription());
//        
//        expect(freePlacement.place(freeVirtualMachines, localControllers)).andReturn(freePlacementPlan);
//        expect(staticPlacement.place(boundVirtualMachines, localControllers)).andReturn(boundPlacementPlan);
//        replay(staticPlacement);
//        replay(freePlacement);
//        
//        PlacementPlan p = managerWorker.buildPlacementPlan(boundVirtualMachines, freeVirtualMachines, localControllers);
//        
//        assertEquals(0, p.getLocalControllers().size());

    }
}
