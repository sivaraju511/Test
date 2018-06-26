import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;

import com.vistara.api.dto.device.LoggedInUser;
import com.vistara.base.util.CalendarUtils;
import com.vistara.core.dao.PartialPage;
import com.vistara.dra.model.AgentVersion;
import com.vistara.indexer.service.ResourceIndexManagementService;
import com.vistara.indexer.service.impl.ResourceIndexManagementServiceImpl;
import com.vistara.scc.model.agentpolicies.AgentPolicy;
import com.vistara.scc.model.cloud.Provider;
import com.vistara.scc.model.device.Device;
import com.vistara.scc.model.device.cloud.Nutanix;
import com.vistara.scc.model.sg.ManagementGatewayProfile;
import com.vistara.scc.model.um.Client;
import com.vistara.scc.model.um.Location;
import com.vistara.service.auth.AuthenticationContext;
import com.vistara.service.memcache.NCareUtil;
import com.vistara.service.util.Util;
import com.vistara.timeseries.collection.SourceType;
import com.vistara.util.ServiceFactory;
import com.vistara.util.StringUtil;
import com.vistara.web.ParamHelper;
import com.vistara.web.PortalUtil;
import com.vistara.web.util.RequestUtil;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class Test {
	public void Test(){
		
	}
	
	public void List()
	{

		long clientId = PortalUtil.getClientIdParam(request);
		long mspId = PortalUtil.getMSPIdParam(request);
		if (AuthenticationContext.isClientUser()) {
			clientId = AuthenticationContext.getCurrentUserOrgId();
			PortalUtil.setClientIDparam(request, clientId);
		}
		String searchStr = ServletRequestUtils.getStringParameter(request, "searchStr", null);
		long deviceTypeId = ServletRequestUtils.getLongParameter(request, "deviceTypeId", 0);
		long deviceTypecategoryId = ServletRequestUtils.getLongParameter(request, "deviceType_CategoryTypeId", 0);
		long deviceGroupId = ServletRequestUtils.getLongParameter(request, "deviceGroupId", 0);
		long deviceLocationId = ServletRequestUtils.getLongParameter(request, "deviceLocationId", 0);
		long providerId = ServletRequestUtils.getLongParameter(request, "cloudProviderId", 0);
		String sType = ServletRequestUtils.getStringParameter(request, "sType","");
		boolean discovered = ServletRequestUtils.getBooleanParameter(request, "discovered", false);
		boolean isUnAssignedDG = ServletRequestUtils.getBooleanParameter(request, "isUnAssignedDG", false);
		boolean isUnAssignedDL = ServletRequestUtils.getBooleanParameter(request, "isUnAssignedDL", false);
		boolean gatewayDevices = ServletRequestUtils.getBooleanParameter(request, "gatewayDevices", false);
		boolean isDeleted = ServletRequestUtils.getBooleanParameter(request, "deleted", false);
		boolean showVoIPDevices = ServletRequestUtils.getBooleanParameter(request, "showVoIPDevices", false);
		boolean showWirelessLANDevices = ServletRequestUtils.getBooleanParameter(request, "showWirelessLANDevices", false);
		boolean showWLANControllers = ServletRequestUtils.getBooleanParameter(request, "showWLANControllers", false);
		boolean showThickApDevices = ServletRequestUtils.getBooleanParameter(request, "showThickApDevices", false);
		boolean showNutanixDevices = ServletRequestUtils.getBooleanParameter(request, "showNutanixDevices", false);
		boolean locationTab = ServletRequestUtils.getBooleanParameter(request, "locationTab", false);
		String type = ServletRequestUtils.getStringParameter(request, "type", null);
		int pageSize = ServletRequestUtils.getIntParameter(request, "limit", 40);
		int pageNo = 0;
		pageSize = (pageSize > 0 ? pageSize:40);
		boolean activeStatus = true;
		if (!StringUtil.isEmpty(request.getParameter("activeStatus"))) {
			activeStatus = ParamHelper.getBoolParam(request, "activeStatus");
		}
		String sortName = ServletRequestUtils.getStringParameter(request, "sort", "name");
		String sortDirection = ServletRequestUtils.getStringParameter(request, "dir", "ASC");
		boolean isDecendingOrder = (!StringUtil.isEmpty(sortDirection) && sortDirection.equalsIgnoreCase("DESC")) ? true : false;
		Map<String, Object> filterInfo = new HashMap<String, Object>();
		Map<String, Object> searchParamMap = new HashMap<String,Object>();
		if(request.getSession().getAttribute("beta") != null) {
			int offset = ServletRequestUtils.getIntParameter(request, "offset", 0);
			pageNo = (offset/pageSize)+1;
			String str=ServletRequestUtils.getStringParameter(request, "filter");
			if(str != null && !str.isEmpty()) {
				JSONObject json = (JSONObject)JSONSerializer.toJSON(str);
				Iterator<?> keys = json.keys();
				while (keys.hasNext()) {
					String key = (String) keys.next();
					searchParamMap.put(key, json.get(key));
					filterInfo.put(key, json.get(key));
				}
			}
			// For handling Search with speacial characters '_' and '%' in DB
			if(searchParamMap != null && searchParamMap.size() > 0) {
				for(String key : searchParamMap.keySet()){
					if(searchParamMap.get(key) instanceof String) {
						String searchString = (String) searchParamMap.get(key);
						searchParamMap.put(key, StringUtil.handleDBSpecialChars(searchString));
					}
				}
			}
			if(searchParamMap.get("technologies") != null){
				String tech = (String)searchParamMap.get("technologies");
				List<Long> saIds = StringUtil.convertStringToList(tech, ",");
				if((saIds != null) && (saIds.size() > 0)){
					if(saIds.contains(Long.valueOf(-1))){
						searchParamMap.put("agentInstalled", true);
						saIds.remove(Long.valueOf(-1));
					}
					if(saIds.contains(Long.valueOf(-2))){
						searchParamMap.put("classCode", "cloudinstance");
						saIds.remove(Long.valueOf(-2));
					}
					if((saIds != null) && (saIds.size() > 0)){
						searchParamMap.put("saId", saIds);
					}else{
						searchParamMap.remove("technologies");
					}
				}
			}
			if(searchParamMap.get("modifiedTime") != null){
				String date = (String)searchParamMap.get("modifiedTime");
				searchParamMap.put("modifiedTime", CalendarUtils.convertDateToGMT(CalendarUtils.StringToDate(date, "dd-MMM-yyyy"), AuthenticationContext.getCurrentUserTimeZone()));
			}
		} else {
			pageNo = ServletRequestUtils.getIntParameter(request, "page", 1);
			pageNo = (pageNo > 0 ? pageNo:1);
			searchParamMap = RequestUtil.getRequestParameterMap(request, "ss.");
			// For handling Search with speacial characters '_' and '%' in DB
			if(searchParamMap != null && searchParamMap.size() > 0) {
				for(String key : searchParamMap.keySet()){
					if(searchParamMap.get(key) instanceof String) {
						String searchString = (String) searchParamMap.get(key);
						searchParamMap.put(key, StringUtil.handleDBSpecialChars(searchString));
					}
				}
			}
			if(searchParamMap.get("saId") != null){
				List<Long> saIds = (ArrayList<Long>)searchParamMap.get("saId");
				if((saIds != null) && (saIds.size() > 0)){
					if(saIds.contains(Long.valueOf(-1))){
						searchParamMap.put("agentInstalled", true);
						saIds.remove(Long.valueOf(-1));
					}
					if(saIds.contains(Long.valueOf(-2))){
						searchParamMap.put("classCode", "cloudinstance");
						saIds.remove(Long.valueOf(-2));
					}
					if((saIds != null) && (saIds.size() > 0)){
						searchParamMap.put("saId", saIds);
					}else{
						searchParamMap.remove("saId");
					}
				}
			}
		}
		Client client = null;
		Map<Long, Integer> deviceStates = new HashMap<Long, Integer>();
		Map<Long, Date> devicePurchaseDate = new HashMap<Long, Date>();
		Map<Long, Date> deviceWarrantyDate = new HashMap<Long, Date>();
		Map<Long, Date> lastPatchInstallDate = new HashMap<Long, Date>();
		Map<Long, Object> maintenanceDeviceWindows = new HashMap<Long, Object>();
		PartialPage<Object[]> deviceObjectPage = null;
		Map<Long, AgentPolicy> agentPolicyMap = new HashMap<Long, AgentPolicy>();
		Map<Long, ManagementGatewayProfile> sgMap = null;
		//Map<Long, Integer> collectorCountMap = new HashMap<Long, Integer>();
		Map<Long,String> assignedGatewayNameMap = new HashMap<Long,String>();
		Map<Long,String> deviceAssignedGatewayNameMap = new HashMap<Long,String>();
		List<Device> deviceList = new ArrayList<Device>();

		Map<Long, AgentVersion> agentVersionDetails = new HashMap<Long, AgentVersion>();
		AgentVersion deviceAgentVersion = null;
		List<Long> deviceIds = new ArrayList<Long>();
		int totalDevices=0;
		LoggedInUser userInfo = null;
		List<ManagementGatewayProfile> confList = null;
		long deviceId;
		Device device;
		if(gatewayDevices) {
			List<ManagementGatewayProfile> gatewayProfiles = sgService.getRegisteredManagementGatewayProfileList(clientId);
			for(ManagementGatewayProfile sg : gatewayProfiles) {
				if(sg.getProfileType().equalsIgnoreCase("Gateway") && sg.getManagementGateway() != null && sg.getManagementGateway().getDeviceId() > 0) {
					deviceIds.add(sg.getManagementGateway().getDeviceId());
				}
			}
		} else if (showVoIPDevices) {
			List<Device> voipDevices = null;
			if(null != type && !type.isEmpty()) {
				voipDevices = inventoryService.getNetworkVoipDevicesListByDeviceTypeName(clientId, "Network Device >> "+type, discovered);
			}else {
				voipDevices = inventoryService.getAllVoipDevicesByDeviceTypeName(clientId, discovered);
			}
			for(Device voipDevice : voipDevices) {
				deviceIds.add(voipDevice.getId());
			}
		}else if (showWirelessLANDevices) {
			List<Device> wlanDevices = inventoryService.getAllWirelessLanDevicesByDeviceTypeName(clientId, discovered);
			for(Device wlanDevice : wlanDevices) {
				deviceIds.add(wlanDevice.getId());
			}
		}else if (showWLANControllers) {
			List<Device> wlancontrollers = inventoryService.getAllWLANControllerDevicesByDeviceTypeName(clientId, discovered);
			for(Device wlancontroller : wlancontrollers) {
				deviceIds.add(wlancontroller.getId());
			}
		} else if (showThickApDevices) {
			List<Device> thickAPDevices = inventoryService.getAllThickAccessPointDevicesByDeviceTypeName(clientId, discovered);
			for(Device thickAPDevice : thickAPDevices) {
				deviceIds.add(thickAPDevice.getId());
			}
		} else if (showNutanixDevices) {
			List<Nutanix> nutanixDevices = inventoryService.getNutanixArrayListByProvider(clientId, Provider.NUTANIX);
			for(Device ndevice : nutanixDevices) {
				deviceIds.add(ndevice.getId());
			}
		}
		try {
			Map<Long,String> locationMap = new HashMap<Long,String>();
			if(clientId > 0) {
				List<Location> clientLocations = clientService.getLocationListByClient(clientId);
				if(clientLocations != null){
					for(Location location:clientLocations){
						locationMap.put(location.getId(), location.getName());
					}
				}
			}
			Map<Object, Object> data = new HashMap<Object, Object>();
			List<String> attributes = new ArrayList<String>();
			attributes.add("name");//1
			attributes.add("ipAddress");//2
			/*attributes.add("resourceTags.id");//3
			attributes.add("resourceTags.name");//4
			attributes.add("resourceTags.icon");//5*/
			attributes.add("osName");//3
			attributes.add("modifiedTime");//4
			attributes.add("nsgManagable");//5
			attributes.add("saId");//6
			attributes.add("classCode");//7
			attributes.add("agentInstalled");//8
			attributes.add("type");//9
			attributes.add("aliasName");//10
			attributes.add("model");//11
			attributes.add("make");//12
			attributes.add("description");//13
			attributes.add("currentLoggedOnUser");//14
			attributes.add("location.id");//15
			attributes.add("wsusEnabled");//16
			attributes.add("agentType");//17
			attributes.add("clientId");//18
			attributes.add("sourceType");//19
			attributes.add("deviceState");//20
			attributes.add("serialNumber");//21
			attributes.add("dnsName");//22
			if(!StringUtil.isEmpty(request.getParameter("status"))) {
				int status = ServletRequestUtils.getIntParameter(request, "status", -9);
				List<String> statusAttributes = new ArrayList<String>();
				statusAttributes.add("agentInstalled");//1
				statusAttributes.add("classCode");//2
				statusAttributes.add("sourceType");//3
				if(locationTab) {
					deviceObjectPage = resourceService.getInventoryResourceObjectPage(mspId, clientId, activeStatus, searchParamMap, "name", false, deviceGroupId, deviceTypeId,deviceLocationId, discovered, -1, -1, isUnAssignedDG, statusAttributes,isUnAssignedDL,null,false,providerId,SourceType.lookup(sType));
				} else if((gatewayDevices || showNutanixDevices) && !deviceIds.isEmpty()) {
					deviceObjectPage = inventoryService.getInventoryObjectPageByDeviceIds(mspId, clientId, activeStatus, sortName, isDecendingOrder, pageNo, pageSize, statusAttributes, deviceIds, searchParamMap);
				} else {
					deviceObjectPage = resourceService.getInventoryObjectPage(mspId, clientId, activeStatus, searchParamMap, "name", false, deviceTypeId, deviceGroupId, deviceLocationId, discovered, -1, -1, isUnAssignedDG, statusAttributes,isUnAssignedDL,searchStr,false,providerId);
				}
				if ((deviceObjectPage != null) && (deviceObjectPage.getTotalResults() > 0)) {
					Map<String,List<Long>> statusMap = deviceStatusService.getResourceIdsStatusMapByList(clientId, deviceObjectPage.getList(), 3, 1, false);

					List<Long> criticalDeviceIds = new ArrayList<Long>(), okDeviceIds = new ArrayList<Long>(), unknownDeviceIds = new ArrayList<Long>();
					if(statusMap != null && statusMap.size() > 0){
						if((statusMap.get("up") != null) && (statusMap.get("up").size() > 0)){
							okDeviceIds = statusMap.get("up");
						}
						if((statusMap.get("down") != null) && (statusMap.get("down").size() > 0)){
							criticalDeviceIds = statusMap.get("down");
						}
						if((statusMap.get("unknown") != null) && (statusMap.get("unknown").size() > 0)){
							unknownDeviceIds = statusMap.get("unknown");
						}
					}
					if(pageNo > 0 && pageSize > 0){
						List<Long> paginatedSubList = new ArrayList<Long>();
						switch(status) {
						case 1:
							if((okDeviceIds != null) && (okDeviceIds.size() > 0)){
								if(okDeviceIds.size() >= ((pageNo-1)*pageSize)){
									int sIdx = (pageNo-1)*pageSize;
									int eIdx = (okDeviceIds.size() > (pageNo*pageSize))?((pageNo*pageSize)-1):(okDeviceIds.size()-1);
									paginatedSubList = okDeviceIds.subList(sIdx, eIdx+1);
								}else{
									paginatedSubList = new ArrayList<Long>(okDeviceIds);
								}
								totalDevices = okDeviceIds.size();
							}
							break;
						case 0:
							if((criticalDeviceIds != null) && (criticalDeviceIds.size() > 0)){
								if(criticalDeviceIds.size() >= ((pageNo-1)*pageSize)){
									int sIdx = (pageNo-1)*pageSize;
									int eIdx = (criticalDeviceIds.size() > (pageNo*pageSize))?((pageNo*pageSize)-1):(criticalDeviceIds.size()-1);
									paginatedSubList = criticalDeviceIds.subList(sIdx, eIdx+1);
								}else{
									paginatedSubList = new ArrayList<Long>(criticalDeviceIds);
								}
								totalDevices = criticalDeviceIds.size();
							}
							break;
						case -1:
							if((unknownDeviceIds != null) && (unknownDeviceIds.size() > 0)){
								if(unknownDeviceIds.size() >= ((pageNo-1)*pageSize)){
									int sIdx = (pageNo-1)*pageSize;
									int eIdx = (unknownDeviceIds.size() > (pageNo*pageSize))?((pageNo*pageSize)-1):(unknownDeviceIds.size()-1);
									paginatedSubList = unknownDeviceIds.subList(sIdx, eIdx+1);
								}else{
									paginatedSubList = new ArrayList<Long>(unknownDeviceIds);
								}
								totalDevices = unknownDeviceIds.size();
							}
							break;
						default:
							break;
						}
						deviceObjectPage = null;
						if(paginatedSubList.size() > 0){
							if(locationTab) {
								deviceObjectPage = inventoryService.getInventoryObjectPageByResourceIds(mspId, clientId, activeStatus, sortName, isDecendingOrder, 1, pageSize, attributes, paginatedSubList , null);
							} else {
								deviceObjectPage = inventoryService.getInventoryObjectPageByDeviceIds(mspId, clientId, activeStatus, sortName, isDecendingOrder, 1, pageSize, attributes, paginatedSubList , null);
							}
						}
					}
					data.put("successCount", okDeviceIds.size());
					data.put("unReachableCount", criticalDeviceIds.size());
					data.put("failedCount", unknownDeviceIds.size());
				}
			}else{
				if(locationTab){
					deviceObjectPage = resourceService.getInventoryResourceObjectPage(mspId, clientId, activeStatus, searchParamMap, sortName, isDecendingOrder, deviceGroupId, deviceTypeId, deviceLocationId, discovered, pageNo, pageSize, isUnAssignedDG, attributes,isUnAssignedDL,searchStr,isDeleted,providerId,SourceType.lookup(sType));
				} else if(deviceTypecategoryId > 0){
					deviceObjectPage = resourceService.getClientInventoryObjectPageForAllOtherDevice(mspId, clientId, activeStatus, searchParamMap, sortName, isDecendingOrder,deviceTypecategoryId,deviceGroupId, deviceLocationId, false, pageNo, pageSize, false, attributes,false);
				} else if((gatewayDevices || showVoIPDevices || showWirelessLANDevices || showWLANControllers || showThickApDevices || showNutanixDevices) && !deviceIds.isEmpty()) {
					deviceObjectPage = inventoryService.getInventoryObjectPageByDeviceIds(mspId, clientId, activeStatus, sortName, isDecendingOrder, pageNo, pageSize, attributes, deviceIds, searchParamMap);
				} else if(isDeleted) {
					if(!StringUtil.isEmpty(searchStr)) {
						deviceObjectPage = resourceService.getInventoryObjectPage(mspId, clientId, activeStatus, searchParamMap, sortName, isDecendingOrder, deviceTypeId, deviceGroupId, deviceLocationId, discovered, pageNo, pageSize, isUnAssignedDG, attributes,isUnAssignedDL,searchStr,isDeleted, providerId);
					} else {
						deviceObjectPage = resourceService.getInventoryObjectPage(mspId, clientId, activeStatus, searchParamMap, sortName, isDecendingOrder, deviceTypeId, deviceGroupId, deviceLocationId, discovered, pageNo, pageSize, isUnAssignedDG, attributes,isUnAssignedDL,null,isDeleted,providerId);
					}
				} else {
					if(!StringUtil.isEmpty(searchStr)) {
						if(deviceTypeId > 0) {
							//Special case to match the device list in the device tree
							List<Long> matchedDevices = new ArrayList<Long>();
							ResourceIndexManagementService indexService = new ResourceIndexManagementServiceImpl();
							List<JSONObject> jsonList = indexService.searchResources(clientId, searchStr);
							for(JSONObject json : jsonList) {
								if(SourceType.DEVICE.name().equalsIgnoreCase(json.getString("type"))){
									matchedDevices.add(json.getLong("id"));
								}
							}
							deviceObjectPage = resourceService.getInventoryObjectPage(mspId, clientId, activeStatus, matchedDevices, sortName, isDecendingOrder, deviceTypeId, deviceGroupId, deviceLocationId, discovered, pageNo, pageSize, isUnAssignedDG, attributes,isUnAssignedDL,false);
						} else {
							deviceObjectPage = resourceService.getInventoryObjectPage(mspId, clientId, activeStatus, searchParamMap, sortName, isDecendingOrder, deviceTypeId, deviceGroupId, deviceLocationId, discovered, pageNo, pageSize, isUnAssignedDG, attributes,isUnAssignedDL,searchStr,false,providerId);
						}
					}else{
						deviceObjectPage = resourceService.getInventoryObjectPage(mspId, clientId, activeStatus, searchParamMap, sortName, isDecendingOrder, deviceTypeId, deviceGroupId, deviceLocationId, discovered, pageNo, pageSize, isUnAssignedDG, attributes,isUnAssignedDL,null,false,providerId);
					}
				}
				totalDevices = deviceObjectPage.getTotalResults();
			}

			if (deviceObjectPage != null) {
				Iterator<Object[]> deviceItr = deviceObjectPage.getList().listIterator();
				Location location = null;
				while (deviceItr.hasNext()) {
					Object[] deviceRowObj = deviceItr.next();
					deviceId = (Long) deviceRowObj[0];
					device = new Device();
					device.setId(deviceId);
					device.setName((String) deviceRowObj[1]);
					device.setIpAddress((String) deviceRowObj[2]);
					/*DeviceType deviceType = new DeviceType();
					deviceType.setId((Long) deviceRowObj[3]);
					deviceType.setName((String) deviceRowObj[4]);
					deviceType.setIcon((String) deviceRowObj[5]);
					device.setDeviceType(deviceType);*/
					device.setOsName((String) deviceRowObj[3]);
					device.setModifiedTime((Date) deviceRowObj[4]);
					device.setNsgManagable((Boolean) deviceRowObj[5]);
					device.setSaId((Long) deviceRowObj[6]);
					device.setClassCode((String) deviceRowObj[7]);
					device.setAgentInstalled((Boolean) deviceRowObj[8]);
					device.setType((String) deviceRowObj[9]);
					device.setAliasName((String) deviceRowObj[10]);
					device.setModel((String) deviceRowObj[11]);
					device.setMake((String) deviceRowObj[12]);
					device.setDescription((String) deviceRowObj[13]);
					device.setCurrentLoggedOnUser((String) deviceRowObj[14]);
					device.setAgentType((int)deviceRowObj[17]);
					device.setClientId((Long) deviceRowObj[18]);
					if(deviceRowObj[15] != null) {
						long locationId = (Long) deviceRowObj[15];
						if((locationId > 0) && (locationMap.get(locationId) != null)) {
							location = new Location();
							location.setName(locationMap.get(locationId));
							device.setLocation(location);
						}
					}
					device.setWsusEnabled((Boolean) deviceRowObj[16]);
					SourceType sourceType = (SourceType)deviceRowObj[19];
					device.setSourceType(sourceType);
					device.setDeviceState((String) deviceRowObj[20]);
					device.setSerialNumber((String) deviceRowObj[21]);
					device.setDnsName((String) deviceRowObj[22]);
					deviceList.add(device);
					deviceStates.put(device.getId(), -9);
					deviceIds.add(device.getId());
				}
			}
			if(deviceIds != null && deviceIds.size() > 0) {
				if(AuthenticationContext.hasPermission("CUSTOM_ATTRIBUTES_VIEW",clientId)) {
					data.put("tagValuesMap", tagService.getKeyValuePairofTagIdAndTagValueByTagggableEntity(deviceIds, Device.class.getName(), clientId));
				}
				data.put("deviceNotes", deviceService.deviceNotesMap(Util.toPrimitiveArray(deviceIds)));
				data.put("skusMap", skuService.deviceSkuCountMap(Util.toPrimitiveArray(deviceIds)));
				data.put("loginUserInfo", NCareUtil.getDeviceUserLoggedInInfoMap(Util.toPrimitiveArray(deviceIds)));
				List<AgentVersion> agentVersionList = deviceManagementService.getAgentVersionList(deviceIds);
				if(agentVersionList != null){
					for(AgentVersion agentVersion:agentVersionList) {
						agentVersionDetails.put(agentVersion.getDevice().getId(),agentVersion);
					}
				}
			}
			List<Long> multipleMasterAgentDevices = new ArrayList<Long>();
			List<ManagementGatewayProfile> profile = sgService.getAgentProfilesByClientId(clientId);
			if(profile != null){
				for(ManagementGatewayProfile masterAgentProfile:profile){
					multipleMasterAgentDevices.add(masterAgentProfile.getDeviceId());
				}
			}

			data.put("multipleMasterAgentDevices", multipleMasterAgentDevices);
			client = clientService.getClient(clientId);
			if (client != null) {
				//boolean gatewayService = client.isSaClient();
				confList = sgService.getRegisteredManagementGatewayProfileList(client.getId());
				sgMap = new HashMap<Long, ManagementGatewayProfile>();
				List<Long> gatewayIds = new ArrayList<>();
				//int i = 1;
				for (ManagementGatewayProfile sg : confList) {
					/*if (gatewayService) {
						sgMap.put(sg.getId(), sg);
						collectorCountMap.put(sg.getId(), i);
						i++;
					}*/
					sgMap.put(sg.getId(), sg);
					//For gateway and agent devices
					if (sg.getProfileType().equalsIgnoreCase("Gateway") && sg.getManagementGateway() != null && sg.getManagementGateway().getDeviceId() > 0) {
						Device gatewaydevice = ServiceFactory.getInstance().getDetourSecurityService().getDevice(sg.getManagementGateway().getDeviceId());
						if ( null != gatewaydevice) {
							gatewayIds.add(gatewaydevice.getId());
							/*if (gatewayService) {
								assignedGatewayNameMap.put(sg.getId(), gatewaydevice.getName());
							}
							Map to remove unwanted gateway icon when client don't have the gateway device irrespective of gateway service
							deviceAssignedGatewayNameMap.put(sg.getId(), gatewaydevice.getName());*/
							assignedGatewayNameMap.put(sg.getId(), gatewaydevice.getName());
						}
					}
				}
				data.put("gatewayIds", gatewayIds);
			}
			data.put("client", client);
			data.put("devicePage", clientService.toPartialPage(deviceList, pageNo, pageSize, totalDevices));
			data.put("deviceStates", deviceStates);
			data.put("devicePurchaseDate", devicePurchaseDate);
			data.put("deviceWarrantyDate", deviceWarrantyDate);
			data.put("lastPatchInstallDate", lastPatchInstallDate);
			request.setAttribute("deviceTypeId", deviceTypeId);
			data.put("sgMap", sgMap);
			data.put("assignedGatewayNameMap", assignedGatewayNameMap);
			data.put("deviceAssignedGatewayNameMap", deviceAssignedGatewayNameMap);
			data.put("agentPolicyMap", agentPolicyMap);
			//data.put("collectorCountMap", collectorCountMap);
			data.put("filterInfo", filterInfo);
			data.put("maintenanceDeviceWindows", maintenanceDeviceWindows);
			data.put("agentConnectedTypeMap", agentVersionDetails);
			return new ModelAndView("device/inventoryData", "clientInfo", data);

		} catch (Throwable t) {
			logger.error("Failed to load inventory data. " + t.getMessage(), t);
			throw new Exception();
		}
	
	}
}
