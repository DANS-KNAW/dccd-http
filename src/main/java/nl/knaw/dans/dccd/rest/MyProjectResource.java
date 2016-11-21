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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.multipart.FormDataParam;

import nl.knaw.dans.common.lang.search.SearchResult;
import nl.knaw.dans.common.lang.search.SortOrder;
import nl.knaw.dans.common.lang.search.simple.CombinedOptionalField;
import nl.knaw.dans.common.lang.search.simple.SimpleField;
import nl.knaw.dans.common.lang.search.simple.SimpleSearchRequest;
import nl.knaw.dans.common.lang.search.simple.SimpleSortField;
import nl.knaw.dans.common.lang.service.exceptions.ServiceException;
import nl.knaw.dans.dccd.util.FileUtil;
import nl.knaw.dans.dccd.application.services.DataServiceException;
import nl.knaw.dans.dccd.application.services.DccdDataService;
import nl.knaw.dans.dccd.application.services.DccdSearchService;
import nl.knaw.dans.dccd.application.services.SearchServiceException;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.model.Project;
import nl.knaw.dans.dccd.rest.archival.FileExtractor;
import nl.knaw.dans.dccd.rest.archival.DccdProjectImporter;
import nl.knaw.dans.dccd.search.DccdProjectSB;
import nl.knaw.dans.dccd.search.DccdSB;

/**
 * Projects, but limiting to the projects of the (authenticated) member, 
 * also handling draft versions. 
 * 
 * @author paulboon
 *
 */
@Path("/myproject")
public class MyProjectResource extends AbstractProjectResource {
	private static final Logger LOGGER   = LoggerFactory.getLogger(MyProjectResource.class);

	@DELETE
	@Path("/{sid}")
	public Response deleteProjectByStoreId(@PathParam("sid") String id) {
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

		// TODO implement
		Project project;
		try {
			project = DccdDataService.getService().getProject(id);
			// test if it has draft status?
			DccdDataService.getService().deleteProject(project, user);
		} catch (DataServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		return Response.status(200).build();
	}
	
	/* example
	 * curl -u normaltestuser:testtest -i -F file=@test.pdf http://localhost:8080/dccd-rest/rest/myproject
	 */
	@POST
	//@Path("/")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
		@FormDataParam("file") InputStream uploadedInputStream
		//,@FormDataParam("name") FormDataContentDisposition fileDetail
		) {

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
		
		String output = "";
		File tempDir = null;
		try {
			tempDir = FileExtractor.createTempDir();
			List<File> unzip = FileExtractor.unzip(uploadedInputStream, tempDir);
		    
			// get some logging info
			//output = "UNZIPPED";
			//for (File file : unzip) {
			//	output +="\n" + file.getPath();
			//}
			
			File zipFolder = unzip.get(0);
			LOGGER.info("zipfolder: " + zipFolder.getAbsolutePath());
			// get the data folder and then the TRiDaS file
			File projectFolder = FileExtractor.getDataFolder(zipFolder);
			
			output = DccdProjectImporter.importProject(projectFolder, user.getId());
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			output = "NOT imported";
		} finally {			
			// need to delete temp after ingest
			if (tempDir != null)
				FileUtil.deleteDirectory(tempDir);
		}

		// construct response that gives you the SID if import succeded and an error otherwise 
		// TODO make the result structured; XML or JSON
		return Response.status(200).entity(output).build();
	}
	
