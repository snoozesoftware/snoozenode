package org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.worker;

import java.util.ArrayList;
import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.virtualcluster.VirtualMachineMetaData;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineErrorCode;
import org.inria.myriads.snoozecommon.communication.virtualcluster.status.VirtualMachineStatus;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineGroupManagerLocation;
import org.inria.myriads.snoozecommon.communication.virtualcluster.submission.VirtualMachineLocation;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.database.api.GroupLeaderRepository;
import org.inria.myriads.snoozenode.groupmanager.estimator.ResourceDemandEstimator;
import org.inria.myriads.snoozenode.groupmanager.leaderpolicies.dispatching.DispatchingPolicy;
import org.inria.myriads.snoozenode.groupmanager.virtualclustermanager.listener.VirtualClusterSubmissionListener;

import junit.framework.TestCase;

public class Testvirtualclustermanagerworker extends TestCase
{

    /**
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
        VirtualClusterSubmissionListener submissionListener = EasyMock.createMock(VirtualClusterSubmissionListener.class);
        NodeConfiguration nodeConfiguration =  EasyMock.createMock(NodeConfiguration.class);
        
        
        VirtualMachineMetaData vm1 = new VirtualMachineMetaData();
        VirtualMachineGroupManagerLocation gmLocation1 = new VirtualMachineGroupManagerLocation();
        gmLocation1.setGroupManagerId("gm1");
        vm1.setGroupManagerLocation(gmLocation1);
        
        VirtualMachineMetaData vm2 = new VirtualMachineMetaData();
        VirtualMachineLocation location1 = new VirtualMachineLocation();
        location1.setLocalControllerId("lc1");
        VirtualMachineGroupManagerLocation gmLocation2 = new VirtualMachineGroupManagerLocation();
        gmLocation2.setGroupManagerId("gm1");
        vm2.setGroupManagerLocation(gmLocation2);
        vm2.setVirtualMachineLocation(location1);
        
        VirtualMachineMetaData vm3 = new VirtualMachineMetaData();
        vm3.setStatus(VirtualMachineStatus.ERROR);
        vm3.setErrorCode(VirtualMachineErrorCode.INVALID_HOST_ID);
        
        VirtualMachineMetaData vm4 = new VirtualMachineMetaData();
        
        ArrayList<VirtualMachineMetaData> virtualMachines = new ArrayList<VirtualMachineMetaData>();
        virtualMachines.add(vm1);
        virtualMachines.add(vm2);
        virtualMachines.add(vm3);
        virtualMachines.add(vm4);
        
        VirtualClusterSubmissionWorker managerWorker_ = 
                new VirtualClusterSubmissionWorker("task1",virtualMachines, nodeConfiguration, dispatchingPolicy, repository, estimator, submissionListener);

//        ArrayList<VirtualMachineMetaData> virtualMachinesCopy = 
//                new ArrayList<VirtualMachineMetaData>(Arrays.asList(new VirtualMachineMetaData[virtualMachines.size()]));  
//            
//            Collections.copy(virtualMachinesCopy, virtualMachines);
        ArrayList<VirtualMachineMetaData> boundVirtualMachines = new ArrayList<VirtualMachineMetaData>();
        ArrayList<VirtualMachineMetaData> freeVirtualMachines = new ArrayList<VirtualMachineMetaData>();
        
        
        managerWorker_.splitVirtualMachines(virtualMachines, boundVirtualMachines, freeVirtualMachines);
        
        assertEquals(2, boundVirtualMachines.size());
        assertTrue(boundVirtualMachines.contains(vm1));
        assertTrue(boundVirtualMachines.contains(vm2));
        assertEquals(1, freeVirtualMachines.size());
        assertTrue(freeVirtualMachines.contains(vm4));
    }

}
