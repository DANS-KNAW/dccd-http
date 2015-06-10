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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import nl.knaw.dans.common.lang.search.SearchHit;
import nl.knaw.dans.common.lang.search.SearchResult;
import nl.knaw.dans.dccd.model.ProjectPermissionLevel;
import nl.knaw.dans.dccd.search.DccdSB;
import nl.knaw.dans.dccd.util.StringUtil;

import nl.knaw.dans.common.lang.service.exceptions.ServiceException;
import nl.knaw.dans.dccd.application.services.DataServiceException;
import nl.knaw.dans.dccd.application.services.DccdDataService;
import nl.knaw.dans.dccd.application.services.DccdUserService;
import nl.knaw.dans.dccd.application.services.UserServiceException;
import nl.knaw.dans.dccd.model.DccdOrganisation;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.model.Project;

/**
 * 
 * @author paulboon
 * 
 */
public abstract class AbstractProjectResource extends AbstractResource {

	/**
	 * Query arguments.
	 */
	public static final String PLAIN_TEXT_QUERY_PARAM = "q";
	public static final String CATEGORY_QUERY_PARAM = "category";
	public static final String LABNAME_QUERY_PARAM = "labname";
	public static final String OBJECT_TYPE_QUERY_PARAM = "object.type";
	public static final String OBJECT_CREATOR_QUERY_PARAM = "object.creator";
	public static final String ELEMENT_TAXON_QUERY_PARAM = "element.taxon";
	public static final String ELEMENT_TYPE_QUERY_PARAM = "element.type";
	public static final String DEATH_YEAR_FROM_QUERY_PARAM = "deathYearFrom";
	public static final String DEATH_YEAR_TO_QUERY_PARAM = "deathYearTo";
	public static final String FIRST_YEAR_FROM_QUERY_PARAM = "firstYearFrom";
	public static final String FIRST_YEAR_TO_QUERY_PARAM = "firstYearTo";
	public static final String LAST_YEAR_FROM_QUERY_PARAM = "lastYearFrom";
	public static final String LAST_YEAR_TO_QUERY_PARAM = "lastYearTo";
	public static final String PITH_YEAR_FROM_QUERY_PARAM = "pithYearFrom";
	public static final String PITH_YEAR_TO_QUERY_PARAM = "pithYearTo";

	/**
	 * Construct search result list information as XML String
	 * 
	 * @param searchResults
	 *            The results
	 * @param offset
	 *            Number of results to skip
	 * @param limit
	 *            Number of results in this list
	 * @return The XML String
	 */
	protected String getProjectListSearchResultAsXml(
			SearchResult<? extends DccdSB> searchResults, int offset, int limit) {
		java.io.StringWriter sw = new StringWriter();

		sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); // XML
																					// instruction
		sw.append("<projects" + " total=\"" + searchResults.getTotalHits()
				+ "\"" + " offset=\"" + offset + "\"" + " limit=\"" + limit
				+ "\"" + ">");

		for (SearchHit<? extends DccdSB> hit : searchResults.getHits()) {
			sw.append("<project>");
			appendSearchResultDataAsXml(sw, hit.getData());
			sw.append("</project>");
		}
		sw.append("</projects>");

