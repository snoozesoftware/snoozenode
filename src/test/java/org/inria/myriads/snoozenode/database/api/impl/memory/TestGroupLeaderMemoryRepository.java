package org.inria.myriads.snoozenode.database.api.impl.memory;


import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.List;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.inria.myriads.snoozecommon.communication.NetworkAddress;
import org.inria.myriads.snoozecommon.communication.groupmanager.GroupManagerDescription;
import org.inria.myriads.snoozecommon.communication.localcontroller.AssignedGroupManager;
import org.inria.myriads.snoozecommon.communication.localcontroller.LocalControllerDescription;
import org.inria.myriads.snoozecommon.communication.rest.CommunicatorFactory;
import org.inria.myriads.snoozecommon.communication.rest.api.GroupManagerAPI;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.monitoring.external.ExternalNotifierSettings;




/**
 * 
 * Test group leaer memory repository.
 * 
 * @author msimonin
 *
 */
public class TestGroupLeaderMemoryRepository extends TestCase
{
    /** Group Leader memory under test.*/
    private BootstrapMemoryRepository repository_;

    
    @Override
    protected void setUp() throws Exception
    {
        repository_ = new BootstrapMemoryRepository();
    }
    
    public void testGetGroupManagers()
    {

        GroupManagerAPI groupManagerCommunicator = EasyMock.createMock(GroupManagerAPI.class);
        NetworkAddress groupLeader = new NetworkAddress();
        expect(CommunicatorFactory.newGroupManagerCommunicator(groupLeader)).andReturn(groupManagerCommunicator);
        
        
        
    }
    
}
