package com.vmware.viclient.helper;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.HttpNfcLeaseInfo;
import com.vmware.vim25.HttpNfcLeaseDeviceUrl;
import com.vmware.vim25.OvfFile;
import com.vmware.vim25.OvfFileItem;
import com.vmware.vim25.OvfCreateImportSpecParams;
import com.vmware.vim25.OvfCreateImportSpecResult;
import com.vmware.vim25.OvfCreateDescriptorParams;
import com.vmware.vim25.OvfCreateDescriptorResult;
import com.vmware.vim25.VimService;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.UserSession;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.InvalidLocaleFaultMsg;
import com.vmware.vim25.InvalidLoginFaultMsg;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Iterator;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HttpsURLConnection;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

/**
 * VMware utility class developed for performing the following operations on a vCenterServer / Esxi host <br/>
 * 1. Fetch basic information and properties of managed objects <br/>
 * 2. Invoke basic operations on managed objects <br/>
 * Date Created: 01/16/2014 <br/><br/>
 * VimUtil could be invoked from the command line with the following parameters and operations <br/>
 * -url <'url to vCenter Server or Esxi host'> <br/>
 * -u <'username'> <br/>
 * -p <'password'> <br/>
 * [Available Operations] <br/> <br/>
 * -importVM <'hostname'> <'path to ovf file'> [new vm name] <br/>
 * -exportVM <'vmname'> <'localpath to ovf file'> <br/>
 * -powerOnVM <'vmname'> <br/>
 * -powerOffVM <'vmname'> <br/>
 * -createVM <br/>
 * -deleteVM <'vmname'> <br/>
 * -listVMs [hostname] <br/>
 * -listHosts <br/>
 * -listResourcePools <br/>
 * -listRP-VMs <br/>
 * -listHost-VMs <br/>
 * -installVMTools <'vmname'> <br/>
 * @author Kannan (kannanb@vmware.com), Rashmi Biswal (rbiswal@vmware.com)
 * @version 1.0
 */
public class VimUtil {
    volatile long TOTAL_BYTES_WRITTEN = 0;

    private VimService vimService;
    private VimPortType vimPort;
    private ServiceContent serviceContent;
    private UserSession userSession;
    private ManagedObjectReference svcInstRef;
    private Map headers = null;

    private URL url = null;
    private String username = null;
    private String password = null;

  /**
   * Managed entity of type HostSystem
   */
    public static final String HOSTSYSTEM = "HostSystem";
  /**
   * Managed entity of type VirtualMachine
   */
    public static final String VIRTUALMACHINE = "VirtualMachine";
  /**
   * Managed entity of type Datacenter
   */
    public static final String DATACENTER = "Datacenter";
  /**
   * Managed entity of type Datastore
   */
    public static final String DATASTORE= "Datastore";
  /**
   * Managed entity of type ResourcePool
   */
    public static final String RESOURCEPOOL = "ResourcePool";
  /**
   * Managed entity of type ComputeResource 
   */
    public static final String COMPUTERESOURCE = "ComputeResource";
  /**
   * Managed entity of type Folder
   */
    public static final String FOLDER = "Folder";
  /**
   * Managed entity of type Network
   */
    public static final String NETWORK = "Network";
  /**
   * Managed entity of type VirtualApp
   */
    public static final String VIRTUALAPP = "VirtualApp";

    public static final String [] MANAGEDENTITIES = {HOSTSYSTEM, VIRTUALMACHINE, DATACENTER, DATASTORE, RESOURCEPOOL, COMPUTERESOURCE, FOLDER, NETWORK, VIRTUALAPP};
    private String methodName = "";
    private boolean logEnabled = false;

  /**
   * Method to retrieve service instance name
   * @return service instance name
   */
    public String getServiceInstanceName() {
        return "ServiceInstance";
    }

    private ManagedObjectReference getServiceInstanceReference() {
        if (svcInstRef == null) {
            ManagedObjectReference ref = new ManagedObjectReference();
            ref.setType(this.getServiceInstanceName());
            ref.setValue(this.getServiceInstanceName());
            svcInstRef = ref;
        }
        return svcInstRef;
    }

   /** 
    * Use this method to use up your own connection and make use of this utility class as required
    * @param service
    * @param port 
    * @param content 
    * @param session
    */
    public void setConnection(VimService service, VimPortType port, ServiceContent content, UserSession session) {
       methodName = "setConnection";
       vimService =  service;
       vimPort = port;
       serviceContent = content;
       userSession = session;
    }

   /**
    * Method for creating SSL session with the Esxi host/ Vcenter Server //kannan
    */
    private void init() {
      try {
         HostnameVerifier hv = new HostnameVerifier() {
              public boolean verify(String urlHostName, SSLSession session) {
                 return true;
              }
         };
        javax.net.ssl.TrustManager [] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager tm = new TrustAllTrustManager();
        trustAllCerts[0] = tm;
        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
        javax.net.ssl.SSLSessionContext sslsc = sc.getServerSessionContext();
        sslsc.setSessionTimeout(0);
        sc.init(null, trustAllCerts, null);
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
      } catch (Exception e) {
          e.printStackTrace();
      }
    } 

    public void connect(String urlstr, String name, String pwd) throws Exception {
        try {
             url = new URL (urlstr);
        } catch (MalformedURLException e) {
             throw e;
        }
        username = name;
        password = pwd;
        connect();
    }

    private void connect() throws Exception {
        if (!isConnected()) {
            try {
                _connect();
            } catch (Exception e) {
                Throwable cause = (e.getCause() != null)?e.getCause():e;
                throw new Exception(
                        "failed to connect: " + e.getMessage() + " : " + cause.getMessage(),
                        cause);
            }
        }
    }

    private void _connect() throws RuntimeFaultFaultMsg, InvalidLocaleFaultMsg, InvalidLoginFaultMsg {
        init();
        System.out.println("\n Connecting to server [ " + url + " ].\n Please Wait... \n");
        vimService = new VimService();
        vimPort = vimService.getVimPort();
        Map<String, Object> ctxt =
                ((BindingProvider) vimPort).getRequestContext();

        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url.toString());
        ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        serviceContent = vimPort.retrieveServiceContent(this.getServiceInstanceReference());
        headers = (Map) ((BindingProvider) vimPort).getResponseContext().get( MessageContext.HTTP_RESPONSE_HEADERS);


