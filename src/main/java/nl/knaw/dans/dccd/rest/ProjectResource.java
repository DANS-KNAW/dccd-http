/*******************************************************************************
 * Copyright 2015 DANS - Data Archiving and Networked Services
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package nl.knaw.dans.dccd.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.tridas.schema.*;

import nl.knaw.dans.common.lang.dataset.DatasetState;
import nl.knaw.dans.common.lang.search.SearchResult;
import nl.knaw.dans.common.lang.search.SortOrder;
import nl.knaw.dans.common.lang.search.simple.SimpleField;
import nl.knaw.dans.common.lang.search.simple.SimpleSearchRequest;
import nl.knaw.dans.common.lang.search.simple.SimpleSortField;
import nl.knaw.dans.common.lang.service.exceptions.ServiceException;
import nl.knaw.dans.common.lang.util.Range;
import nl.knaw.dans.common.solr.SolrUtil;
import nl.knaw.dans.dccd.application.services.DataServiceException;
import nl.knaw.dans.dccd.application.services.DccdDataService;
import nl.knaw.dans.dccd.application.services.DccdSearchService;
import nl.knaw.dans.dccd.application.services.SearchServiceException;
import nl.knaw.dans.dccd.model.DccdAssociatedFileBinaryUnit;
import nl.knaw.dans.dccd.model.DccdOriginalFileBinaryUnit;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.model.Project;
import nl.knaw.dans.dccd.model.ProjectPermissionLevel;
import nl.knaw.dans.dccd.model.DccdUser.Role;
import nl.knaw.dans.dccd.model.ProjectPermissionMetadata;
import nl.knaw.dans.dccd.model.UserPermission;
import nl.knaw.dans.dccd.rest.tridas.TridasPermissionRestrictor;
import nl.knaw.dans.dccd.rest.tridas.TridasRequestedLevelRestrictor;
import nl.knaw.dans.dccd.rest.util.UrlConverter;
import nl.knaw.dans.dccd.rest.util.XmlStringUtil;
import nl.knaw.dans.dccd.search.DccdProjectSB;
import nl.knaw.dans.dccd.search.DccdSB;
import nl.knaw.dans.dccd.tridas.TridasNamespacePrefixMapper;

/**
 * 
 * @author paulboon
 *
 */
@Path("/project")
public class ProjectResource extends AbstractProjectResource {

	/**
	 * extra parameters for 'harvesting' clients.
	 */
	public static final String MODIFIED_FROM_PARAM = "modFrom";
	public static final String MODIFIED_UNTIL_PARAM = "modUntil";
 
