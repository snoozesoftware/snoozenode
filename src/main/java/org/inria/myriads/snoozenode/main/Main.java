/**
 * Copyright (C) 2010-2013 Eugen Feller, INRIA <eugen.feller@inria.fr>
 *
 * This file is part of Snooze, a scalable, autonomic, and
 * energy-aware virtual machine (VM) management framework.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package org.inria.myriads.snoozenode.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;

import org.inria.myriads.snoozecommon.communication.NodeRole;
import org.inria.myriads.snoozecommon.guard.Guard;
import org.inria.myriads.snoozecommon.util.ErrorUtils;
import org.inria.myriads.snoozecommon.util.LoggerUtils;
import org.inria.myriads.snoozenode.bootstrap.BootstrapBackend;
import org.inria.myriads.snoozenode.configurator.NodeConfiguratorFactory;
import org.inria.myriads.snoozenode.configurator.api.NodeConfiguration;
import org.inria.myriads.snoozenode.configurator.api.NodeConfigurator;
import org.inria.myriads.snoozenode.configurator.httpd.HTTPdSettings;
import org.inria.myriads.snoozenode.exception.ConnectorException;
import org.inria.myriads.snoozenode.exception.HostMonitoringException;
import org.inria.myriads.snoozenode.exception.NodeConfiguratorException;
import org.inria.myriads.snoozenode.exception.VirtualMachineMonitoringException;
import org.inria.myriads.snoozenode.groupmanager.GroupManagerBackend;
import org.inria.myriads.snoozenode.localcontroller.LocalControllerBackend;
import org.inria.myriads.snoozenode.main.applications.BootstrapApplication;
import org.inria.myriads.snoozenode.main.applications.GroupManagerApplication;
import org.inria.myriads.snoozenode.main.applications.LocalControllerApplication;
import org.inria.myriads.snoozenode.util.OutputUtils;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.engine.Engine;
import org.restlet.ext.httpclient.HttpClientHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main node class.
 * 
 * @author Eugen Feller
 */
public final class Main 
{    
    /** Define the logger. */
    private static final Logger log_ = LoggerFactory.getLogger(Main.class);
 
    /** Number of arguments. */
    private static final int NUMBER_OF_CMD_ARGUMENTS = 2;
    
    /** Hide constructor. */
    private Main()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Main method.
     * 
     * @param args  The arguments
     */
    public static void main(String[] args) 
    {
        String configurationFile = null;
        String logFile = null;

        if (args.length == NUMBER_OF_CMD_ARGUMENTS) 
        {
            configurationFile = args[0];
            logFile = args[1];
        } else
        {
            System.out.println("Usage: java -jar snoozenode.jar configurationFile logFile");
            System.exit(1); 
        }
        
        LoggerUtils.configureLogger(logFile);      
        NodeConfiguration nodeConfiguration = null;
        try 
        {
            NodeConfigurator nodeConfigurator = NodeConfiguratorFactory.newNodeConfigurator(configurationFile);
            nodeConfiguration = nodeConfigurator.getNodeConfiguration();
            OutputUtils.printNodeConfiguration(nodeConfiguration);
            startNode(nodeConfiguration);
            log_.info(String.format("%s started successfully!", nodeConfiguration.getNode().getRole()));
        }
        catch (NodeConfiguratorException exception) 
        {
            ErrorUtils.processError(String.format("Node configuration exception! Error during node " +
                                                  "configuration parsing: %s", exception.getMessage()));
        }
        catch (IllegalArgumentException exception) 
        {
            ErrorUtils.processError(String.format("Illegal argument exception! Check your configuration file: %s",  
                                                   ErrorUtils.getStackTrace(exception)));
        }
        catch (BindException exception)
        {
            ErrorUtils.processError(String.format("Bind exception: %s", exception.getMessage()));
        }
        catch (FileNotFoundException exception) 
        {
            ErrorUtils.processError(String.format("Configuration file does not exist: %s", exception.getMessage()));
        } 
        catch (ConnectorException exception) 
        {
            ErrorUtils.processError(String.format("Something critical happened during hypervisor connection: %s", 
                                                  exception.getMessage()));
        }
        catch (VirtualMachineMonitoringException exception) 
        {
            ErrorUtils.processError(String.format("Something critical happened during monitoring: %s", 
                                                  exception.getMessage()));
        }
        catch (SocketException exception)
        {
            ErrorUtils.processError(String.format("Socket exception: %s! Are you sure network is available?", 
                                                  exception.getMessage()));
        }
        catch (HostMonitoringException exception) 
        {
            ErrorUtils.processError(String.format("Something critical happened during host monitoring: %s", 
                                                  exception.getMessage()));
        }
        catch (UnsatisfiedLinkError error)
        {
            ErrorUtils.processError(String.format("Link error: %s", error.getMessage()));
        }
        catch (IOException exception) 
        {
            ErrorUtils.processError(String.format("I/O exception", ErrorUtils.getStackTrace(exception)));
        } 
        catch (InterruptedException exception) 
        {
            ErrorUtils.processError(String.format("Interrupted exception", ErrorUtils.getStackTrace(exception)));
        } 
        catch (Exception exception)
        {
            log_.error("Exception", exception);
        } 
    }