        userSession = vimPort.login(
                serviceContent.getSessionManager(),
                username,
                password,
                null);
        String server = isVCenterServer() ? "VCenter server" : "Esxi Host" ;
        System.out.println("Successfully connected to " + server + " [ " + url + " ] ... \n");
    }

    public VimPortType getVimPort() {
        return vimPort;
    }

    public ServiceContent getServiceContent() {
        return serviceContent;
    }

  /**
   * Method to determine if the connection is made to a vCenter Server or an Esxi host
   * @return true if connected to vCenterServer, false if connected to an Esxi host
   */
    public boolean isVCenterServer() {
        String apiType = serviceContent.getAbout().getApiType();
	return apiType.equals("VirtualCenter");
    }

  /**
   * Method to determine if conection to vCenter Server or Esxi host is still active
   * @return true if connection exists, false otherwise
   */
    public boolean isConnected() {
        if (userSession == null) {
            return false;
        }
        long startTime = userSession.getLastActiveTime().toGregorianCalendar().getTime().getTime();

        // 30 minutes in milliseconds = 30 minutes * 60 seconds * 1000 milliseconds
        //return new Date().getTime() < startTime + 30 * 60 * 1000; //commentedK
	return true;
    }

  /**
   * Method to disconnect connection to the connected vCenter Server or Esi host
   */ 
    public void disconnect() throws Exception {
        if (this.isConnected()) {
            try {
                vimPort.logout(serviceContent.getSessionManager());
            } catch (Exception e) {
                Throwable cause = e.getCause();
                throw new Exception(
                        "failed to disconnect properly: " + e.getMessage() + " : " + cause.getMessage(),
                        cause
                );
            } finally {
                // A connection is very memory intensive, I'm helping the garbage collector here
                userSession = null;
                serviceContent = null;
                vimPort = null;
                vimService = null;
            }
        }
    }

   /**
    * Enable or disable log messages
    * @param flag to enable logging or not
    */
    public void enableLog(boolean flag) {
        logEnabled = flag;
    }

  /**
   * Method to print out all the available command line arguments and its parameters
   */
    public void help() {
        methodName = "help";
	System.out.println("VimUtil -help"); 
	System.out.println("=============\n"); 
	System.out.println("-url <url to vCenter Server or Esxi host>"); 
	System.out.println("-u <username>"); 
	System.out.println("-p <password> \n"); 
	System.out.println("[Available Operations] \n");
	System.out.println("-importVM <hostname> <path to ovf file> [new vm name]");
	System.out.println("-exportVM <vmname> <localpath to ovf file>");
	System.out.println("-powerOnVM <vmname>");
	System.out.println("-powerOffVM <vmname> ");
	System.out.println("-createVM");
	System.out.println("-deleteVM <vmname>");
	System.out.println("-listVMs [hostname]");
	System.out.println("-listHosts");
	System.out.println("-listResourcePools");
	System.out.println("-listRP-VMs");
	System.out.println("-listHost-VMs");
	System.out.println("-installVMTools <vmname>");
	System.out.println("\n====================\n"); 
    }

    private void parseArgs(String [] args) {
       methodName =  "parseArgs";
       String logE = System.getProperty("logEnabled", "false");
       if (logE != null && logE.toLowerCase().equals("true")) {
           logEnabled = true;
       }
       int index= 0;
       int argCount =  args.length;
       boolean listVMs = false;
       boolean listHosts = false;
       boolean listResourcePools = false;
       boolean listHostVMs = false;
       boolean listRPVMs= false;
       boolean exportVM = false;
       boolean importVM = false;
       boolean powerOnVM = false;
       boolean powerOffVM = false;
       boolean deleteVM = false;
       boolean createVM = false;
       boolean installvmtools = false;

       String urlName = null;
       String vmname = null;
       String hostname = null;
       String localpath = null;
       String ovfFile = null;

       while (index < argCount) {
         String arg = args[index].toLowerCase();
	 if (arg.equals("-url")) {
             if (! validateNextArg(args, index+1, argCount, "1 Please specify url to vCenter Server or Esxi Host")) {
                 return;
	     }
             urlName = args[index+1];
             index++;
	 } else if (arg.equals("-u")) {
             if (! validateNextArg(args, index+1, argCount, "Please provide username")) {
                 return;
	     }
             username = args[index+1];
             index++;
	 } else if (arg.equals("-p")) {
             if (! validateNextArg(args, index+1, argCount, "Please provide password")) {
                 return;
	     }
             password = args[index+1];
	     index++;
	 } else if (arg.equals("-listvms")) {
             listVMs = true;
             if (validateNextArg(args, index+1, argCount, "")) {
                 hostname = args[index+1];
                 index++;
	     }
	 } else if (arg.equals("-listhosts")) {
             listHosts = true;
	 } else if (arg.equals("-listresourcepools")) {
             listResourcePools = true;
	 } else if (arg.equals("-listrp-vms")) {
             listRPVMs = true;
	 } else if (arg.equals("-listhost-vms")) {
             listHostVMs = true;
	 } else if (arg.equals("-exportvm")) {
             exportVM = true;
             if (! validateNextArg(args, index+1, argCount, "Please provide vmname for exportVM operation")) {
                 return;
	     }
             vmname = args[index+1];
	     index++;
             if (! validateNextArg(args, index+1, argCount, "Please provide localpath for exportVM operation")) {
                 return;
	     }
             localpath = args[index+1];
	     index++;
	 } else if (arg.equals("-importvm")) {
             importVM = true;
             if (! validateNextArg(args, index+1, argCount, "Please provide hostname for importVM operation")) {
                 return;
	     }
             hostname = args[index+1];
	     index++;
             if (! validateNextArg(args, index+1, argCount, "Please provide path to ovffile for importVM operation")) {
                 return;
	     }
             localpath = args[index+1];
	     index++;
             if (! validateNextArg(args, index+1, argCount, "Please provide new vmname for import vm operation")) {
                 return;
	     }
             vmname = args[index+1];
	     index++;
	 } else if (arg.equals("-createvm")) {
             createVM = true;
	 } else if (arg.equals("-deletevm")) {
             deleteVM = true;
             if (! validateNextArg(args, index+1, argCount, "Please provide vmname for deleteVM operation")) {
                 return;
	     }
             vmname = args[index+1];
	     index++;
	 } else if (arg.equals("-poweronvm")) {
             powerOnVM = true;
             if (! validateNextArg(args, index+1, argCount, "Please provide vmname for power on operation")) {
                 return;
	     }
             vmname = args[index+1];
	     index++;
	 } else if (arg.equals("-poweroffvm")) {
             powerOffVM = true;
             if (! validateNextArg(args, index+1, argCount, "Please provide vmname for power off operation")) {
                 return;
	     }
             vmname = args[index+1];
	     index++;
	 } else if (arg.equals("-installvmtools")) {
             installvmtools = true;
             if (! validateNextArg(args, index+1, argCount, "Please provide vmname for installvmtools operation")) {
                 return;
	     }
             vmname = args[index+1];
	     index++;
	 } else {
             help();
	     return;
	 }
	 index++;
       }

       if (urlName == null) {
           System.err.println("2 Please specify url to VCenter Server or Esxi Host");
           return;          
       } else {
           try {
               url = new URL (urlName);
           } catch (MalformedURLException e) {
               System.err.println("Invalid url: " + url);
               return;
           }
       }
       if (username == null) {
           System.err.println("Please provide username");
           return;          
       }
       if (password == null) {
           System.err.println("Please provide password");
           return;          
       }

       try {
            _connect();
       } catch (Exception e) {
            System.err.println("Connection failed !");
            return;
       }

       if (listVMs) {
           List<String> list = null;
           if (hostname != null) {
               list = (List<String>)getVMListFromHost(hostname, true);
	       System.out.println("Printing all VMs in host [ " + hostname + " ] ....");
	   } else {
               list = getList(VIRTUALMACHINE);
	       System.out.println("Printing all VMs....");
	   }
	   if (list != null && !list.isEmpty()) {
               for (String vm : list) {
                    System.out.println(vm);
               }
	   } else {
               System.out.println("No VMs found...");
	   }
	   System.out.println();
       }
       if (listHosts) {
           List<String> list = getList(HOSTSYSTEM);
	   System.out.println("Printing all Hosts....");
	   for (String host : list) {
              System.out.println(host);
 	   }
	   System.out.println();
       }
       if (listResourcePools) {
           List<String> list = getList(COMPUTERESOURCE);
	   System.out.println("Printing all ResourcePool....");
	   if (list == null || list.isEmpty()) {
               System.err.println("No compute resource available and hence no resourcepools found !");
	   } else  {
	       for (String cr : list) {
                    ManagedObjectReference mor = getMORFromEntityName(COMPUTERESOURCE, cr);
		    Map<String, Object> map = getProperties(mor, new String[] {"resourcePool"});
		    if (map != null && !map.isEmpty()) {
                        if (map.get("resourcePool") != null) {
		            System.out.println(((ManagedObjectReference)map.get("resourcePool")).getValue());
		        }
		    }
	       }
           }
	   System.out.println();
       }
       if (listRPVMs) {
	   System.out.println("Printing all ResourcePool to VM mapping....");
           Map<String, Object> map = getRPtoVMMap();
	   if (map == null || map.isEmpty()) {

               System.err.println("No compute resource available and hence no resourcepools found !");
	   } else {
               Iterator iter = map.keySet().iterator();
	       while (iter.hasNext()) {
                   String rpKey = (String)iter.next();
		   Vector<String> vmNames = (Vector<String>)map.get(rpKey);
                   System.out.println("Resource Pool: " + rpKey);
                   System.out.println("================");
		   if (vmNames != null && ! vmNames.isEmpty()) {
		       for (String vm : vmNames) {
                            System.out.println(vm);
		       }
		   }
	       }
	   }
	   System.out.println();
       }
       if (powerOnVM) {
	   System.out.println("Initiating power on operation on [ " + vmname + " ] ....");
           powerOnVM(vmname, null);
	   System.out.println();
       }
       if (powerOffVM) {
	   System.out.println("Initiating power off operation on [ " + vmname + " ] ....");
           powerOffVM(vmname, null);
	   System.out.println();
       }
       if (exportVM) {
	   System.out.println("Initiating export vm operation on [ " + vmname + " ] to folder [ " + localpath + " ] ....");
           exportVM(vmname, localpath);
	   System.out.println();
       }
       if (importVM) {
	   System.out.println("Initiating import vm operation on ovffile [ " + localpath + " ] to host [ " + hostname + " ]  ....");
           importVM(localpath, hostname, vmname);
	   System.out.println();
       }
       if (installvmtools) {
	   System.out.println("Initiating install vmware tools operation on [ " + vmname + " ] ....");
           installVMWareTools(vmname);
	   System.out.println();
       }
       if (deleteVM) {
	   System.out.println("Initiating delete vm operation on [ " + vmname + " ]  ....");
           deleteVM(vmname);
	   System.out.println();
       }
       if (createVM) {
	   System.out.println("Initiating create vm operation ....");
	   System.out.println("create vm operation under construction....");
	   System.out.println();
       }
       if (listHostVMs) {
	   System.out.println("Listing Host to VM mapping....");
	   List<String> hostList = getList(HOSTSYSTEM);
	   if (hostList == null || hostList.isEmpty()) {
               System.err.println("No hosts found...");
	   } else {
               for (String host : hostList) {
                   List<String> vmList = (List<String>)getVMListFromHost(host, true);
		   System.out.println("Listing VMs in Host [ " + host + " ] ");
		   System.out.println("===================");
		   for (String vm : vmList) {
                        System.out.println(vm);
		   }
                   System.out.println();
	       }
	   }
	   System.out.println();
       }
    }

   /**
    * Method to determine if the arg is a command. All command line arguments must begin with a '-'
    * Note: All commands must begin with '-'
    * @param arg command line argument
    * @return true if the argument is a command, false otherwise
    */
    private boolean isCommandLineArg(String arg) {
       methodName = "isCommandLineArg";
       return (arg != null && arg.startsWith("-"));
    }

   /**
    * Method to determine if next argument is a parameter to the previous command or a command itself
    * @param args
    * @param index
    * @param argCount
    * @param errMsg
    * @return  true if the next argument is a parameter to the previous parsed command
    */
    private boolean validateNextArg(String [] args, int index, int argCount, String errMsg) {
        methodName = "validateNextArg";
        if (index < argCount) {
            return ! isCommandLineArg(args[index]);
	}
        System.err.println(errMsg);
        return false;
    } 

   //main method
   public static void main (String [] args) throws Exception {
        VimUtil obj = new VimUtil();
	obj.parseArgs(args);
    }

    private static class TrustAllTrustManager implements TrustManager, X509TrustManager {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
          }
          public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
              return true;
          }
          public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
              return true;
          }
          public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) throws java.security.cert.CertificateException {
               return;
          }
          public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) throws java.security.cert.CertificateException {
               return;
          }
    }

   /**
    * Method for getting list of all VMs on a given hostname 
    * @param hostName name of the host system
    * @param retName set it to true if list of all VM names are to be returned. set to false to return list of all VM MORs
    * @return list of MORs or names of all matched VMs depending on retName parameter
    */
    public List<?> getVMListFromHost(String hostName, boolean retName) {
          methodName = "getVMListFromHost";
          boolean bHostFound = false;
      try {
          ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
          ManagedObjectReference propCol = serviceContent.getPropertyCollector();
          List <String> hostList = new ArrayList<String>();
          hostList.add(HOSTSYSTEM);
          ManagedObjectReference viewRef = vimPort.createContainerView(viewMgrRef, serviceContent.getRootFolder(), hostList, true);
          ObjectSpec oSpec = new ObjectSpec();
          oSpec.setObj(viewRef);
          oSpec.setSkip(true);

          TraversalSpec tSpec = new TraversalSpec();
          tSpec.setName("traverseEntities");
          tSpec.setPath("view");
          tSpec.setSkip(false);
          tSpec.setType("ContainerView");

          oSpec.getSelectSet().add(tSpec);

          PropertySpec pSpec = new PropertySpec();
          pSpec.setType(HOSTSYSTEM);
          pSpec.getPathSet().add("vm");
          pSpec.getPathSet().add("name");
  
          PropertyFilterSpec fSpec = new PropertyFilterSpec();
          fSpec.getObjectSet().add(oSpec);
          fSpec.getPropSet().add(pSpec);

          RetrieveOptions ro = new RetrieveOptions();
          List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
          fSpecList.add(fSpec);

          RetrieveResult props = vimPort.retrievePropertiesEx(propCol, fSpecList, ro);

          if (props == null) {
              logOutput("Warning! No properties retrieved for HostSystem");
              return null;
	  }
          for (ObjectContent oc : props.getObjects()) {
               ArrayOfManagedObjectReference vms = null;
               List <DynamicProperty> dps = oc.getPropSet();
               bHostFound = false;
               if (dps != null) {
                   for (DynamicProperty dp : dps) {
                        String propName = dp.getName();
                        if (propName.equals("name") && dp.getVal().equals(hostName)) {
                            bHostFound = true;
                        } else if (propName.equals("vm")) {
                            vms = (ArrayOfManagedObjectReference)dp.getVal();
                        }
                    }
               }
               if (bHostFound) {
                   if (vms != null) {
                       logOutput("Listing VMs on Esxi host [ " + hostName + " ] " );
                       List<ManagedObjectReference> vmlist = ((ArrayOfManagedObjectReference)vms).getManagedObjectReference();
		       if (!retName) {
                           return vmlist;
		       }
                       if (vmlist != null) {
                           List<String> vmnames = new Vector<String>();
                           for (ManagedObjectReference vm : vmlist) {
                                Map<String,  Object> map = getProperties(vm, new String [] {"name"} );
				vmnames.add((String)map.get("name"));
                           }
                           return vmnames;
                       }
                    }
                }
             }
      } catch (Exception e) {
         e.printStackTrace();
      }
      if (!bHostFound) {
          System.err.println("Host [ " + hostName + " ] not found...");
      }
      return null;
    }
 
   /**
    * Method to get a map of ResourcePool to VM mapping
    * @return a map whose key is the ResourcePool name and value is a Vector<'String'> of vmnames belonging to the respective ResourcePool
    */
    public Map<String, Object> getRPtoVMMap() {
        methodName = "getRPtoVMMap";
        Map<String, Object> rpMap = new HashMap<String, Object>();
        List<String> list = getList(COMPUTERESOURCE);
        if (list == null || list.isEmpty()) {
           return null;
        }
        for (String cr : list) {
             ManagedObjectReference mor = getMORFromEntityName(COMPUTERESOURCE, cr);
             Map<String, Object> map = getProperties(mor, new String[] {"resourcePool"});
	     if (map != null && !map.isEmpty()) {
                 ManagedObjectReference rpRef = (ManagedObjectReference)map.get("resourcePool");
                 if (rpRef != null) {
                     String rpName = (String)((ManagedObjectReference)map.get("resourcePool")).getValue();
	             Map<String, Object> rpmap = getProperties(rpRef, new String[] {"vm"});
	             if (rpmap != null && !rpmap.isEmpty()) {
                         ArrayOfManagedObjectReference vmarray = (ArrayOfManagedObjectReference) rpmap.get("vm");
		         if (vmarray == null ) continue;  
                             List<ManagedObjectReference> vmlist = vmarray.getManagedObjectReference();
			     Vector<String> vmnames = new Vector<String>();
                             for (ManagedObjectReference vm : vmlist) {

                                  Map<String, Object> vmmap = getProperties(vm, new String [] {"name"});
			          logOutput("vm name:= " + vmmap.get("name"));
				  vmnames.add((String)vmmap.get("name"));
                             }
			     rpMap.put(rpName, vmnames);
                          }
                      }
                  }
          }
	return rpMap;
    }

    public Object getProperty(ManagedObjectReference ref, String prop) {
        methodName = "getProperty";
	Map<String, Object> map = getProperties(ref, new String[]{prop});
	return map == null ? null : map.get(prop);
    }

   /**
    * Method to retrieve all or specific properties of a given entity 
    * @param entityType type of the managed entity
    * @param entityName name of the managed entity
    * @param props specific list of properties to be retrieved. If passed null, the method will retrieve all properties
    * @return map containing property name as the key and the property's value as the value
    */
    public Map<String, Object> getProperties(String entityType, String entityName, String [] props) {
	   methodName = "getProperties0";
           ManagedObjectReference mor = getMORFromEntityName(entityType, entityName);
	   if (mor == null) {
               System.err.println("getProperties - MOR for " + entityType + " [ " + entityName + " ] is null ...");
	       return null;
	   }
	   return getProperties(mor, props);
    }

   /**
    * Method to retrieve all or specific properties of a given managed entity //kannan
    * @param ref MOR of the entity whose properties need to be retrieved
    * @param props specific list of properties to be retrieved. If passed null, the method will retrieve all properties
    * @return map containing property name as the key and the property's value as the value
    */
    public Map<String, Object> getProperties(ManagedObjectReference ref, String [] props) {
       methodName = "getProperties1";
       RetrieveResult rlist = null;
       Map<String, Object> map = new HashMap<String, Object>();
       try {
            ManagedObjectReference propCol = serviceContent.getPropertyCollector();

            ObjectSpec oSpec = new ObjectSpec();
            oSpec.setObj(ref);
            oSpec.setSkip(Boolean.FALSE);

            PropertySpec pSpec = new PropertySpec();
            pSpec.setAll( (props == null || props.length == 0) ? true : false );
            pSpec.setType(ref.getType());
            if (props != null) {
                for (String prop : props) {
                     pSpec.getPathSet().add(prop);
                }
            }

            PropertyFilterSpec pfSpec = new PropertyFilterSpec();
            pfSpec.getObjectSet().add(oSpec);
            pfSpec.getPropSet().add(pSpec);

            List<PropertyFilterSpec> specList = new ArrayList<PropertyFilterSpec>();
            specList.add(pfSpec);

            RetrieveOptions ro = new RetrieveOptions();
            rlist = vimPort.retrievePropertiesEx(propCol, specList, ro);
       } catch (Exception e) {
            e.printStackTrace();
            return null;
       }
       if (rlist != null) {
           List <ObjectContent> list = rlist.getObjects();
           for (ObjectContent obj : list) {
                List<DynamicProperty> properties = obj.getPropSet();
                if (properties != null) {
                    for (DynamicProperty prop : properties) {
			 if (prop.getVal() instanceof ManagedObjectReference) {

			     ManagedObjectReference mor = (ManagedObjectReference)prop.getVal();
                             logOutput("Property: [ " + mor.getType() + " = " + mor.getValue() + " ] ");
		         } else {
                             logOutput("Property: [ " + prop.getName() + " = " + prop.getVal() + " ] ");
			 }
                         map.put(prop.getName(), prop.getVal());
                    }
                }
           }
       }
       return map;
    }

   /**
    * Method to retrieve parent name of a given entity name
    * @param entityType managed entity type
    * @param entityName managed entity name
    * @return parent entity name of the requested entity
    */
    public String getParentName(String entityType, String entityName) {
        methodName = "getParentName0";
        ManagedObjectReference mor = getMORFromEntityName(entityType, entityName);
        if (mor == null) {
            logOutput("MOR for entity is [ " + entityName + " ] is null...");
            return null;
	}
        return getParentName(mor);
    }

   /**
    * Method to retrieve parent name of a given MOR
    * @param mor MOR of the requested entity
    * @return parent entity name of the requested entity
    */
    public String getParentName(ManagedObjectReference mor) {
        methodName = "getParentName1";
	ManagedObjectReference pmor = getParentMOR(mor);
	Map<String, Object> map = getProperties(pmor, new String[] {"name"});
	return (String)map.get("name");
    }

   /**
    * Method to retrieve parent MOR of a given MOR
    * @param mor MOR of the requested entity
    * @return parent MOR of the requested entity
    */
    public ManagedObjectReference getParentMOR(ManagedObjectReference mor) {
        methodName = "getParentMOR";
        Map<String, Object> map = getProperties(mor, new String[] {"parent"});
        return (ManagedObjectReference)map.get("parent");
    }

   /**
    * Get list of available managed entities from vCenter Server or Esxi host 
    * @param entityType managed entity type
    * @return list containing names of all managed entities of type entityType
    */
    public List<String> getList(String entityType) {
          methodName = "getList"; 
          List<String> mList = new ArrayList<String>();
      try {
          ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
          ManagedObjectReference propCol = serviceContent.getPropertyCollector();
          List <String> entityList = new ArrayList<String>();
          entityList.add(entityType);
          ManagedObjectReference viewRef = vimPort.createContainerView(viewMgrRef, serviceContent.getRootFolder(), entityList, true);
          ObjectSpec oSpec = new ObjectSpec();
          oSpec.setObj(viewRef);
          oSpec.setSkip(true);

          TraversalSpec tSpec = new TraversalSpec();
          tSpec.setName("traverseEntities");
          tSpec.setPath("view");
          tSpec.setSkip(false);
          tSpec.setType("ContainerView");

          oSpec.getSelectSet().add(tSpec);

          PropertySpec pSpec = new PropertySpec();
          pSpec.setType(entityType);
          pSpec.getPathSet().add("name");
  
          PropertyFilterSpec fSpec = new PropertyFilterSpec();
          fSpec.getObjectSet().add(oSpec);
          fSpec.getPropSet().add(pSpec);

          RetrieveOptions ro = new RetrieveOptions();
          List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
          fSpecList.add(fSpec);

          RetrieveResult props = vimPort.retrievePropertiesEx(propCol, fSpecList, ro);

          if (props != null) {
             logOutput("Listing managed entities of type [ " + entityType + " ] on the server : " + url);
             for (ObjectContent oc : props.getObjects()) {
                 String value = null;
                 String propName = null;
                 List <DynamicProperty> dps = oc.getPropSet();
                 if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        value = (String)dp.getVal();
                        propName = dp.getName();
                        mList.add(value);
                        logOutput(propName + " = " + value);
                    }
                 }
             }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return mList;
   }

  /**
   * Method to get the MOR for a given entity type and entity name
   * @param entityType managed entity type
   * @param entityName managed entity name
   * @return MOR of the requested managed entity type and its name
   */
   public ManagedObjectReference getMORFromEntityName(String entityType, String entityName) {
      methodName = "getMORFromEntityName"; 
      ManagedObjectReference entityMOR = null; 
      try {
          ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
          ManagedObjectReference propCol = serviceContent.getPropertyCollector();
          List <String> entityList = new ArrayList<String>();
          entityList.add(entityType);
          ManagedObjectReference viewRef = vimPort.createContainerView(viewMgrRef, serviceContent.getRootFolder(), entityList, true);
          ObjectSpec oSpec = new ObjectSpec();
          oSpec.setObj(viewRef);
          oSpec.setSkip(true);

          TraversalSpec tSpec = new TraversalSpec();
          tSpec.setName("traverseEntities");
          tSpec.setPath("view");
          tSpec.setSkip(false);
          tSpec.setType("ContainerView");

          oSpec.getSelectSet().add(tSpec);

          PropertySpec pSpec = new PropertySpec();
          pSpec.setType(entityType);
          pSpec.getPathSet().add("name");
  
          PropertyFilterSpec fSpec = new PropertyFilterSpec();
          fSpec.getObjectSet().add(oSpec);
          fSpec.getPropSet().add(pSpec);

          RetrieveOptions ro = new RetrieveOptions();
          List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
          fSpecList.add(fSpec);

          RetrieveResult props = vimPort.retrievePropertiesEx(propCol, fSpecList, ro);

          if (props != null) {
             for (ObjectContent oc : props.getObjects()) {
                 List <DynamicProperty> dps = oc.getPropSet();
                 if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        if (dp.getVal().equals(entityName)) {
                            entityMOR = oc.getObj();
                            logOutput("MOR for Entity [ " + entityName + " ] found: " + entityMOR + "\n");
                        }
                    }
                 }
             }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      if (entityMOR == null) {
          System.err.println("Cannot retrieve MOR for [ " + entityName + " ] ...\n");
      }
      return entityMOR;
   }

   /**Method to retrieve VM MOR from a given host name and vm name
    * @param hostName name of the hostsystem
    * @param vmName name of the virtual machine
    * @return MOR of the virtual machine residing in the requested hostName
    */
   public ManagedObjectReference getVMMORFromHostName(String hostName, String vmName) {
           methodName = "getVMMORFromHostName";
	   ManagedObjectReference hostMOR = getMORFromEntityName(HOSTSYSTEM, hostName);
	   if (hostMOR == null) {
               System.err.println("getVMMORFromHostName - Cannot get MOR for hostsystem: " + hostName);
               return null;
	   }
	   return getVMMORFromHostSystem(hostMOR, vmName);
   }

   //Method to retrieve VM MOR from a given host MOR and vm name
   public ManagedObjectReference getVMMORFromHostSystem(ManagedObjectReference hostRef, String vmName) {
          methodName = "getVMMORFromHostSystem";
          Map<String, Object> map = getProperties(hostRef, new String[] {"vm"});
	  if (map == null || map.isEmpty()) {
              logOutput("No VMs available in host: " + hostRef);
              return null;
	  }
	  ArrayOfManagedObjectReference vmArray = (ArrayOfManagedObjectReference)map.get("vm");
	  if (vmArray == null || vmArray.getManagedObjectReference().isEmpty()) {
              logOutput("Cannot retrieve VMs available in host: " + hostRef);
              return null;
	  }
	  List<ManagedObjectReference> vms = vmArray.getManagedObjectReference();
          for (ManagedObjectReference vm : vms) {
               Map <String, Object> vmap = getProperties(vm, new String[]{"name"});
	       if (vmap == null || vmap.isEmpty()) continue;
               String vmname = (String)vmap.get("name");
	       if (vmname != null && vmname.equals(vmName)) {
                   return vm;
	       }
	  }
	  return null;
   }


   public ManagedObjectReference [] getAllEntities() {
          try {
               if (list != null && list.isEmpty()) {
                   list.clear();
               }
               ManagedObjectReference rootFolder = serviceContent.getRootFolder();
               ArrayOfManagedObjectReference morArray = (ArrayOfManagedObjectReference)getProperty(rootFolder, "childEntity");
               if (morArray == null) {
                   return null;
               }
               List<ManagedObjectReference> morList = new Vector<ManagedObjectReference>();
               morList.add(rootFolder);
               List<ManagedObjectReference> rootList = morArray.getManagedObjectReference();
               ManagedObjectReference [] morefs = getArray(rootList);
               for (ManagedObjectReference mor : morefs) {
                    morList.add(mor);
                    List<ManagedObjectReference> cList = getChildEntities(mor);
                    if (list != null && !list.isEmpty()) {
                        //System.out.println("cList1 : " + list.size());
                        morList.addAll(list);
                    }
               }
               return getArray(morList);
          } catch (Exception e) {
               e.printStackTrace();
          }
          return null;
    }

    List<ManagedObjectReference> list = null;
    public List<ManagedObjectReference> getChildEntities (ManagedObjectReference mor) {
        List<ManagedObjectReference> childList = null;
        if (mor.getType().equals("Folder")) {
            ArrayOfManagedObjectReference array = (ArrayOfManagedObjectReference)getProperty(mor, "childEntity");
            List<ManagedObjectReference> tlist = array.getManagedObjectReference();
            ManagedObjectReference [] morefs = getArray(tlist);
            for (ManagedObjectReference cmor : morefs) {
                 list.add(cmor);
                 childList = getChildEntities(cmor);
                 if (childList != null && !childList.isEmpty()) {
                     list.addAll(childList);
                 }
            }
            return childList;
        }
         //System.out.println("getC1 : " + mor.getType() + " = " + mor.getValue());
        if (list == null) {
            list = new Vector<ManagedObjectReference>();
            //System.err.println("new list created...");
        }
		
        if (mor.getType().equals("Datacenter")) {
            ManagedObjectReference cFolder = (ManagedObjectReference)getProperty(mor, "datastoreFolder");
            //System.out.println("getC2 : " + cFolder);
            if (cFolder != null) {
                list.add(cFolder);
                childList = getChildEntities(cFolder);
	        if (childList != null && !childList.isEmpty()) {
                    list.addAll(childList);
                }
             }

             cFolder = (ManagedObjectReference)getProperty(mor, "hostFolder");
             //System.out.println("getC3 : " + cFolder);
             if (cFolder != null) {
                 list.add(cFolder);
                 childList = getChildEntities(cFolder);
                 if (childList != null && !childList.isEmpty()) {
                     list.addAll(childList);
                 }
             }

             cFolder = (ManagedObjectReference)getProperty(mor, "networkFolder");
             //System.out.println("getC4 : " + cFolder);
             if (cFolder != null) {
                 list.add(cFolder);
                 childList = getChildEntities(cFolder);
                 if (childList != null && !childList.isEmpty()) {
                     list.addAll(childList);
                 }
             }

             cFolder = (ManagedObjectReference)getProperty(mor, "vmFolder");
             if (cFolder != null) {
                 list.add(cFolder);
                 childList = getChildEntities(cFolder);
                 if (childList != null && !childList.isEmpty()) {
                     list.addAll(childList);
                 }
             }
       } else if (mor.getType().equals("ComputeResource")) {
                  ManagedObjectReference rpRef = (ManagedObjectReference)getProperty(mor, "resourcePool");
             if (rpRef != null) {
                 list.add(rpRef);
             }
             ArrayOfManagedObjectReference hostArray = (ArrayOfManagedObjectReference)getProperty(mor, "host");
             List<ManagedObjectReference> hList = hostArray.getManagedObjectReference();
             ManagedObjectReference [] hostRefs = getArray(hList);
             for (ManagedObjectReference href : hostRefs) {
                  list.add(href);
             }
        }
        return childList;
     }

  /**
   * Method to power ON a given VM name on a specific host name.
   * @param vmName name of the virtual machine that needs to be powered on
   * @param hostName name of the host where the requested vmName resides. This parameter is optional. 
   */
   public void powerOnVM(String vmName, String hostName) {
	  methodName = "powerOnVM";
          ManagedObjectReference hostMOR = null;
          ManagedObjectReference vmMOR = null;
          try {
               if (hostName != null) {
                   hostMOR = getMORFromEntityName(HOSTSYSTEM, hostName);
                   vmMOR = getVMMORFromHostName(hostName, vmName);
	       } else {
                   vmMOR = getMORFromEntityName(VIRTUALMACHINE, vmName);

	       }
               if (vmMOR != null) {
                   ManagedObjectReference task = vimPort.powerOnVMTask(vmMOR, hostMOR); 
	           if (task != null) {
                       while (true) {
                           Map<String, Object> map = getProperties(task, new String [] {"info.state"});
                           TaskInfoState state = (TaskInfoState)map.get("info.state");
			   logOutput("VM: " + vmName + " is in " + state + " state ");
			   if (state == TaskInfoState.SUCCESS || state == TaskInfoState.ERROR) {
                               if (state == TaskInfoState.SUCCESS) {
                                   System.out.println("powerOnVM - VM powered on sucessfully...");
			       } else {
                                   System.out.println("powerOnVM - VM couldn't be powered on ...");
			       }
                               break;
			   }
                           Thread.sleep(1000);
			}
		     }
		   }

	  } catch (Exception e) {
               e.printStackTrace();
	  }
   }

  /**
   * Method to power OFF a given VM name on a specific host name. 
   * @param vmName name of the virtual machine that needs to be powered off
   * @param hostName name of the host where the requested vmName resides. This parameter is optional. 
   */
   public void powerOffVM(String vmName, String hostName) {
	  methodName = "powerOffVM";
          ManagedObjectReference hostMOR = null;
          ManagedObjectReference vmMOR = null;
          try {
               if (hostName != null) {
                   hostMOR = getMORFromEntityName(HOSTSYSTEM, hostName);
                   vmMOR = getVMMORFromHostName(hostName, vmName);
	       } else {
                   vmMOR = getMORFromEntityName(VIRTUALMACHINE, vmName);

	       }
               if (vmMOR != null) {
                   ManagedObjectReference task = vimPort.powerOffVMTask(vmMOR); 
	           if (task != null) {
                       while (true) {
                           Map<String, Object> map = getProperties(task, new String [] {"info.state"});
                           TaskInfoState state = (TaskInfoState)map.get("info.state");
			   if (state == TaskInfoState.SUCCESS || state == TaskInfoState.ERROR) {
                               if (state == TaskInfoState.SUCCESS) {
                                   System.out.println("powerOffVM - VM powered off sucessfully...");
			       } else {
                                   System.out.println("powerOffVM - VM couldn't be powered off ...");
			       }
                               break;
			   }
                           Thread.sleep(1000);
			}
		     }
		   }

	  } catch (Exception e) {
               e.printStackTrace();
	  }
   }

  /**
   * Method to import a ovf file into a requested host
   * @param ovfFilePath path to ovf file that needs to be imported
   * @param hostName host system where the ovf file needs to be imported to
   * @param newVmName name of the virtual machine that needs to be given on deploying the ovf
   */
   public void importVM(String ovfFilePath, String hostName, String newVmName) {
      methodName = "importVM";
      try {
           ManagedObjectReference ovfMgrRef = serviceContent.getOvfManager();
           ManagedObjectReference dsRef =  null;
           ManagedObjectReference dcRef =  null;
           ManagedObjectReference rpRef =  null;
           ManagedObjectReference vmFolderRef =  null;

           ManagedObjectReference hostRef = getMORFromEntityName(HOSTSYSTEM, hostName);
           if (hostRef == null) {
               System.err.println("Fatal: Cannot get host MOR for " + hostName + " ... Exiting!");
               return;
           }

           Map<String, Object> hostMap = getProperties(hostRef, null);
           if (hostMap != null && !hostMap.isEmpty()) {
               Object o = hostMap.get("datastore");
               if ( o != null && o instanceof ArrayOfManagedObjectReference ) {
                   List<ManagedObjectReference> dsRefs =  ((ArrayOfManagedObjectReference)o).getManagedObjectReference();
                   dsRef = dsRefs.get(0);
		   if (dsRef == null) {
                       System.err.println("Fatal: No datastore found! Exiting...");
                       return;
		   }
		   logOutput("RP Reference: " + dsRef.getValue());


                   dcRef = getManagedEntity("Datacenter", dsRef);
		   if (dcRef == null) {
                       System.err.println("Fatal: Datacenter not found! Exiting...");
                       return;
		   }
                   Map<String, Object> dcMap = getProperties(dcRef, new String [] {"vmFolder"});
                   vmFolderRef = (ManagedObjectReference)dcMap.get("vmFolder");
		   if (vmFolderRef == null) {
                       System.err.println("Warning: vmFolder is null ! ");
		   }


                   OvfCreateImportSpecParams iParams = new OvfCreateImportSpecParams();
                   iParams.setHostSystem(hostRef);
                   iParams.setLocale("");
                   iParams.setEntityName(newVmName);
                   iParams.setDeploymentOption("");

                   String ovfDesc = getOvfDesc(ovfFilePath);
                   if (ovfDesc == null || ovfDesc.isEmpty()) {
                       System.err.println("Fatal: ovfDesc is empty. Exiting... ");
                       return;
                   }
		   //logOutput("ovf desc: " + ovfDesc);
              
                   rpRef = getResourcePool(hostRef);
                   if (rpRef == null) {
                       System.err.println("Fatal: Resource Pool not found! Exiting... ");
                       return;
                   }
		   logOutput("Resource pool ref : "+ rpRef);
                   OvfCreateImportSpecResult ovfRes = vimPort.createImportSpec(ovfMgrRef, ovfDesc, rpRef, dsRef, iParams);
                   if (ovfRes == null) {
                       System.err.println("Fatal: ovfRes is null. Exiting... ");
                       return;
                   }
                   List<OvfFileItem> fileArr = ovfRes.getFileItem();
		   if (fileArr == null || fileArr.isEmpty()) {
                       System.err.println("Warning!  ovf file item is empty...");
		   }
		   logOutput("file attr : " + fileArr);
                   if (fileArr != null) {
                       for (OvfFileItem file : fileArr) {
                            logOutput("filepath: " + file.getPath() + " : size = " + file.getSize());
                            logOutput("deviceId: " + file.getDeviceId());
                            logOutput("chunk size: " + file.getChunkSize());
                            logOutput("cim Type: " + file.getCimType());
                            logOutput("compression method: " + file.getCompressionMethod());
                       }
                   }
                   logOutput("ImportSpec " + ovfRes.getImportSpec().getInstantiationOst().getId() + " - " + ovfRes.getImportSpec().getInstantiationOst().getType() );
                   ManagedObjectReference httpNfcLease = vimPort.importVApp(rpRef, ovfRes.getImportSpec(), vmFolderRef, hostRef);
		   if (httpNfcLease == null) {
                       System.err.println("Fatal: httpNfcLease is null. Exiting...");
                       return;
		   }
                   logOutput("httpNfcLease : " + httpNfcLease + " : " + httpNfcLease.getType() + " : " + httpNfcLease.getValue());
                   Map <String, Object> nmap = getProperties(httpNfcLease, new String [] {"state", "initializeProgress"});
		   System.out.println("NfcLeaseState: " + nmap.get("state") + " = " + nmap.get("initializeProgress"));
                    System.out.println("Please Wait......................");
		    int result = waitOnLease(httpNfcLease);

            if (result == 0) { //lease is in ready state //kannan
                HttpNfcLeaseInfo httpNfcLeaseInfo = null;
                Map<String, Object> leaseMap = getProperties(httpNfcLease, new String [] {"info"});
                if (leaseMap != null && !leaseMap.isEmpty()) {
                    httpNfcLeaseInfo = (HttpNfcLeaseInfo)leaseMap.get("info");
                }
		if (httpNfcLeaseInfo == null) {
                    System.err.println("Fatal: httpNfcLeaseInfo is null. Check if a VM with the same name [ " + newVmName + " ] already exists.   Exiting...");
                    return;
		}
                logOutput("HttpNfcLeaseInfo: " + httpNfcLeaseInfo);
                List<HttpNfcLeaseDeviceUrl> deviceUrlArr =
                        httpNfcLeaseInfo.getDeviceUrl();
                for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrlArr) {
                    String deviceKey = deviceUrl.getImportKey();
                    for (OvfFileItem ovfFileItem : fileArr) {
                        if (deviceKey.equals(ovfFileItem.getDeviceId())) {
                            logOutput("Import key: " + deviceKey);
                            logOutput("OvfFileItem device id: " + ovfFileItem.getDeviceId());
                            logOutput("HTTP Post file: " + ovfFileItem.getPath());
                            logOutput("ovf file path: " + ovfFilePath );
			    logOutput("Path separator: " + File.separator);
                            String absoluteFile = "";
			    if (ovfFilePath.indexOf(File.separator) != -1) {
                                absoluteFile = ovfFilePath.substring(0, ovfFilePath.lastIndexOf(File.separator));
			    }
                            absoluteFile = absoluteFile + File.separator + ovfFileItem.getPath();
                            logOutput("Absolute path: " + absoluteFile);
                            getVMDKFile(ovfFileItem.isCreate(), absoluteFile,
                                    deviceUrl.getUrl().replace("*", hostName),
                                    ovfFileItem.getSize(), httpNfcLease);
                            logOutput("Completed uploading the VMDK file");
                        }
                    }
                }
                if (!logEnabled) {
                    System.out.print("\n");
		}

                vimPort.httpNfcLeaseProgress(httpNfcLease, 100);
                vimPort.httpNfcLeaseComplete(httpNfcLease);
		System.out.println("Vm [ " + newVmName + " ] successfully imported !");
             } else {
                System.err.println("Lease cannot be moved to ready state.. Possibly an error");
	     }
	   }
         }
      } catch (Exception e) {
           e.printStackTrace();
      }
   }

  /**
   * Method to retrieve ResourcePool for a given host MOR
   * @param hostRef MOR of the host system
   * @return MOR of the ResourcePool to which the requested host system belongs to
   */
   public ManagedObjectReference getResourcePool(ManagedObjectReference hostRef) {
      methodName = "getResourcePool";
      ManagedObjectReference rpRef = null;
      try {
          Map <String, Object> map = getProperties(hostRef, new String[] {"parent"});
          ManagedObjectReference parent = (ManagedObjectReference)map.get("parent");
	  if (parent == null) {
              System.err.println("Fatal: host parent cannot be null!");
              return null;
	  }
	  map = getProperties(parent, new String[] {"resourcePool"});
	  return (ManagedObjectReference)map.get("resourcePool");
      } catch (Exception e) {
          e.printStackTrace();
      }
      return null;
   }

  /**
   * Method to retrieve MOR of a given managed entity type. NOTE: This method may be removed later
   * @param entityType
   * @return MOR of the requested managed entity
   */
   public ManagedObjectReference getManagedEntity(String entityType, ManagedObjectReference dsRef) {
      methodName = "getManagedEntity";
      ManagedObjectReference mRef = null;
      try {
          ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
          ManagedObjectReference propCol = serviceContent.getPropertyCollector();
          List <String> entityList = new ArrayList<String>();
          entityList.add(entityType);
          ManagedObjectReference viewRef = vimPort.createContainerView(viewMgrRef, serviceContent.getRootFolder(), entityList, true);
          ObjectSpec oSpec = new ObjectSpec();
          oSpec.setObj(viewRef);
          oSpec.setSkip(true);

          TraversalSpec tSpec = new TraversalSpec();
          tSpec.setName("traverseEntities");
          tSpec.setPath("view");
          tSpec.setSkip(false);
          tSpec.setType("ContainerView");

          oSpec.getSelectSet().add(tSpec);

          PropertySpec pSpec = new PropertySpec();
          pSpec.setType(entityType);
          pSpec.getPathSet().add("name");
  
          PropertyFilterSpec fSpec = new PropertyFilterSpec();
          fSpec.getObjectSet().add(oSpec);
          fSpec.getPropSet().add(pSpec);

          RetrieveOptions ro = new RetrieveOptions();
          List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
          fSpecList.add(fSpec);

          RetrieveResult props = vimPort.retrievePropertiesEx(propCol, fSpecList, ro);
          logOutput(dsRef + " props: " + props);

          if (props != null) {
             for (ObjectContent oc : props.getObjects()) {
                  mRef = oc.getObj();
                  logOutput("rpool: "  + mRef);
		  getProperties(mRef, null);
                  List <DynamicProperty> dps = oc.getPropSet();
                  if (dps != null) {
                      for (DynamicProperty dp : dps) {
                         logOutput("key : " + dp.getName() + " = " + dp.getVal());
                      }
                  }
                 //break;
             }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return mRef;
   }

   public ManagedObjectReference [] getManagedEntities(String entityType) {
      methodName = "getManagedEntity";
      List<ManagedObjectReference> mRefs = new Vector<ManagedObjectReference>();
      try {
          ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
          ManagedObjectReference propCol = serviceContent.getPropertyCollector();
          List <String> entityList = new ArrayList<String>();
          entityList.add(entityType);
          ManagedObjectReference viewRef = vimPort.createContainerView(viewMgrRef, serviceContent.getRootFolder(), entityList, true);
          ObjectSpec oSpec = new ObjectSpec();
          oSpec.setObj(viewRef);
          oSpec.setSkip(true);

          TraversalSpec tSpec = new TraversalSpec();
          tSpec.setName("traverseEntities");
          tSpec.setPath("view");
          tSpec.setSkip(false);
          tSpec.setType("ContainerView");

          oSpec.getSelectSet().add(tSpec);

          PropertySpec pSpec = new PropertySpec();
          pSpec.setType(entityType);
          pSpec.getPathSet().add("name");
  
          PropertyFilterSpec fSpec = new PropertyFilterSpec();
          fSpec.getObjectSet().add(oSpec);
          fSpec.getPropSet().add(pSpec);

          RetrieveOptions ro = new RetrieveOptions();
          List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
          fSpecList.add(fSpec);

          RetrieveResult props = vimPort.retrievePropertiesEx(propCol, fSpecList, ro);
          if (props != null) {
             for (ObjectContent oc : props.getObjects()) {
                  mRefs.add(oc.getObj());
             }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return getArray(mRefs);
   }

   public ManagedObjectReference[] getArray(List<ManagedObjectReference> list) {
      ManagedObjectReference [] refs = new ManagedObjectReference[list.size()];
      int i=0;
      for (ManagedObjectReference ref : list) {
           refs[i] = list.get(i); 
	   i++;
      }
      return refs;
   }

   public ManagedObjectReference [] getAllManagedEntities() {
      List<ManagedObjectReference> list = new Vector<ManagedObjectReference>();
      for (String entity : MANAGEDENTITIES) {
           ManagedObjectReference [] refs = getManagedEntities(entity);
	   if (refs != null && refs.length > 0) {
               for (ManagedObjectReference ref : refs) {
                    list.add(ref);
	       }
	   }
      }
      return (ManagedObjectReference[])list.toArray();
   }

  /**
   * Method to wait on httpNfcLease till it reaches ready state or times out
   * @param httpNfcLease
   * @return 0 if success, -1 if failure
   */
   private int waitOnLease(ManagedObjectReference httpNfcLease) {
      methodName = "waitOnLease";
      try {
            Map <String, Object> nmap = null;
	    int progress = 0;
	    int timeout = 0;
            while (progress != 100) {
                   nmap = getProperties(httpNfcLease, new String [] {"state", "initializeProgress"});
                   progress = (Integer)nmap.get("initializeProgress");
		   if (progress == 100) {
                       return 0; //lease is in ready state
		   }
                   Thread.sleep(1000);
		   timeout++;
		   if (timeout > 15) {
                       return -1; //lease couldn't move to ready state
		   }
	    }

      } catch (Exception e) {
            e.printStackTrace();
      }
      return -1; //lease couldn't move to ready state
   }


  /**
   * Method to get Ovf descriptor
   * @param ovfDescFile path to the ovf file
   * @return descriptor of the ovf file
   */
   public String getOvfDesc(String ovfDescFile) {
      methodName = "getOvfDesc";
      StringBuffer buf = new StringBuffer("");
      InputStream istream = null;
      int x;
      try {
           istream = new FileInputStream(ovfDescFile);
           if (istream != null) {
               while ((x = istream.read()) != -1) {
                      buf.append((char)x);
               }
           }
           return buf.toString();
      } catch (Exception e) {
           e.printStackTrace();
      }
      finally {
           try {
                if (istream != null) {
                    istream.close();
		}
           } catch (Exception ex) {

           }
      }
      return null;
   }

    private long writeVMDKFile(String absoluteFile, String urlString, String localpath, ManagedObjectReference httpNfcLease) throws IOException {
    methodName = "writeVMDKFile";
    long written = 0;
    try {
        URL urlCon = new URL(urlString);
        HttpsURLConnection conn = (HttpsURLConnection) urlCon.openConnection();

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setAllowUserInteraction(true);

        // Maintain session
        //Map headers = (Map) ((BindingProvider) vimPort).getResponseContext().get( MessageContext.HTTP_RESPONSE_HEADERS);
        List<String> cookies = (List<String>) headers.get("Set-cookie");
        String cookieValue = cookies.get(0);
        StringTokenizer tokenizer = new StringTokenizer(cookieValue, ";");
        cookieValue = tokenizer.nextToken();
        String path = "$" + tokenizer.nextToken();
        String cookie = "$Version=\"1\"; " + cookieValue + "; " + path;

        // set the cookie in the new request header
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("Cookie", Collections.singletonList(cookie));
        ((BindingProvider) vimPort).getRequestContext().put(
                MessageContext.HTTP_REQUEST_HEADERS, map);

        conn.setRequestProperty("Cookie", cookie);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("Expect", "100-continue");
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Length", "1024");

        InputStream in = conn.getInputStream();
        String _localpath = localpath + "/" + absoluteFile;
        OutputStream out = new FileOutputStream(new File(_localpath));
        byte[] buf = new byte[102400];
        int len = 0;
        WaitThread waitThread = null;
        if (!logEnabled) {
            waitThread = new WaitThread();
            waitThread.start();
        }
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
            written = written + len;
            //percentCompleted = ((float)bytesWrote/ totalbytes) * 100;
            try {
                 if (httpNfcLease != null) {
                     vimPort.httpNfcLeaseProgress(httpNfcLease, 50); //just to keep the http lease alive, keep sending progress information to vimport. It may timeout otherwise //kannan
		 }
            } catch (Exception ex) {

            }
        }
        System.out.println("   Exported File " + absoluteFile + " : " + written);
        in.close();
        out.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
      return written;
   }

    private void getVMDKFile(boolean put, String fileName, String uri, long diskCapacity, ManagedObjectReference httpNfcLease) {
	methodName = "getVMDFile";
        HttpsURLConnection conn = null;
        BufferedOutputStream bos = null;

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 64 * 1024;

        WaitThread waitThread = null;
        try {
            logOutput("Destination host URL: " + uri);
            HostnameVerifier hv = new HostnameVerifier() {
                @Override
                public boolean verify(String urlHostName, SSLSession session) {
                    logOutput("Warning: URL Host: " + urlHostName + " vs. "
                            + session.getPeerHost());
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
            URL url = new URL(uri);
            conn = (HttpsURLConnection) url.openConnection();


            // Maintain session
            @SuppressWarnings("unchecked")
            //Map headers = (Map) ((BindingProvider) vimPort).getResponseContext().get( MessageContext.HTTP_RESPONSE_HEADERS);
            List<String> cookies = (List<String>) headers.get("Set-cookie");
            String cookieValue = cookies.get(0);
            StringTokenizer tokenizer = new StringTokenizer(cookieValue, ";");
            cookieValue = tokenizer.nextToken();
            String path = "$" + tokenizer.nextToken();
            String cookie = "$Version=\"1\"; " + cookieValue + "; " + path;

            // set the cookie in the new request header
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            map.put("Cookie", Collections.singletonList(cookie));
            ((BindingProvider) vimPort).getRequestContext().put(
                    MessageContext.HTTP_REQUEST_HEADERS, map);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setChunkedStreamingMode(maxBufferSize);
            if (put) {
                conn.setRequestMethod("PUT");
                logOutput("HTTP method: PUT");
            } else {
                conn.setRequestMethod("POST");
                logOutput("HTTP method: POST");
            }
            conn.setRequestProperty("Cookie", cookie);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type",
                    "application/x-vnd.vmware-streamVmdk");
            conn.setRequestProperty("Content-Length", String.valueOf(diskCapacity));
            conn.setRequestProperty("Expect", "100-continue");
            bos = new BufferedOutputStream(conn.getOutputStream());
            logOutput("Local file path: " + fileName);
            InputStream io = new FileInputStream(fileName);
            BufferedInputStream bis = new BufferedInputStream(io);
            bytesAvailable = bis.available();
            logOutput("vmdk available bytes: " + bytesAvailable);
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            bytesRead = bis.read(buffer, 0, bufferSize);
            long bytesWrote = bytesRead;
            TOTAL_BYTES_WRITTEN += bytesRead;
	    if (!logEnabled) {
                waitThread = new WaitThread();
                waitThread.start();
	    }
	    File file = new File(fileName);
	    long totalbytes = file.length();  //kannan
	    float percentCompleted = 0; //kannan
            while (bytesRead >= 0) {
                bos.write(buffer, 0, bufferSize);
                bos.flush();
                logOutput("Bytes Wrote: " + bytesWrote);
                bytesAvailable = bis.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesWrote += bufferSize;
                TOTAL_BYTES_WRITTEN += bufferSize;
                buffer = new byte[bufferSize];
                bytesRead = bis.read(buffer, 0, bufferSize);
                logOutput("Bytes Available: " + bytesAvailable);
                if ((bytesRead == 0) && (bytesWrote >= diskCapacity)) {
                    logOutput("Total bytes written: " + TOTAL_BYTES_WRITTEN);
                    bytesRead = -1;
                }
	        percentCompleted = ((float)bytesWrote/ totalbytes) * 100;
		try {
                     if (httpNfcLease != null) {
                         vimPort.httpNfcLeaseProgress(httpNfcLease, (int)percentCompleted); //just to keep the http lease alive, keep sending progress information to vimport. It may timeout otherwise //kannan
		     }
		} catch (Exception ex) {

		}
            }
            try {
                DataInputStream dis = new DataInputStream(conn.getInputStream());
                dis.close();
            } catch (SocketTimeoutException stex) {
                System.err.println("From (ServerResponse): " + stex);
            } catch (IOException ioex) {
                System.err.println("From (ServerResponse): " + ioex);
            }
            logOutput("Writing vmdk to the output stream done");
            bis.close();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
	        if (waitThread != null) {
                    waitThread.interrupt();
	        }
		if (bos != null) {
                    bos.flush();
                    bos.close();
		}
		if (conn != null) {
                    conn.disconnect();
		}
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

   /**
    * Method to export a VM to a ovf file. 
    * @param vmName name of the virtual machine to be exported to
    * @param localpath path to the directory where the ovf needs to be created
    */
    public void exportVM(String vmName, String localpath) {
	methodName = "exportVM"; 
	ManagedObjectReference vmRef = getMORFromEntityName(VIRTUALMACHINE, vmName);
	if (vmRef == null) {
            System.err.println("Error: Cannot find MOR for VM: " + vmName);
            System.err.println("Error: Cannot export VM !");
            return;
	}
	String hostName = getParentName(vmRef);
	try {
                OvfCreateDescriptorParams ovfCreateDescriptorParams =
                        new OvfCreateDescriptorParams();
                ManagedObjectReference httpNfcLease = vimPort.exportVm(vmRef);
                if (httpNfcLease == null) {
                    System.err.println("Fatal: httpNfcLease is null. Exiting...");
                    return;
	        }

		int result = waitOnLease(httpNfcLease);
                if (result == 0) { //lease is in ready state //kannan
                    HttpNfcLeaseInfo httpNfcLeaseInfo = null;
                    Map<String, Object> leaseMap = getProperties(httpNfcLease, new String [] {"info"});
                    if (leaseMap != null && !leaseMap.isEmpty()) {
                        httpNfcLeaseInfo = (HttpNfcLeaseInfo)leaseMap.get("info");
                    }
		    if (httpNfcLeaseInfo == null) {
                        System.err.println("exportVM - Fatal: httpNfcLeaseInfo is null. Check if the VM [ " + vmName + " ] is powered off.   Exiting...");
                        return;
		    }

                    httpNfcLeaseInfo.setLeaseTimeout(300000000);
                    long diskCapacity =
                            (httpNfcLeaseInfo.getTotalDiskCapacityInKB()) * 1024;
                    System.out.println("************ " + diskCapacity);

                    long TOTAL_BYTES = diskCapacity;

                    List<HttpNfcLeaseDeviceUrl> deviceUrlArr =
                            httpNfcLeaseInfo.getDeviceUrl();
                    if (deviceUrlArr != null) {
                        List<OvfFile> ovfFiles = new ArrayList<OvfFile>();
                        for (int i = 0; i < deviceUrlArr.size(); i++) {
                            System.out.println("Downloading Files:");
                            String deviceId = deviceUrlArr.get(i).getKey();
                            String deviceUrlStr = deviceUrlArr.get(i).getUrl();
                            String absoluteFile =
                                    deviceUrlStr.substring(deviceUrlStr
                                            .lastIndexOf("/") + 1);
                            System.out.println("   Absolute File Name: " + absoluteFile);
                            System.out.println("   VMDK URL: " + deviceUrlStr.replace("*", hostName));
                            long writtenSize = writeVMDKFile(absoluteFile, deviceUrlStr.replace("*", hostName), localpath, httpNfcLease);
                            OvfFile ovfFile = new OvfFile();
                            ovfFile.setPath(absoluteFile);
                            ovfFile.setDeviceId(deviceId);
                            ovfFile.setSize(writtenSize);
                            ovfFiles.add(ovfFile);
                        }
                        ovfCreateDescriptorParams.getOvfFiles().addAll(ovfFiles);
                        OvfCreateDescriptorResult ovfCreateDescriptorResult =
                                vimPort.createDescriptor( serviceContent.getOvfManager(), vmRef, ovfCreateDescriptorParams);
                        System.out.println();
                        String outOVF = localpath + "/" + vmName + ".ovf";
                        File outFile = new File(outOVF);
                        FileWriter out = new FileWriter(outFile);
                        out.write(ovfCreateDescriptorResult.getOvfDescriptor());
                        out.close();
                        System.out.println("OVF Desriptor Written to file " + vmName + ".ovf");
                        System.out.println("DONE");
                        if (!ovfCreateDescriptorResult.getError().isEmpty()) {
                            System.out.println("SOME ERRORS");
                        }
                        if (!ovfCreateDescriptorResult.getWarning().isEmpty()) {
                            System.out.println("SOME WARNINGS");
                        }
                    } else {
                        System.out.println("No Device URLS");
                    }
                    System.out.println("Completed Downloading the files");
                    vimPort.httpNfcLeaseProgress(httpNfcLease, 100);
                    vimPort.httpNfcLeaseComplete(httpNfcLease);
		    System.out.println("Vm [ " + vmName + " ] successfully exported!");
             } else {
                System.err.println("Lease cannot be moved to ready state.. Possibly an error");
	     }
	} catch (Exception e) {
             e.printStackTrace();
	}
    }

   /**
    * Method to install vmware tools on a specific vm
    * @param vmName name of the virtual machine where vmware tools need to be installed
    */
    public void installVMWareTools(String vmName) {
        methodName = "installVMWareTools0";
	ManagedObjectReference vmRef = getMORFromEntityName(VIRTUALMACHINE, vmName);
	if (vmRef == null) {
            System.err.println("Error: Cannot find MOR for VM: " + vmName);
            return;
	}
	installVMWareTools(vmRef);
    }

   /**
    * Method to install vmware tools on a specific vm(MOR)
    * @param vmRef MOR of the virtual machine here vmware tools need to be installed
    */
    public void installVMWareTools(ManagedObjectReference vmRef) {
        methodName = "installVMWareTools1";
	System.out.println("Attempting to install VMWare Tools ...\n");
        try {
             vimPort.mountToolsInstaller(vmRef);

	} catch (Exception e) {
            e.printStackTrace();
	    return;
	}
	System.out.println("VMWare Tools successfully installed...");
    }

  /**
   * Method to create a new Virtual Machine on a specific host. This method is under construction
   * @param hostName Esxi host where the virtual machine needs to be created
   */
    public void createVM(String hostName) {
       try {
            ManagedObjectReference hostRef = getMORFromEntityName(HOSTSYSTEM, hostName);
            if (hostRef == null) {
                System.err.println("Cannot retrieve MOR for host [ " + hostName + " ] ...");
                return;
	    }
	    ManagedObjectReference rpRef = getResourcePool(hostRef);
            if (rpRef == null) {
                System.err.println("Cannot retrieve ResourcePool for host [ " + hostName + " ] ...");
                return;
	    }
	    VirtualMachineConfigSpec vmSpec = new VirtualMachineConfigSpec();
	    vimPort.createVMTask(null, vmSpec, rpRef, hostRef);
       } catch(Exception e) {
          e.printStackTrace();
       }
    }

   /**
    * Method to delete a VM
    * @param vmName name of the virtual machine that needs to be deleted from the inventory
    */
    public void deleteVM(String vmName) {
	methodName = "deleteVM"; 
	ManagedObjectReference mor = getMORFromEntityName(VIRTUALMACHINE, vmName);
	if (mor == null) {
            System.err.println("Cannot find MOR for VM: " + vmName);
            return;
	}
	try {
             vimPort.destroyTask(mor);
	     System.err.println("deleteVM - Successfully deleted vm: " + vmName);
	} catch (Exception e) {
             e.printStackTrace();
	}
    }

   /**
    * Method to log any comments to std out prefixed with the caller method's name
    */
    protected void logOutput(String str) {
        if (logEnabled) {
            System.out.println(methodName + " - " + str);
	}
    }
}

class WaitThread extends Thread {

    private int delay = 2000;  //2 seconds
    private String waitChar = ". ";

    public WaitThread() {
       setDaemon(true);
    }

    public void setDelay(int d) {
      delay = d;
    }

    public void setWaitChar(String s) {
      waitChar = s;
    }

    public void run() {
       try {
            while (! isInterrupted()) {
               Thread.sleep(delay);
	       System.out.print(waitChar);
	    }
       } catch (Exception e) {

       }
    }
}
