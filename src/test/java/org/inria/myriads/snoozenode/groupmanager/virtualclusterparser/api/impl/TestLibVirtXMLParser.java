package org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.api.impl;


import junit.framework.TestCase;

import org.inria.myriads.snoozenode.groupmanager.virtualclusterparser.util.VirtualClusterParserUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * Tests the libvirt parser.
 * 
 * @author msimonin
 *
 */
public class TestLibVirtXMLParser extends TestCase 
{

    /**
     * 
     * The the handle resize request method.
     * 
     */
    public void testHandleResizeRequest() 
    {
        String xmlDesc = 
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                   "<domain type=\"kvm\"> " +
                       "<name>debian2</name>"  +
                       "<uuid>0f476e56-67ea-11e1-858e-00216a972a37</uuid>" +
                       "<memory>597152</memory>" +
                       "<vcpu>2</vcpu>" +
                       "<os>" +
                           "<type arch=\"x86_64\" machine=\"pc-0.12\">hvm</type>" +
                           "<boot dev=\"hd\"/>" +
                       "</os>" +
                       "<features>" +
                           "<acpi/>" +
                           "<apic/>" +
                           "<pae/>" +
                       "</features>" +
                       "<clock offset=\"utc\"/>" +
                       "<on_poweroff>destroy</on_poweroff>" +
                       "<on_reboot>restart</on_reboot>" +
                       "<on_crash>restart</on_crash>" +
                       "<devices>" +
                           "<emulator>/usr/bin/kvm</emulator>" +
                           "<disk device=\"disk\" type=\"file\">" +
                              "<driver name=\"qemu\" type=\"qcow2\"/>" +
                               "<source file=\"/home/msimonin/Images-VM/Snooze-images/imgs/debian2.qcow2\"/>" +
                               "<target bus=\"virtio\" dev=\"vda\"/>" +
                               "<address bus=\"0x00\" domain=\"0x0000\" function=\"0x0\" slot=\"0x05\" type=\"pci\"/>" +
                           "</disk>" +
                       "<controller index=\"0\" type=\"ide\">" +
                          "<address bus=\"0x00\" domain=\"0x0000\" function=\"0x1\" slot=\"0x01\" type=\"pci\"/>" +
                       "</controller>" +
                       "<interface type=\"bridge\">" +
                          "<mac address=\"52:54:00:83:25:2b\"/>" +
                          "<source bridge=\"virbr0\"/>" +
                       "</interface>" +
                       "<serial type=\"pty\">" +
                           "<target port=\"0\"/>" +
                       "</serial>" +
                       "<console type=\"pty\">" +
                           "<target port=\"0\" type=\"serial\"/>" +
                       "</console>" +
                       "<graphics autoport=\"yes\" listen=\"0.0.0.0\" port=\"-1\" type=\"vnc\"/>" +
                       "<input bus=\"usb\" type=\"tablet\"/>" +
                       "<input bus=\"ps2\" type=\"tablet\"/>" +
                       "<memballoon model=\"virtio\">" +
                          "<address bus=\"0x00\" domain=\"0x0000\" function=\"0x0\" slot=\"0x06\" type=\"pci\"/>" +
                       "</memballoon>" +
                       "</devices>" +
                 "</domain>";
        String objXmlDesc = xmlDesc.replaceAll("<vcpu>2</vcpu>", "<vcpu>1</vcpu>");
        Document doc = VirtualClusterParserUtils.stringToDom(xmlDesc);
        NodeList nodes = doc.getElementsByTagName("vcpu");
        
        assertEquals(nodes.getLength(), 1);
        Node node = nodes.item(0);
        assertEquals(node.getNodeType(), Node.ELEMENT_NODE);
        
        node = node.getFirstChild();
        assertEquals(node.getNodeType(), Node.TEXT_NODE);
        node.setNodeValue("1");
        //careful it sorts all the attributes by alpĥabetical order...
        String newXmlDesc = VirtualClusterParserUtils.domToString(doc);
        assertEquals(newXmlDesc, objXmlDesc);
    }

}
