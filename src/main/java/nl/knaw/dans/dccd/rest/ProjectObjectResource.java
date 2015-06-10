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

import java.io.StringWriter;
import java.util.ArrayList;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.knaw.dans.common.lang.dataset.DatasetState;
import nl.knaw.dans.common.lang.search.SearchHit;
import nl.knaw.dans.common.lang.search.SearchRequest;
import nl.knaw.dans.common.lang.search.SearchResult;
import nl.knaw.dans.common.lang.search.simple.CombinedOptionalField;
import nl.knaw.dans.common.lang.search.simple.SimpleField;
import nl.knaw.dans.common.lang.search.simple.SimpleSearchRequest;
import nl.knaw.dans.common.lang.service.exceptions.ServiceException;
import nl.knaw.dans.common.lang.util.Range;
import nl.knaw.dans.dccd.application.services.DccdSearchService;
import nl.knaw.dans.dccd.application.services.SearchServiceException;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.search.DccdObjectSB;
import nl.knaw.dans.dccd.search.DccdProjectSB;
import nl.knaw.dans.dccd.search.DccdSB;

/**
 * TRiDaS Object is alway part of a Project, and the project information is also returned with a response
 * 
 * @author paulboon
 *
 */
@Path("/object")
public class ProjectObjectResource extends AbstractProjectResource {
	
