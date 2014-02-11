package org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.worker;

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPlan;
import org.inria.myriads.snoozenode.groupmanager.managerpolicies.placement.PlacementPolicy;
import org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.listener.VirtualClusterSubmissionListener;

/**
 * 
 * Test virtual cluster manager worker.
 * 
 * @author msimonin
 *
 */
public class Testvirtualclustermanagerworker extends TestCase
{

    /**
     * 
     * Test split.
     * 
     * vm1 bound gm
     * vm2 bound gm + lc
     * vm3 bound with wrong host id
     * vm4 free
     * 
     * */
    public void testSplitVirtualMachines()
    {
       
        ResourceDemandEstimator estimator = EasyMock.createMock(ResourceDemandEstimator.class);
        DispatchingPolicy dispatchingPolicy = EasyMock.createMock(DispatchingPolicy.class);
        GroupLeaderRepository repository =  EasyMock.createMock(GroupLeaderRepository.class);
        VirtualClusterSubmissionListener submissionListener = 
                EasyMock.createMock(VirtualClusterSubmissionListener.class);
        NodeConfiguration nodeConfiguration =  EasyMock.createMock(NodeConfiguration.class);
        
        VirtualMachineMetaData vm1 = new VirtualMachineMetaData();
        VirtualMachineLocation location1 = new VirtualMachineLocation();
        location1.setGroupManagerId("gm1");
        vm1.setVirtualMachineLocation(location1);
        
        VirtualMachineMetaData vm2 = new VirtualMachineMetaData();
        VirtualMachineLocation location2 = new VirtualMachineLocation();
        location2.setGroupManagerId("gm1");
        location2.setLocalControllerId("lc1");
        vm2.setVirtualMachineLocation(location2);
        
        VirtualMachineMetaData vm3 = new VirtualMachineMetaData();
        vm3.setStatus(VirtualMachineStatus.ERROR);
        vm3.setErrorCode(VirtualMachineErrorCode.INVALID_HOST_ID);
        
        VirtualMachineMetaData vm4 = new VirtualMachineMetaData();
        
        ArrayList<VirtualMachineMetaData> virtualMachines = new ArrayList<VirtualMachineMetaData>();
        virtualMachines.add(vm1);
        virtualMachines.add(vm2);
        virtualMachines.add(vm3);
        virtualMachines.add(vm4);
        
        VirtualClusterSubmissionWorker managerWorker = 
                new VirtualClusterSubmissionWorker("task1",
                                                    virtualMachines,
                                                    nodeConfiguration,
                                                    dispatchingPolicy,
                                                    repository,
                                                    estimator,
                                                    submissionListener);

        //usually works on a copy : see the code.
        ArrayList<VirtualMachineMetaData> boundVirtualMachines = new ArrayList<VirtualMachineMetaData>();
        ArrayList<VirtualMachineMetaData> freeVirtualMachines = new ArrayList<VirtualMachineMetaData>();
        
        
        managerWorker.splitVirtualMachines(virtualMachines, boundVirtualMachines, freeVirtualMachines);
        
        assertEquals(2, boundVirtualMachines.size());
        assertTrue(boundVirtualMachines.contains(vm1));
        assertTrue(boundVirtualMachines.contains(vm2));
        assertEquals(1, freeVirtualMachines.size());
        assertTrue(freeVirtualMachines.contains(vm4));
    }
    


}