		return sw.toString();
	}

	/**
	 * Append search result information as XML String
	 * 
	 * @param sw
	 *            writer to append to
	 * @param dccdSB
	 *            search result
	 */
	protected abstract void appendSearchResultDataAsXml(
			java.io.StringWriter sw, DccdSB dccdSB);

	// TODO strings need to be escaped for xml, maybe use a lib for constructing
	// xml

	/**
	 * Append information anyone is allowed to see
	 * The most important project data but not identical to TRiDaS!
	 * 
	 * @param sw
	 *            writer to append to
	 * @param dccdSB
	 *            search result
	 */
	protected void appendProjectPublicDataAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		// Note the Fedora pid is our sid, but sometimes called pid anyway;
		// confusing I know
		sw.append(getXMLElementString("sid", dccdSB.getPid()));

		// modified timestamp
		// convert to UTC and format as ISO
		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		DateTime dUtc = dccdSB.getAdministrativeStateLastChange().toDateTime(DateTimeZone.UTC);
		//sw.append(getXMLElementString("stateChanged", dccdSB.getAdministrativeStateLastChange().toString()));
		sw.append(getXMLElementString("stateChanged", fmt.print(dUtc)));		
		
		// Not at first only added title, so a client can show something in a
		// user interface,
		// but now we put in (almost) everything from the search results.

		// title
		sw.append(getXMLElementString("title", dccdSB.getTridasProjectTitle()));

		// identifier
		sw.append(getXMLElementString("identifier", dccdSB.getTridasProjectIdentifier()));

		// category, but not std, normal etc.
		sw.append(getXMLElementString("category", dccdSB.getTridasProjectCategory()));
		
		// investigator
		sw.append(getXMLElementString("investigator", dccdSB.getTridasProjectInvestigator()));
		
		// lab(s) (combined name, address, but not concatenated...)
		sw.append("<laboratories>");
		for (String lab : dccdSB.getTridasProjectLaboratoryCombined()) {
			sw.append(getXMLElementString("laboratory", lab));
		}
		sw.append("</laboratories>");
		
		// type(s)
		sw.append("<types>");
		for (String type : dccdSB.getTridasProjectType()) {
			sw.append(getXMLElementString("type", type));
		}
		sw.append("</types>");
		
		// Note that this goes to another service and is a Performance Penalty
		sw.append(getXMLElementString("ownerOrganizationId", getOwnerOrganizationId(dccdSB)));
		// And this one goes to the data archive... a penalty...
		sw.append(getXMLElementString("language", getProjectlanguage(dccdSB)));
	}

	/**
	 * Append location XML, but only when allowed
	 * 
	 * @param sw
	 *            writer to append to
	 * @param dccdSB
	 *            search result
	 */
	protected void appendProjectPublicLocationAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		// NOTE also give location if object level is open to everyone (even
		// when not logged in)!
		ProjectPermissionLevel effectivelevel = ProjectPermissionLevel
				.valueOf(dccdSB.getPermissionDefaultLevel());
		Boolean isAllowedToViewLocation = ProjectPermissionLevel.OBJECT
				.isPermittedBy(effectivelevel);
		if (isAllowedToViewLocation) {
			appendProjectLocationAsXml(sw, dccdSB);
		}
	}

	@SuppressWarnings({ "serial" })
	public static final Map<ProjectPermissionLevel, String> MAP_PERMISSION_TO_ENTITYLEVEL = 
		    Collections.unmodifiableMap(new HashMap<ProjectPermissionLevel, String>() {{ 
		    	put(ProjectPermissionLevel.MINIMAL, "minimal");
		        put(ProjectPermissionLevel.PROJECT, "project");
		        put(ProjectPermissionLevel.OBJECT, "object");
		        put(ProjectPermissionLevel.ELEMENT, "element");
		        put(ProjectPermissionLevel.SAMPLE, "sample");
		        put(ProjectPermissionLevel.RADIUS, "radius");
		        put(ProjectPermissionLevel.SERIES, "series");
		        put(ProjectPermissionLevel.VALUES, "values");
		    }});

	/**
	 * Append the permission related information of the project
	 * 
	 * @param sw
	 *            writer to append to
	 * @param dccdSB
	 *            search result
	 */
	protected void appendProjectPermissionAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		// only the default level
		sw.append("<permission>");
		sw.append(getXMLElementString("defaultLevel", 
				//dccdSB.getPermissionDefaultLevel()));
				MAP_PERMISSION_TO_ENTITYLEVEL.get(ProjectPermissionLevel.valueOf(dccdSB.getPermissionDefaultLevel())))); 
		sw.append("</permission>");
	}
	
	/**
	 * Append location XML
	 * 
	 * @param sw
	 *            writer to append to
	 * @param dccdSB
	 *            search result
	 */
	protected void appendProjectLocationAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		if (dccdSB.hasLatLng()) 
		{
			// just append it, no WGS84 or EPSG indications, it's implicit
			sw.append("<location>");
			sw.append(getXMLElementString("lat", dccdSB.getLat().toString()));
			sw.append(getXMLElementString("lng", dccdSB.getLng().toString()));
			sw.append("</location>");
		}
	}
	
	/**
	 * Append Taxon's, but only when allowed
	 * 
	 * @param sw
	 * @param dccdSB
	 */
	protected void appendProjectPublicTaxonsAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		ProjectPermissionLevel effectivelevel = ProjectPermissionLevel
				.valueOf(dccdSB.getPermissionDefaultLevel());
		Boolean isAllowedToViewTaxon = ProjectPermissionLevel.ELEMENT
				.isPermittedBy(effectivelevel);
		if (isAllowedToViewTaxon) {
			appendProjectTaxonsAsXml(sw, dccdSB);
		}
	}
	
	protected void appendProjectTaxonsAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		if (dccdSB.hasTridasElementTaxon())
		{
			// avoid duplicates
			List<String> taxons = StringUtil.getUniqueStrings(dccdSB.getTridasElementTaxon());
			
			sw.append("<taxons>");
			for(String taxon : taxons)
			{
				sw.append(getXMLElementString("taxon", taxon));
			}
			sw.append("</taxons>");
		}
	}
	
	/**
	 * Append Object and elements Types, but only when allowed
	 * 
	 * @param sw
	 * @param dccdSB
	 */
	protected void appendProjectPublicTypesAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		ProjectPermissionLevel effectivelevel = ProjectPermissionLevel
				.valueOf(dccdSB.getPermissionDefaultLevel());
		Boolean isAllowedToViewType = ProjectPermissionLevel.ELEMENT
				.isPermittedBy(effectivelevel);
		if (isAllowedToViewType) {
			appendProjectElementTypesAsXml(sw, dccdSB);
			appendProjectObjectTypesAsXml(sw, dccdSB);
		} else {
			// maybe only object types
			Boolean isAllowedToViewObjectType = ProjectPermissionLevel.OBJECT
					.isPermittedBy(effectivelevel);
			if (isAllowedToViewObjectType) {
				appendProjectObjectTypesAsXml(sw, dccdSB);
			}
		}
	}
	
	protected void appendProjectElementTypesAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		if (dccdSB.hasTridasElementType())
		{
			// avoid duplicates
			List<String> types = StringUtil.getUniqueStrings(dccdSB.getTridasElementType());

			sw.append("<elementTypes>");
			for(String type : types)
			{
				sw.append(getXMLElementString("elementType", type));
			}
			sw.append("</elementTypes>");
		}
		// Note: what to do with normal and normalId ?
	}
	
	protected void appendProjectObjectTypesAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		if (dccdSB.hasTridasObjectType())
		{
			// avoid duplicates
			List<String> types = StringUtil.getUniqueStrings(dccdSB.getTridasObjectType());

			sw.append("<objectTypes>");
			for(String type : types)
			{
				sw.append(getXMLElementString("objectType", type));
			}
			sw.append("</objectTypes>");
		}
		// Note: what to do with normal and normalId ?
	}
	
	/**
	 * Append project description XML, but only when allowed
	 * 
	 * @param sw
	 *            writer to append to
	 * @param dccdSB
	 *            search result
	 */
	protected void appendProjectPublicDescriptionAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		// NOTE also give description if project level is open to everyone (even
		// when not logged in)!
		// Also note that we don't do object descriptions...
		ProjectPermissionLevel effectivelevel = ProjectPermissionLevel
				.valueOf(dccdSB.getPermissionDefaultLevel());
		Boolean isAllowedToViewDescription = ProjectPermissionLevel.PROJECT
				.isPermittedBy(effectivelevel);
		if (isAllowedToViewDescription) {
			appendProjectDescriptionAsXml(sw, dccdSB);
		}
	}
	
	protected void appendProjectDescriptionAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {	
		if (dccdSB.hasTridasProjectDescription())
			sw.append(getXMLElementString("description", dccdSB.getTridasProjectDescription()));
	}
	
	/**
	 * Append time range, but only when allowed
	 * 
	 * @param sw
	 * @param dccdSB
	 */
	protected void appendProjectPublicTimeRangeAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		ProjectPermissionLevel effectivelevel = ProjectPermissionLevel
				.valueOf(dccdSB.getPermissionDefaultLevel());
		Boolean isAllowedToViewTimeRange = ProjectPermissionLevel.SERIES
				.isPermittedBy(effectivelevel);
		if (isAllowedToViewTimeRange) {
			appendProjectTimeRangeAsXml(sw, dccdSB);
		}
	}
	
	/**
	 * TimeRange (or Temporal Coverage)
	 * 
	 * @param sw
	 * @param dccdSB
	 */
	protected void appendProjectTimeRangeAsXml(java.io.StringWriter sw,
			DccdSB dccdSB) {
		
		// concat all the lists, but only non null elements
		List<Integer> years = new ArrayList<Integer>();
		List<Integer> yearsFromTridas = dccdSB.getTridasMeasurementseriesInterpretationPithyear();
		if (yearsFromTridas != null) 
		{
			for(Integer year : yearsFromTridas)
			{
				if (year != null) years.add(year);
			}
		}
		yearsFromTridas = dccdSB.getTridasMeasurementseriesInterpretationFirstyear();
		if (yearsFromTridas != null) 
		{
			for(Integer year : yearsFromTridas)
			{
				if (year != null) years.add(year);
			}
		}
		yearsFromTridas = dccdSB.getTridasMeasurementseriesInterpretationLastyear();
		if (yearsFromTridas != null) 
		{
			for(Integer year : yearsFromTridas)
			{
				if (year != null) years.add(year);
			}
		}
		yearsFromTridas = dccdSB.getTridasMeasurementseriesInterpretationDeathyear();
		if (yearsFromTridas != null) 
		{
			for(Integer year : yearsFromTridas)
			{
				if (year != null) years.add(year);
			}
		}
		
		if (!years.isEmpty())
		{
			// we have at least one year (and it is not null)
			Integer min = years.get(0);
			Integer max = min;
			
			for(int i=1; i < years.size(); i++)
			{
				if (years.get(i) < min) min = years.get(i);
				if (years.get(i) > max) max = years.get(i);
			}
			
			// firstDate = min
			// lastDate = max
			sw.append("<timeRange>");
			sw.append(getXMLElementString("firstYear", min.toString()));
			sw.append(getXMLElementString("lastYear", max.toString()));
			sw.append("</timeRange>");
		}
	}
	
	public static String getXMLElementString(final String name, final String value)
	{
		// NOTE the name is not escaped
		return "<" + name + ">" + StringEscapeUtils.escapeXml(value) + "</" + name + ">";
	}
	
	/**
	 * get the user information from the user service to obtain the organisation id
	 * 
	 * TODO have this id indexed in the dccdSB !
	 * 
	 * @param dccdSB
	 * @return
	 */
	String getOwnerOrganizationId(DccdSB dccdSB) {
		String id = "";
		try {
			DccdUser user = DccdUserService.getService().getUserById(dccdSB.getOwnerId());
			id = user.getOrganization();
		} catch (UserServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return id;
	}
	
	String getProjectlanguage(DccdSB dccdSB)
	{
		String lang = "";
		
		// Aye, get the project ....
		
		try {
			Project p = DccdDataService.getService().getProject(dccdSB.getPid());
			lang = p.getTridasLanguage().getLanguage();
		} catch (DataServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return lang;
	}
}
