package com.vmware.viclient.managedentities;

import com.vmware.viclient.helper.VimUtil;

import com.vmware.viclient.connectionmgr.ConnectionManager;
import com.vmware.vim25.HostFileSystemMountInfo;
import com.vmware.vim25.HostFileSystemVolume;
import com.vmware.vim25.HostFileSystemVolumeInfo;
import com.vmware.vim25.HostHostBusAdapter;
import com.vmware.vim25.HostMountInfo;
import com.vmware.vim25.HostMultipathInfo;
import com.vmware.vim25.HostMultipathInfoLogicalUnit;
import com.vmware.vim25.HostMultipathInfoLogicalUnitPolicy;
import com.vmware.vim25.HostMultipathInfoPath;
import com.vmware.vim25.HostScsiTopology;
import com.vmware.vim25.HostScsiTopologyInterface;
import com.vmware.vim25.HostScsiTopologyLun;
import com.vmware.vim25.HostScsiTopologyTarget;
import com.vmware.vim25.HostStorageDeviceInfo;
import com.vmware.vim25.ScsiLun;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.ManagedObjectReference;

import java.util.List;

public class Hoststoragesystem
{
		private ServiceContent inst = null;
		private String Esxhost = null;
		private VimUtil vimUtil = null;

		public Hoststoragesystem() {
			ConnectionManager cmgr = ConnectionManager.getInstance();
			Esxhost = cmgr.getEsxHost();
			inst=cmgr.getServiceContent();
			vimUtil = cmgr.getVimUtil();
		}

		public ManagedObjectReference getStorageSystem(String serverName)  {
			try {
				  ManagedObjectReference rootFolder = inst.getRootFolder();
				  ManagedObjectReference host = vimUtil.getMORFromEntityName(VimUtil.HOSTSYSTEM, serverName);
				    if(host==null)
				    {
				      System.out.println("Host not found");
				      
				      return null;
				    }
				    ManagedObjectReference hds = (ManagedObjectReference)vimUtil.getProperty(host, "configManager.storageSystem");
				  
				  return hds;
	    } catch (Exception e) {
	              e.printStackTrace();
	    }
	    return null;
	}
	
		
	public void printStorageDeviceInfo(){
		
		ManagedObjectReference hds = getStorageSystem(Esxhost);
		HostStorageDeviceInfo hsdi = (HostStorageDeviceInfo)vimUtil.getProperty(hds, "storageDeviceInfo");
		System.out.println("\nHost bus adapters");
	    printHBAs(hsdi.getHostBusAdapter());
	    System.out.println("\nMultipath information");
	    HostMultipathInfo hmi = hsdi.getMultipathInfo();
	    printMultiPathInfo(hmi);
	    System.out.println("\nSCSI LUNs");
	    printScsiLuns(hsdi.getScsiLun());
	    HostScsiTopology hst = hsdi.getScsiTopology();
	    printScsiTopology(hst);
	    System.out.println("\nSoftware iSCSI enabled:"+ hsdi.isSoftwareInternetScsiEnabled());
		
		
	}
 
  public void printHBAs(List<HostHostBusAdapter> hbas)
  {
    for(HostHostBusAdapter hba : hbas)
    {
      System.out.println("Device:" + hba.getDevice());
      System.out.println("Bus:" + hba.getBus());
      System.out.println("Driver:" + hba.getDriver());
      System.out.println("Key:" + hba.getKey());
      System.out.println("Model:" + hba.getModel());
      System.out.println("PCI:" + hba.getPci());
      System.out.println("Status:" + hba.getStatus());
    }
  }
  public void printScsiTopology(HostScsiTopology hst)
  {
    List<HostScsiTopologyInterface> hstis = hst.getAdapter();
    for(HostScsiTopologyInterface hsti : hstis)
    {
      System.out.println("Adapter:" + hsti.getAdapter());
      System.out.println("Key:" + hsti.getKey());
      List<HostScsiTopologyTarget> hstts = hsti.getTarget();
      for(HostScsiTopologyTarget hstt : hstts)
      {
        System.out.println("Key:" + hstt.getKey());
        System.out.println("Target:" + hstt.getTarget());
        System.out.println("Transport:"
            + hstt.getTransport().getClass().getName());
        List<HostScsiTopologyLun> luns = hstt.getLun();
        for(HostScsiTopologyLun lun : luns)
        {
          System.out.println("Key:" + lun.getKey());
          System.out.println("LUN:" + lun.getLun());
          System.out.println("ScsiLun:" + lun.getScsiLun());
        }
      }
    }
  }
  public void printScsiLuns(List<ScsiLun> sls)
   {
    for(ScsiLun sl : sls) 
    {
      System.out.println("UUID:" + sl.getUuid());
      System.out.println("CanonicalName:"
          + sl.getCanonicalName());
      System.out.println("LunType:" + sl.getLunType());
      System.out.print("OperationalState:");
      List<String> states = sl.getOperationalState();
      for(String state : states)
      {
        System.out.print(state + " ");
      }
      System.out.println("\nSCSI Level:"
          + sl.getScsiLevel());
      System.out.println("Vendor:" + sl.getVendor());
    }
  }
  public void printMultiPathInfo(HostMultipathInfo hmi)
  {
    List<HostMultipathInfoLogicalUnit> lus = hmi.getLun();
    for(HostMultipathInfoLogicalUnit lu : lus)
    {
      System.out.println("ID:" + lu.getId());
      System.out.println("Key:" + lu.getKey());
      System.out.println("LUN:" + lu.getLun());
      List<HostMultipathInfoPath> hmips = lu.getPath();
      for(HostMultipathInfoPath hmip : hmips)
      {
        System.out.println("Adpator:" + hmip.getAdapter());
        System.out.println("Key:" + hmip.getLun());
        System.out.println("Name:" + hmip.getName());
        System.out.println("PathState:"
            + hmip.getPathState());
        System.out.println("Transport:"
            + hmip.getTransport().getClass().getName());
      }
      HostMultipathInfoLogicalUnitPolicy policy =
          lu.getPolicy();
      System.out.println("Policy:" + policy.getPolicy());
    }
  }
  public void printFileVolumeInfo()
  {
	  ManagedObjectReference hds = getStorageSystem(Esxhost);
	  HostFileSystemVolumeInfo info = (HostFileSystemVolumeInfo)vimUtil.getProperty(hds, "fileSystemVolumeInfo");
	  
    List<String> volTypes = info.getVolumeTypeList();
    for(String vol : volTypes)
    {
      System.out.println(vol);
    }
    System.out.println("\nThe file system volumes mounted:");
    List<HostFileSystemMountInfo> mis = info.getMountInfo();
    for(HostFileSystemMountInfo mi : mis)
    {
      HostMountInfo hmi = mi.getMountInfo();
      System.out.println("\nAccessible:" + hmi.isAccessible());
      System.out.println("AccessMode:" + hmi.getAccessMode());
      System.out.println("Path:" + hmi.getPath());
      HostFileSystemVolume hfsv = mi.getVolume();
      System.out.println("Capacity:" + hfsv.getCapacity());
      System.out.println("Name:" + hfsv.getName());
       System.out.println("Type:" + hfsv.getType());
    }
  }
}