	/**
	 * Produce a paged result list of object (and its parent project) information
	 *
	 * NOTE maybe place the requested query params in a <query> tag ?
	 * 
	 * @param q
	 * @param projectCategory
	 * @param objectType
	 * @param elementTaxon
	 * @param offset
	 * @param limit
	 * @return A response containing the paged list of results
	 */
	@GET
	@Path("/query")
	public Response getProjectsByQuery(
			   @QueryParam(PLAIN_TEXT_QUERY_PARAM) @DefaultValue("") String q,
			   @QueryParam(CATEGORY_QUERY_PARAM) @DefaultValue("") String projectCategory,
			   @QueryParam(LABNAME_QUERY_PARAM) @DefaultValue("") String projectLabname,
			   @QueryParam(OBJECT_TYPE_QUERY_PARAM) @DefaultValue("") String objectType,
			   @QueryParam(OBJECT_CREATOR_QUERY_PARAM) @DefaultValue("") String objectCreator,   
			   @QueryParam(ELEMENT_TAXON_QUERY_PARAM) @DefaultValue("") String elementTaxon,
			   @QueryParam(ELEMENT_TYPE_QUERY_PARAM) @DefaultValue("") String elementType,
			   @QueryParam(DEATH_YEAR_FROM_QUERY_PARAM) Integer deathYearFrom,
			   @QueryParam(DEATH_YEAR_TO_QUERY_PARAM) Integer deathYearTo,
			   @QueryParam(FIRST_YEAR_FROM_QUERY_PARAM) Integer firstYearFrom,
			   @QueryParam(FIRST_YEAR_TO_QUERY_PARAM) Integer firstYearTo,
			   @QueryParam(LAST_YEAR_FROM_QUERY_PARAM) Integer lastYearFrom,
			   @QueryParam(LAST_YEAR_TO_QUERY_PARAM) Integer lastYearTo,
			   @QueryParam(PITH_YEAR_FROM_QUERY_PARAM) Integer pithYearFrom,
			   @QueryParam(PITH_YEAR_TO_QUERY_PARAM) Integer pithYearTo,
			   @QueryParam(OFFSET_PARAM) @DefaultValue("0") int offset,
	           @QueryParam(LIMIT_PARAM) @DefaultValue("" + DEFAULT_LIST_LIMIT) int limit) {
		// Advanced Search need login on the GUI, so we want it here also
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
		
		// Show the standard Object result
		request.addFilterBean(DccdObjectSB.class);
		//request.addSortField(new SimpleSortField(DccdProjectSB.PID_NAME, SortOrder.ASC));
	
		// Make sure it is published and not draft!
		SimpleField<String> stateField = new SimpleField<String>(DccdProjectSB.ADMINISTRATIVE_STATE_NAME, 
				DatasetState.PUBLISHED.toString());
		request.addFilterQuery(stateField);

		// project fields
		addProjectCategory(projectCategory, request);
		addProjectLabname(projectLabname, request);
		// object fields
		addObjectType(objectType, request);
		addObjectCreator(objectCreator, request);
		// element fields
		addElementTaxon(elementTaxon, request);
		addElementType(elementType, request);

		// TODO add title and identifier for project, object and element

		// year range queries
		addDeathYearRange(deathYearFrom, deathYearTo, request);
		addFirstYearRange(firstYearFrom, firstYearTo, request);
		addLastYearRange(lastYearFrom, lastYearTo, request);
		addPithYearRange(pithYearFrom, pithYearTo, request);
		
		try {
			searchResults = DccdSearchService.getService().doSearch(request);
			return responseXmlOrJson(getProjectListSearchResultAsXml(searchResults, offset, limit));
		} catch (SearchServiceException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/* 
	 * NOTE could refactor the nl.knaw.dans.dccd.web.search.years.YearRange 
	 * and put it in the dccd-lib instaedv of the dccd webapp
	 */
	private void addYearRange(final String name, final Integer yearFrom, final Integer yearTo, SearchRequest request) 
	{
		if (yearFrom != null || yearTo != null)
		{
			SimpleField<Range<Integer>> yearField = 
					new SimpleField<Range<Integer>>(name);
			yearField.setValue(new Range<Integer>(yearFrom, yearTo));
			request.addFilterQuery(yearField);
		}		
	}
	
	private void addDeathYearRange(final Integer yearFrom, final Integer yearTo, SearchRequest request) 
	{
		addYearRange(DccdSB.TRIDAS_MEASUREMENTSERIES_INTERPRETATION_DEATHYEAR_NAME, yearFrom, yearTo, request);
	}

	private void addFirstYearRange(final Integer yearFrom, final Integer yearTo, SearchRequest request) 
	{
		addYearRange(DccdSB.TRIDAS_MEASUREMENTSERIES_INTERPRETATION_FIRSTYEAR_NAME, yearFrom, yearTo, request);
	}
	
	private void addLastYearRange(final Integer yearFrom, final Integer yearTo, SearchRequest request) 
	{
		addYearRange(DccdSB.TRIDAS_MEASUREMENTSERIES_INTERPRETATION_LASTYEAR_NAME, yearFrom, yearTo, request);
	}

	private void addPithYearRange(final Integer yearFrom, final Integer yearTo, SearchRequest request) 
	{
		addYearRange(DccdSB.TRIDAS_MEASUREMENTSERIES_INTERPRETATION_PITHYEAR_NAME, yearFrom, yearTo, request);
	}

	private void addProjectCategory(final String projectCategory, SearchRequest request) 
	{
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
	}
	
	private void addProjectLabname(final String projectLabname, SearchRequest request) 
	{
		if(!projectLabname.isEmpty())
		{
			SimpleField<String> projectLabnameField = new SimpleField<String>(DccdSB.TRIDAS_PROJECT_LABORATORY_NAME_NAME);
			projectLabnameField.setValue(projectLabname);
			request.addFilterQuery(projectLabnameField);
		}
	}
	
	private void addObjectType(final String objectType, SearchRequest request) 
	{
		if (!objectType.isEmpty()) {
			@SuppressWarnings("serial")
			CombinedOptionalField<String> objectTypeField = new CombinedOptionalField<String>(new ArrayList<String>(){{
				add(DccdSB.TRIDAS_OBJECT_TYPE_NAME);
				add(DccdSB.TRIDAS_OBJECT_TYPE_NORMAL_NAME);
			}});
			objectTypeField.setValue(objectType);
			request.addFilterQuery(objectTypeField);
		}		
	}
	
	private void addObjectCreator(final String objectCreator, SearchRequest request) 
	{
		if(!objectCreator.isEmpty())
		{
			SimpleField<String> objectCreatorField = new SimpleField<String>(DccdSB.TRIDAS_OBJECT_CREATOR_NAME);
			objectCreatorField.setValue(objectCreator);
			request.addFilterQuery(objectCreatorField);
		}		
	}
	
	private void addElementTaxon(final String elementTaxon, SearchRequest request) 
	{
		if (!elementTaxon.isEmpty()) {
			SimpleField<String> elementTaxonField = new SimpleField<String>(DccdSB.TRIDAS_ELEMENT_TAXON_NAME);
			elementTaxonField.setValue(elementTaxon);
			request.addFilterQuery(elementTaxonField);
		}		
	}
	
	private void addElementType(final String elementType, SearchRequest request) 
	{
		if (!elementType.isEmpty()) {
			@SuppressWarnings("serial")
			CombinedOptionalField<String> elementTypeField = new CombinedOptionalField<String>(new ArrayList<String>(){{
				add(DccdSB.TRIDAS_ELEMENT_TYPE_NAME);
				add(DccdSB.TRIDAS_ELEMENT_TYPE_NORMAL_NAME);
			}});
			elementTypeField.setValue(elementType);
			request.addFilterQuery(elementTypeField);
		}		
	}
	
	
	protected String getObjectListSearchResultAsXml(SearchResult<? extends DccdSB> searchResults, 
			int offset, int limit) {
		java.io.StringWriter sw = new StringWriter();
		
		sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); // XML instruction
		sw.append("<objects" + 
				" total=\"" + searchResults.getTotalHits() + "\"" +
				" offset=\"" + offset + "\"" +
				" limit=\"" + limit + "\"" +
				">");

		
		for (SearchHit<? extends DccdSB> hit: searchResults.getHits()) {
			sw.append("<object>");
			appendSearchResultDataAsXml(sw, hit.getData());
			sw.append("</object>");
		}
		sw.append("</objects>");

		return sw.toString();
	}
	
	/**
	 * Append object (and its parent project) XML
	 * 
	 * @param sw
	 *            writer to append to
	 * @param dccdSB
	 *            search result
	 */
	@Override
	protected void appendSearchResultDataAsXml(StringWriter sw, DccdSB dccdSB) {
		// store id for the TRiDaS Object datastream
		sw.append(getXMLElementString("sid", dccdSB.getId()));
		sw.append(getXMLElementString("title", getObjectTitleString(dccdSB)));
		sw.append(getXMLElementString("identifier", getObjectIdentifierString(dccdSB)));		
		
		// Now also add the 'Project' information
		// Note that you can only retrieve projects with the API
		sw.append("<project>");
		appendProjectPublicDataAsXml(sw, dccdSB);
		
		appendProjectPublicLocationAsXml(sw, dccdSB);	
		appendProjectPublicTimeRangeAsXml(sw, dccdSB);
		
		appendProjectPublicTaxonsAsXml(sw, dccdSB);
		appendProjectPublicTypesAsXml(sw, dccdSB);

		appendProjectPublicDescriptionAsXml(sw, dccdSB);
		
		// permission
		appendProjectPermissionAsXml(sw, dccdSB);

		sw.append("</project>");
	}
	
	private static String getObjectTitleString(final DccdSB dccdHit)
	{
		String objectTitleStr = "";

		// just the first one, the Object SB should have only one anyway
		if (dccdHit.hasTridasObjectTitle() && dccdHit.getTridasObjectTitle().get(0).length() > 0)
			objectTitleStr = dccdHit.getTridasObjectTitle().get(0);
		
		return objectTitleStr;
	}

	private static String getObjectIdentifierString(final DccdSB dccdHit)
	{
		String objectIdentifierStr = "";

		// just the first one, the Object SB should have only one anyway
		if (dccdHit.hasTridasObjectIdentifier() && dccdHit.getTridasObjectIdentifier().get(0).length() > 0)
			objectIdentifierStr = dccdHit.getTridasObjectIdentifier().get(0);
		
		return objectIdentifierStr;
	}
}