	/**
	 * Get the complete tridas file, 
	 * you need to be logged in and authorized for download!
	 * It is using the data service instead of the search service to get all of the data
	 * 
 	 * @param id
 	 * 			The store ID
	 * @return
	 */
	@GET
	@Path("/{sid}/tridas")
	public Response getProjectTridasBySid(@PathParam("sid") String id) {
		// TODO prevent injection, sid must be "dccd:<number>"
		
		// authenticate user
		DccdUser user = null;
		try {
			user = authenticate();
			if (user == null)
				return Response.status(Status.UNAUTHORIZED).build();
		} catch (ServiceException e1) {
			e1.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		// get the project
		try
		{
			Project project = DccdDataService.getService().getProject(id);
			TridasProject tridasProject = project.getTridas();
			
			if (!project.isDownloadAllowed(user) ) {
				//return Response.status(Status.UNAUTHORIZED).build();
				if(!project.isViewingAllowed(user))
					return Response.status(Status.UNAUTHORIZED).build();
				
				// Filter it for 'partial' download; what would be visible!
				ProjectPermissionLevel level = project.getEffectivePermissionLevel(user);
				TridasPermissionRestrictor permissionRestrictor = new TridasPermissionRestrictor();
				permissionRestrictor.restrictToPermitted(tridasProject, level);
			}
			
			// Get all tridas xml
			java.io.StringWriter sw = new StringWriter();
			JAXBContext jaxbContext = null;
			// System.out.println("\n TRiDaS XML, non valid, but with the structure");
			try {
				// can it find the schema, and why not part of the lib
				jaxbContext = JAXBContext.newInstance("org.tridas.schema");
				Marshaller marshaller = jaxbContext.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				// improve the namespace mapping
				marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new TridasNamespacePrefixMapper());

				marshaller.marshal(tridasProject, sw);
			} catch (JAXBException e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			// Always XML, because its TRiDaS
			return Response.status(Status.OK).entity(sw.toString()).build();
		}
		catch (DataServiceException e)
		{
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Note that for the API consistency this should be the reverse mapping of 
	 * AbstractProjectResource.MAP_PERMISSION_TO_ENTITYLEVEL
	 */
	@SuppressWarnings({ "serial" })
	public static final Map<String, ProjectPermissionLevel> MAP_ENTITYLEVEL_TO_PERMISSION = 
		    Collections.unmodifiableMap(new HashMap<String, ProjectPermissionLevel>() {{ 
		        put("project", ProjectPermissionLevel.PROJECT);
		        put("object", ProjectPermissionLevel.OBJECT);
		        put("element", ProjectPermissionLevel.ELEMENT);
		        put("sample", ProjectPermissionLevel.SAMPLE);
		        put("radius", ProjectPermissionLevel.RADIUS);
		        put("series", ProjectPermissionLevel.SERIES);
		        put("values", ProjectPermissionLevel.VALUES);
		    }});

	@GET
	@Path("/{sid}/tridas/{entityLevel}")
	public Response getProjectTridasBySidForLevel(@PathParam("sid") String id, @PathParam("entityLevel") String entityLevel) {
		if (!MAP_ENTITYLEVEL_TO_PERMISSION.containsKey(entityLevel))
		{
			// we only support the strings from the map
			return Response.status(Status.NOT_FOUND).build();
		}
		
		// authenticate user
		DccdUser user = null;
		try {
			user = authenticate();
			if (user == null)
				return Response.status(Status.UNAUTHORIZED).build();
		} catch (ServiceException e1) {
			e1.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		// get the project
		try
		{
			Project project = DccdDataService.getService().getProject(id);
			TridasProject tridasProject = project.getTridas();
			
			if (!project.isDownloadAllowed(user) ) {
				//return Response.status(Status.UNAUTHORIZED).build();
				if(!project.isViewingAllowed(user))
					return Response.status(Status.UNAUTHORIZED).build();
				
				// Filter it for 'partial' download; what would be visible!
				// first remove unwanted (not requested) stuff
				// TEST
				//ProjectPermissionLevel requestedlevel = ProjectPermissionLevel.PROJECT;
				ProjectPermissionLevel requestedlevel = MAP_ENTITYLEVEL_TO_PERMISSION.get(entityLevel);
				TridasRequestedLevelRestrictor requestedRestrictor = new TridasRequestedLevelRestrictor();
				requestedRestrictor.restrictToPermitted(tridasProject, requestedlevel);
				
				// Finally use permission, if we requested more than allowed
				ProjectPermissionLevel level = project.getEffectivePermissionLevel(user);
				if (!requestedlevel.isPermittedBy(level))
				{
					TridasPermissionRestrictor permissionRestrictor = new TridasPermissionRestrictor();
					permissionRestrictor.restrictToPermitted(tridasProject, level);
				}
			}
			
			// Get all tridas xml
			java.io.StringWriter sw = new StringWriter();
			JAXBContext jaxbContext = null;
			// System.out.println("\n TRiDaS XML, non valid, but with the structure");
			try {
				// can it find the schema, and why not part of the lib
				jaxbContext = JAXBContext.newInstance("org.tridas.schema");
				Marshaller marshaller = jaxbContext.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);// testing
				marshaller.marshal(tridasProject, sw);
				
				// NOTE namespace is ugly, I did fix that somewhere?
				
			} catch (JAXBException e) {
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
			
			// Always XML, because its TRiDaS
			return Response.status(Status.OK).entity(sw.toString()).build();
		}
		catch (DataServiceException e)
		{
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}	
	}
	
	/**
	 * Produce a list in xml with the filenames, so you can request a download
	 * normally you need to be logged in and authorized for download!
	 * 
	 * @param id
	 * 			The store ID
	 * @return A response containing the complete list of associated files
	 */
	@GET
	@Path("/{sid}/associated")
	public Response listAssociatedFilesByProjectSid(@PathParam("sid") String id) {
		// authenticate user
		DccdUser user = null;
		try {
			user = authenticate();
			if (user == null)
				return Response.status(Status.UNAUTHORIZED).build();
		} catch (ServiceException e1) {
			e1.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		try {
			Project project = DccdDataService.getService().getProject(id);

			// For listing download is not needed!
			//if (!project.isDownloadAllowed(user) ) {
			//	return Response.status(Status.UNAUTHORIZED).build();
			//}

			java.io.StringWriter sw = new StringWriter();
			
			sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); // XML instruction
			sw.append("<files>");
			List<DccdAssociatedFileBinaryUnit> fileBinaryUnits = project.getAssociatedFileBinaryUnits();
			for (DccdAssociatedFileBinaryUnit unit : fileBinaryUnits) {
				sw.append(getXMLElementString("file", unit.getFileName()));
			}
			sw.append("</files>");
			
			return responseXmlOrJson(sw.toString());
		} catch (DataServiceException e) {
			e.printStackTrace();
		}
		
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}
	
	/**
	 * normally you need to be logged in and authorized for download!
	 * 
	 * @param id
	 * 			The store ID
	 * @param filename
	 * 			The name of the file to retrieve/download
	 * @return
	 */
	@GET
	@Path("/{sid}/associated/{filename}")
	public Response getAssociatedFilesByProjectSid(@PathParam("sid") String id, 
													@PathParam("filename") String filename) {
		// authenticate user
		DccdUser user = null;
		try {
			user = authenticate();
			if (user == null)
				return Response.status(Status.UNAUTHORIZED).build();
		} catch (ServiceException e1) {
			e1.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		try {
			Project project = DccdDataService.getService().getProject(id);
	
			if (!project.isDownloadAllowed(user) ) {
				return Response.status(Status.UNAUTHORIZED).build();
			}

			DccdAssociatedFileBinaryUnit requestedUnit = null;
			List<DccdAssociatedFileBinaryUnit> fileBinaryUnits = project.getAssociatedFileBinaryUnits();
			for (DccdAssociatedFileBinaryUnit unit : fileBinaryUnits) {
				if (unit.getFileName().contentEquals(filename)) {
					requestedUnit = unit; 
					break; // Found!
				}
			}
			if (requestedUnit == null) {
				// not found
				return Response.status(Status.NOT_FOUND).build();
			} else {
				// found
				String unitId = requestedUnit.getUnitId();
				// get the url
				URL fileURL = DccdDataService.getService().getFileURL(project.getSid(), unitId);
				
				// NOTE we have all bytes in memory, maybe we can get circumvent it with streaming 
				byte[] bytes = UrlConverter.toByteArray(fileURL);

				// The file bytes
				return Response.status(Status.OK).entity(bytes).build();
			}
		} catch (DataServiceException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}

	/**
	 * Produce a list in xml with the filenames, so you can request a download
	 * normally you need to be logged in and authorized for download!
	 * 
	 * @param id
	 * 			The store ID
	 * @return A response containing the complete list of original files
	 */
	@GET
	@Path("/{sid}/originalvalues")
	public Response listOriginalFilesByProjectSid(@PathParam("sid") String id) {
		// authenticate user
		DccdUser user = null;
		try {
			user = authenticate();
			if (user == null)
				return Response.status(Status.UNAUTHORIZED).build();
		} catch (ServiceException e1) {
			e1.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		try {
			Project project = DccdDataService.getService().getProject(id);

			// For listing download is not needed!
			//if (!project.isDownloadAllowed(user) ) {
			//	return Response.status(Status.UNAUTHORIZED).build();
			//}

			java.io.StringWriter sw = new StringWriter();
			
			sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); // XML instruction
			sw.append("<files>");
			List<DccdOriginalFileBinaryUnit> fileBinaryUnits = project.getOriginalFileBinaryUnits();
			for (DccdOriginalFileBinaryUnit unit : fileBinaryUnits) {
				sw.append(getXMLElementString("file", unit.getFileName()));
			}
			sw.append("</files>");
			
			return responseXmlOrJson(sw.toString());
		} catch (DataServiceException e) {
			e.printStackTrace();
		}
		
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}
	
	/**
	 * normally you need to be logged in and authorized for download!
	 * 
	 * @param id
	 * 			The store ID
	 * @param filename
	 * 			The name of the file to retrieve/download
	 * @return
	 */
	@GET
	@Path("/{sid}/originalvalues/{filename}")
	public Response getOriginalFilesByProjectSid(@PathParam("sid") String id, 
													@PathParam("filename") String filename) {
		// authenticate user
		DccdUser user = null;
		try {
			user = authenticate();
			if (user == null)
				return Response.status(Status.UNAUTHORIZED).build();
		} catch (ServiceException e1) {
			e1.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		try {
			Project project = DccdDataService.getService().getProject(id);
	
			if (!project.isDownloadAllowed(user) ) {
				return Response.status(Status.UNAUTHORIZED).build();
			}

			DccdOriginalFileBinaryUnit requestedUnit = null;
			List<DccdOriginalFileBinaryUnit> fileBinaryUnits = project.getOriginalFileBinaryUnits();
			for (DccdOriginalFileBinaryUnit unit : fileBinaryUnits) {
				if (unit.getFileName().contentEquals(filename)) {
					requestedUnit = unit; 
					break; // Found!
				}
			}
			if (requestedUnit == null) {
				// not found
				return Response.status(Status.NOT_FOUND).build();
			} else {
				// found
				String unitId = requestedUnit.getUnitId();
				// get the url
				URL fileURL = DccdDataService.getService().getFileURL(project.getSid(), unitId);
				
				// NOTE we have all bytes in memory, maybe we can get circumvent it with streaming 
				byte[] bytes = UrlConverter.toByteArray(fileURL);

				// The file bytes
				return Response.status(Status.OK).entity(bytes).build();
			}
		} catch (DataServiceException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}
	
	@GET
	@Path("/{sid}/permission")
	public Response getPermission(@PathParam("sid") String id) 
	{
		// authenticate user
		DccdUser user = null;
		try {
			user = authenticate();
			if (user == null)
				return Response.status(Status.UNAUTHORIZED).build();
		} catch (ServiceException e1) {
			e1.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		try {
			Project project = DccdDataService.getService().getProject(id);

			if (user.hasRole(Role.ADMIN) || user.getId().equals(project.getOwnerId())) 
			{
				ProjectPermissionMetadata permissionMetadata = project.getPermissionMetadata();

				java.io.StringWriter sw = new StringWriter();
				sw.append(XmlStringUtil.XML_INSTRUCTION_STR);
				sw.append("<permission>");
				sw.append(XmlStringUtil.getXMLElementString("projectId", project.getSid()));
				sw.append(XmlStringUtil.getXMLElementString("ownerId", project.getOwnerId()));
				sw.append(XmlStringUtil.getXMLElementString("defaultLevel", permissionMetadata.getDefaultLevel().toString()));

				ArrayList<UserPermission> userPermissionsArrayList = permissionMetadata.getUserPermissionsArrayList();
				if (!userPermissionsArrayList.isEmpty()) {
					sw.append("<userPermissions>");
					for (UserPermission userPermission : userPermissionsArrayList) {
						sw.append("<userPermission>");
						sw.append(XmlStringUtil.getXMLElementString("userId", userPermission.getUserId()));
						sw.append(XmlStringUtil.getXMLElementString("level", userPermission.getLevel().toString()));
						sw.append("</userPermission>");
					}
					sw.append("</userPermissions>");
				}
				sw.append("</permission>");
				
				return responseXmlOrJson(sw.toString());
			} else {
				return Response.status(Status.UNAUTHORIZED).build();
			}
		} catch (DataServiceException e) {
			e.printStackTrace();
		}
		
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}

	/**
	 * NOTE for searching with permission it might be needed to use the  ObjectSB, 
	 * but we then get multiple results...
	 * Project searching was in the GUI only on MyProjects!
	 * We need to test it!
	 * With ObjectSB only the tridas above the defaultlevel is indexed!!!!
	 * Also note that advanced searching was always for logged-in users, never for anonymous ones. 
	 * Maybe use Path: project/object/query ?
	 *
	 * One solution might be to add a PublicProjectSB 
	 * and reindex with that one as well, filling it similar to the ObjectSB
	 */

	
	/**
	 * List the projects with their information; 
	 * including sid's which can be used to get more data
	 * 
	 * @param offset
	 * @param limit
	 * @return A response containing the paged list of Published/Archived projects
	 */
	@GET
	//@Path("/")
	public Response getProjects(
			   @QueryParam(MODIFIED_FROM_PARAM) String modFromStr,
			   @QueryParam(MODIFIED_UNTIL_PARAM) String modUntilStr,
	           @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
	           @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit) {
		SearchResult<? extends DccdSB> searchResults = null;
		
		SimpleSearchRequest request = new SimpleSearchRequest();
		request.setOffset(offset);
		// TODO support getting all results in a nice way
		// limit=0 means no-limit or 'give me everything'
		//if (limit == 0) 
		//{
		//	// Solr  has a default of 10 and no way to specify 'all'
		//	limit = Integer.MAX_VALUE;
		//}
		request.setLimit(limit)	;
		
		// Show Project and not the standard Object result
		request.addFilterBean(DccdProjectSB.class);

		if (modFromStr != null || modUntilStr != null)
		{
			// Sorting on the date makes sense 
			// Recently changed first (last archived)
			request.addSortField(new SimpleSortField(DccdProjectSB.ADMINISTRATIVE_STATE_LASTCHANGE, SortOrder.DESC));
			
			try {
				addFilterQueryForModified(request, modFromStr, modUntilStr);
			} catch (IllegalArgumentException e) {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		else
		{
			// sorting on the SID makes sense
			request.addSortField(new SimpleSortField(DccdProjectSB.PID_NAME, SortOrder.ASC));
		}

		// Make sure it is published and not draft!
		SimpleField<String> stateField = new SimpleField<String>(DccdProjectSB.ADMINISTRATIVE_STATE_NAME, 
				DatasetState.PUBLISHED.toString());
		request.addFilterQuery(stateField);

		try {
			searchResults = DccdSearchService.getService().doSearch(request);
			return responseXmlOrJson(getProjectListSearchResultAsXml(searchResults, offset, limit));
		} catch (SearchServiceException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	private void addFilterQueryForModified(SimpleSearchRequest request, 
			final String modFromStr, final String modUntilStr) 
		throws IllegalArgumentException
	{
		DateTimeWrapper modFrom = null;
		DateTimeWrapper modUntil = null;

	    // parse the strings an get DateTime objects 
		// Note that it should be the same format as we use when outputting the "stateChanged" property. 

		if (modFromStr != null) {
	    	DateTimeFormatter df = ISODateTimeFormat.dateTime();
	        modFrom = new DateTimeWrapper(df.parseDateTime(modFromStr));
		}
		
		if (modUntilStr != null) {
	    	DateTimeFormatter df = ISODateTimeFormat.dateTime();
	        modUntil = new DateTimeWrapper(df.parseDateTime(modUntilStr));
		}
		
		if (modFrom != null || modUntil != null) {
			// use DccdProjectSB.ADMINISTRATIVE_STATE_LASTCHANGE
			// Note that for Published (aka Archived) projects this is the timestamp for the publishing. 
			// Draft projects can be modified without the 'change of state', but we won't expose those. 
			
			SimpleField<Range<DateTimeWrapper>> periodField = 
					new SimpleField<Range<DateTimeWrapper>>(DccdProjectSB.ADMINISTRATIVE_STATE_LASTCHANGE);
			periodField.setValue(new Range<DateTimeWrapper>(modFrom, modUntil));
			request.addFilterQuery(periodField);
		}		
	}
	
	// Wrapper to fix problems with DateTime within a Range query 
	// that produces a wrong Solr query url
	// Should be fixed in 'dans-solr' commons project  
	// nl.knaw.dans.common.solr.SolrUtil.toString(final Range<?> range)
	//
	public class DateTimeWrapper implements Comparable<DateTimeWrapper> {
		public DateTime d;
		public DateTimeWrapper(DateTime d) { this.d = d; } 
	    public String toString()
	    {
	    	return SolrUtil.toString(d);
	    }
		@Override
		public int compareTo(DateTimeWrapper o) {
			return d.compareTo(o.d);
		}
	}
	
	/**
	 * Get the 'open access' project information, 
	 * what you see if you search without being logged in
	 * Note that using search service is more efficient than going into the fedora archive/store
	 * 
	 * @param id
	 * 			The store ID
	 * @return
	 */
	@GET
	@Path("/{sid}")
	public Response getProjectByStoreId(@PathParam("sid") String id) {
				
		SearchResult<? extends DccdSB> searchResults = null;
		
		SimpleSearchRequest request = new SimpleSearchRequest();
		// one and only one result needed
		request.setLimit(1)	;
		request.setOffset(0);
		
		// Show Project and not the standard Object result
		request.addFilterBean(DccdProjectSB.class);
		request.addSortField(new SimpleSortField(DccdProjectSB.PID_NAME, SortOrder.ASC));

		if (!isRequestByAdmin()) {
			// Make sure it is published and not draft!
			SimpleField<String> stateField = new SimpleField<String>(DccdProjectSB.ADMINISTRATIVE_STATE_NAME, 
					DatasetState.PUBLISHED.toString());
			request.addFilterQuery(stateField);
		}
		
		// restrict to specific sid
		SimpleField<String> idField = new SimpleField<String>(DccdProjectSB.PID_NAME, id);
		request.addFilterQuery(idField);

		try {
			searchResults = DccdSearchService.getService().doSearch(request);
			if (searchResults.getHits().isEmpty()) {
				return Response.status(Status.NOT_FOUND).build();
			} else {
				DccdSB dccdSB = searchResults.getHits().get(0).getData();
				return responseXmlOrJson(getProjectSearchResultAsXml(dccdSB));
			}
		} catch (SearchServiceException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/** 
	 * Construct search result information as XML String
	 * 
	 * @param dccdSB
	 *            search result
	 */
	private String getProjectSearchResultAsXml(DccdSB dccdSB) {
		java.io.StringWriter sw = new StringWriter();
		
		sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); // XML instruction
		sw.append("<project>");
		appendSearchResultDataAsXml(sw, dccdSB);
		sw.append("</project>");

		return sw.toString();
	}
	
	/**
	 * Append project XML
	 * 
	 * @param sw
	 *            writer to append to
	 * @param dccdSB
	 *            search result
	 */
	protected void appendSearchResultDataAsXml(java.io.StringWriter sw, DccdSB dccdSB) {
		appendProjectPublicDataAsXml(sw, dccdSB);

		appendProjectPublicLocationAsXml(sw, dccdSB);
		appendProjectPublicTimeRangeAsXml(sw, dccdSB);
		
		appendProjectPublicTaxonsAsXml(sw, dccdSB);
		appendProjectPublicTypesAsXml(sw, dccdSB);
		
		appendProjectPublicDescriptionAsXml(sw, dccdSB);

		// NOTE should the id, owner, state and permission be in here as well
		// but only when logged in... 		?

		// permission
		appendProjectPermissionAsXml(sw, dccdSB);
	}
	
	private boolean isRequestByAdmin() 
	{
		try {
			DccdUser requestingUser = authenticate();
			if (requestingUser != null && requestingUser.hasRole(Role.ADMIN) )
				return true;
			else
				return false;
		} catch (ServiceException eAuth) {
			eAuth.printStackTrace();
			return false; // we don't know so; false
		}
	}
	
	// TODO query and have the objects as result list, just like the GUI and a bit less than download search results
	// therefore we could have Object title and then the project info, nothing more
	// the problem is how to identify the objects....
	
}
