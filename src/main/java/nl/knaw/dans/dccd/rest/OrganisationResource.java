package nl.knaw.dans.dccd.rest;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;

import nl.knaw.dans.dccd.application.services.DccdUserService;
import nl.knaw.dans.dccd.application.services.UserServiceException;
import nl.knaw.dans.dccd.model.DccdOrganisation;

@Path("/organisation")
public class OrganisationResource extends AbstractResource {

	/**
	 * Get a full list of all organisations, but only the public data
	 * 
	 * @return
	 */
	@GET
	@Path("/")
	public Response getOrganisations()
	{
		// OK, now get the organisations from the UserRepo...
		try {
			List<DccdOrganisation> organisations = retrieveOrganisations();
			
			// construct the response
			java.io.StringWriter sw = new StringWriter();
			
			sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); // XML instruction
			sw.append("<organisations>");
			for (DccdOrganisation organisation : organisations) {
				sw.append("<organisation>");
				sw.append("<id>" + StringEscapeUtils.escapeXml(organisation.getId()) + "</id>");
				String city = organisation.getCity();
				if(city != null && !city.trim().isEmpty()) 
				{
					sw.append("<city>" + StringEscapeUtils.escapeXml(city) + "</city>");
				}
				String country = organisation.getCountry();
				if (country != null && !country.trim().isEmpty()) 
				{
					sw.append("<country>" + StringEscapeUtils.escapeXml(country) + "</country>");
				}
				sw.append("</organisation>");
			}
			sw.append("</organisations>");
			
			return responseXmlOrJson(sw.toString());
			
		} catch (UserServiceException e) {			
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	private List<DccdOrganisation> retrieveOrganisations() throws UserServiceException {
		List<DccdOrganisation> organisations = new ArrayList<DccdOrganisation>();

		//if (isAdmin())
		//{
		//	organisations = DccdUserService.getService().getAllOrganisations();
		//}
		//else
		//{
			organisations = DccdUserService.getService().getActiveOrganisations();
		//}
		return organisations;
	}
}