	/**
	 * List the projects with sid's
	 * needs authentication!
	 * 
	 * @param offset
	 * @param limit
	 * @return
	 */
	@GET
	//@Path("/")
	public Response getProjects(
	           @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
	           @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit) {
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
		
		SearchResult<? extends DccdSB> searchResults = null;
		
		SimpleSearchRequest request = new SimpleSearchRequest();
		request.setLimit(limit)	;
		request.setOffset(offset);
		// Show Project and not the standard Object result
		request.addFilterBean(DccdProjectSB.class);
		request.addSortField(new SimpleSortField(DccdProjectSB.PID_NAME, SortOrder.ASC));
	
		// make sure its of the owner
    	// restrict results to the current user as owner
		String userId =  user.getId();
		// Escape any whitespace characters, because otherwise the search will fail!
		userId = userId.replaceAll(" ", "\\\\ ");
		SimpleField<String> ownerIdField = new SimpleField<String>(DccdSB.OWNER_ID_NAME, userId);
		request.addFilterQuery(ownerIdField);
		
		try {
			searchResults = DccdSearchService.getService().doSearch(request);
			
			//return Response.status(Status.OK)
			//		.entity(getProjectListSearchResultAsXml(searchResults, offset, limit))
			//		.build();
			return responseXmlOrJson(getProjectListSearchResultAsXml(searchResults, offset, limit, user));
		} catch (SearchServiceException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Path("/{sid}")
	public Response getProjectByStoreId(@PathParam("sid") String id) {
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
		
		SearchResult<? extends DccdSB> searchResults = null;
		
		SimpleSearchRequest request = new SimpleSearchRequest();
		// one and only one result needed
		request.setLimit(1)	;
		request.setOffset(0);
		
		// Show Project and not the standard Object result
		request.addFilterBean(DccdProjectSB.class);
		request.addSortField(new SimpleSortField(DccdProjectSB.PID_NAME, SortOrder.ASC));
	
		// make sure its of the owner
    	// restrict results to the current user as owner
		String userId =  user.getId();
		// Escape any whitespace characters, because otherwise the search will fail!
		userId = userId.replaceAll(" ", "\\\\ ");
		SimpleField<String> ownerIdField = new SimpleField<String>(DccdSB.OWNER_ID_NAME, userId);
		request.addFilterQuery(ownerIdField);
		
		// restrict to specific sid
		SimpleField<String> idField = new SimpleField<String>(DccdProjectSB.PID_NAME, id);
		request.addFilterQuery(idField);

		try {
			searchResults = DccdSearchService.getService().doSearch(request);
			if (searchResults.getHits().isEmpty()) {
				return Response.status(Status.NOT_FOUND).build();
			} else {
				DccdSB dccdSB = searchResults.getHits().get(0).getData();
				//return Response.status(Status.OK).entity(getProjectSearchResultAsXml(dccdSB)).build();
				return responseXmlOrJson(getProjectSearchResultAsXml(dccdSB, user));
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
	private String getProjectSearchResultAsXml(DccdSB dccdSB, DccdUser user) {
		java.io.StringWriter sw = new StringWriter();
		
		sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); // XML instruction
		sw.append("<project>");
		appendSearchResultDataAsXml(sw, dccdSB, user);
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
	protected void appendSearchResultDataAsXml(java.io.StringWriter sw, DccdSB dccdSB, DccdUser user) {
		appendProjectPublicDataAsXml(sw, dccdSB);
		
		// status is interesting for MyProjects
		sw.append(getXMLElementString("state", dccdSB.getAdministrativeState()));
		
		// permission
		appendProjectPermissionAsXml(sw, dccdSB);
		
		// always show location; it's our own data!
		appendProjectLocationAsXml(sw, dccdSB);
		// and timerange as well
		appendProjectTimeRangeAsXml(sw, dccdSB);
		
		appendProjectDescriptionAsXml(sw, dccdSB);
		appendProjectTaxonsAsXml(sw, dccdSB);
		appendProjectObjectTypesAsXml(sw, dccdSB);
		appendProjectElementTypesAsXml(sw, dccdSB);
	}
	
	
	// Note maybe add a way of getting the extensive permission data/metadata, 
	// we won't probably need it always so maybe put it in a '/myproject/{sid}/permission' resource
	
	// Note maybe support 'endpoints, for draft and published, /myproject/state/draft then only lists the drafts
	
	/**
	 * Produce a paged result list of project information
	 * 
	 * @param q
	 * @param projectCategory
	 * @param objectType
	 * @param elementTaxon
	 * @param offset
	 * @param limit
	 * @return
	 */
	@GET
	@Path("/query")
	public Response getProjectsByQuery(
			   @QueryParam(PLAIN_TEXT_QUERY_PARAM) @DefaultValue("") String q,
			   @QueryParam(CATEGORY_QUERY_PARAM) @DefaultValue("") String projectCategory,
			   @QueryParam(OBJECT_TYPE_QUERY_PARAM) @DefaultValue("") String objectType,
			   @QueryParam(ELEMENT_TAXON_QUERY_PARAM) @DefaultValue("") String elementTaxon,
	           @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
	           @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit) {
		
// NOTE maybe get rid of object & element stuff, because we don't return it?
		
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
		
		SearchResult<? extends DccdSB> searchResults = null;
		
		SimpleSearchRequest request = new SimpleSearchRequest(q); // text query
		request.setLimit(limit)	;
		request.setOffset(offset);
		// Show Project and not the standard Object result
		request.addFilterBean(DccdProjectSB.class);
		//request.addSortField(new SimpleSortField(DccdProjectSB.PID_NAME, SortOrder.ASC));
	
		// Make sure it is published and not draft!
		//SimpleField<String> stateField = new SimpleField<String>(DccdProjectSB.ADMINISTRATIVE_STATE_NAME, 
		//		DatasetState.PUBLISHED.toString());
		//request.addFilterQuery(stateField);
		
		// make sure its of the owner
    	// restrict results to the current user as owner
		String userId =  user.getId();
		// Escape any whitespace characters, because otherwise the search will fail!
		userId = userId.replaceAll(" ", "\\\\ ");
		SimpleField<String> ownerIdField = new SimpleField<String>(DccdSB.OWNER_ID_NAME, userId);
		request.addFilterQuery(ownerIdField);
		
		// project.category
		if (!projectCategory.isEmpty()) {
			@SuppressWarnings("serial")
			CombinedOptionalField<String> projectCategoryField = new CombinedOptionalField<String>(new ArrayList<String>(){{
				add(DccdSB.TRIDAS_PROJECT_CATEGORY_NAME);
				add(DccdSB.TRIDAS_PROJECT_CATEGORY_NORMAL_NAME);
				add(DccdSB.TRIDAS_PROJECT_CATEGORY_NORMALTRIDAS_NAME);
			}});
			projectCategoryField.setValue(projectCategory);
			request.addFilterQuery(projectCategoryField);
		}
		
		// object.type
		if (!objectType.isEmpty()) {
			@SuppressWarnings("serial")
			CombinedOptionalField<String> objectTypeField = new CombinedOptionalField<String>(new ArrayList<String>(){{
				add(DccdSB.TRIDAS_OBJECT_TYPE_NAME);
				add(DccdSB.TRIDAS_OBJECT_TYPE_NORMAL_NAME);
			}});
			objectTypeField.setValue(objectType);
			request.addFilterQuery(objectTypeField);
		}
		
		// element.taxon
		if (!elementTaxon.isEmpty()) {
			SimpleField<String> elementTaxonField = new SimpleField<String>(DccdSB.TRIDAS_ELEMENT_TAXON_NAME);
			elementTaxonField.setValue(elementTaxon);
			request.addFilterQuery(elementTaxonField);
		}
		
		try {
			searchResults = DccdSearchService.getService().doSearch(request);
			return responseXmlOrJson(getProjectListSearchResultAsXml(searchResults, offset, limit, user));
		} catch (SearchServiceException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}