    /**
     * Initializes the component.
     * 
     * @param component            The component
     * @param context              The context
     * @param configuration        The node parameters
     * @throws Exception 
     */
    private static void initializeRESTletComponent(Component component,
                                                   Context context,
                                                   NodeConfiguration configuration) 
        throws Exception
    {
        Guard.check(component, context, configuration);
        log_.debug("Initializing the RESTlet component");
        
        Engine.getInstance().getRegisteredClients().clear();
        Engine.getInstance().getRegisteredClients().add(new HttpClientHelper(null)); 
        
        component.getClients().add(Protocol.HTTP);
        Server jettyServer = new Server(context,
                                        Protocol.HTTP, 
                                        configuration.getNetworking().getListen().getControlDataAddress().getPort(), 
                                        component);
        
        HTTPdSettings settings = configuration.getHTTPd();
        jettyServer.getContext().getParameters().add("maxThreads", settings.getMaximumNumberOfThreads());
        jettyServer.getContext().getParameters().add("maxTotalConnections", settings.getMaximumNumberOfConnections());
        jettyServer.getContext().getParameters().add("maxThreads", settings.getMaximumNumberOfThreads());
        jettyServer.getContext().getParameters().add("maxTotalConnections", settings.getMaximumNumberOfConnections());
        jettyServer.getContext().getParameters().add("minThreads", settings.getMinThreads());
        jettyServer.getContext().getParameters().add("lowThreads", settings.getLowThreads());
        jettyServer.getContext().getParameters().add("maxThreads", settings.getMaxThreads());
        jettyServer.getContext().getParameters().add("maxQueued", settings.getMaxQueued());
        jettyServer.getContext().getParameters().add("maxIoIdleTimeMs", settings.getMaxIoIdleTimeMs());
        component.getServers().add(jettyServer);
    }
    
    /** 
     * Main routine to start the node.
     *  
     * @param nodeConfiguration    The node configuration
     * @throws Exception 
     */
    private static void startNode(NodeConfiguration nodeConfiguration) 
        throws Exception 
    {
        Guard.check(nodeConfiguration);
        log_.debug("Starting the node initialization");
                
        Component component = new Component();
        Context context = component.getContext().createChildContext();
        initializeRESTletComponent(component, context, nodeConfiguration);
        
        Application application = null;
        NodeRole nodeRole = nodeConfiguration.getNode().getRole();
        
        switch (nodeRole) 
        {
            case bootstrap :
                log_.debug("Starting in bootstap mode");
                application = new BootstrapApplication(context);
                attachApplication(component, application);
                BootstrapBackend bootstrap = new BootstrapBackend(nodeConfiguration);
                context.getAttributes().put("backend", bootstrap);
                break;

            case groupmanager :
                log_.debug("Starting in group manager mode");
                application = new GroupManagerApplication(context);
                attachApplication(component, application);
                GroupManagerBackend groupmanager = new GroupManagerBackend(nodeConfiguration);
                context.getAttributes().put("backend", groupmanager);
                break;

            case localcontroller :
                log_.debug("Starting in local controller mode");
                application = new LocalControllerApplication(context);
                attachApplication(component, application);
                LocalControllerBackend localcontroller = new LocalControllerBackend(nodeConfiguration);
                context.getAttributes().put("backend", localcontroller);
                break;
    
            default:
                log_.error(String.format("This node role is not supported: %s", nodeRole));
        }
    }

    /**
     * Attaches the application to RESTlet component.
     * 
     * @param component        The component
     * @param application      The application
     * @throws Exception       The exception
     */
    private static void attachApplication(Component component, Application application) 
        throws Exception
    {
        Guard.check(component, application);
        log_.debug("Attaching application to the RESTlet component");
        component.getDefaultHost().attach(application);
        component.start();
        log_.debug("RESTlet component started!");
    }
}
