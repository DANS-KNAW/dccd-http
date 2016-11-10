package nl.knaw.dans.dccd.rest;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.knaw.dans.common.lang.service.exceptions.ServiceException;
import nl.knaw.dans.dccd.application.services.DccdUserService;
import nl.knaw.dans.dccd.application.services.UserServiceException;
import nl.knaw.dans.dccd.model.DccdOrganisation;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.model.DccdUser.Role;
import nl.knaw.dans.dccd.rest.util.XmlStringUtil;

@Path("/organisation")
public class OrganisationResource extends AbstractResource {

	/**
	 * Get a list of organisations
	 * 
	 * @return
	 */
	@GET
	//@Path("/")
	public Response getOrganisations()
	{
		boolean requestByAdmin = isRequestByAdmin();
		
		try {
			List<DccdOrganisation> organisations = new ArrayList<DccdOrganisation>();
			if (requestByAdmin)
			{
				organisations = DccdUserService.getService().getAllOrganisations();
			}
			else
			{
				organisations = DccdUserService.getService().getActiveOrganisations();
			}
			
			// construct the response
			java.io.StringWriter sw = new StringWriter();
			
			sw.append(XmlStringUtil.XML_INSTRUCTION_STR);
			sw.append("<organisations>");
			for (DccdOrganisation organisation : organisations) {
				sw.append("<organisation>");
				sw.append(XmlStringUtil.getXMLElementString("id", organisation.getId()));
				sw.append(XmlStringUtil.getXMLElementStringOptional("city", organisation.getCity()));
				sw.append(XmlStringUtil.getXMLElementStringOptional("country", organisation.getCountry()));
				
				if (requestByAdmin) 
				{
					sw.append(XmlStringUtil.getXMLElementString("accountState", organisation.getState().toString())); //account state and not a location
				}
				sw.append("</organisation>");
			}
			sw.append("</organisations>");
			
			return responseXmlOrJson(sw.toString());
			
		} catch (UserServiceException e) {			
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Get detailed information of an organisation
	 * 
	 * @param oid
	 * 				The organisation id
	 * @return
	 */
	@GET
	@Path("/{oid:.+}/") // ':.+' is needed to match any slashes in the organisation id
	public Response getOrganisationByid(@PathParam("oid") String oid) {
		// authenticate requesting user
		DccdUser requestingUser = null;
		try {
			requestingUser = authenticate();
			if (requestingUser == null || !isAllowed(requestingUser, oid))
				return Response.status(Status.UNAUTHORIZED).build();
		} catch (ServiceException eAuth) {
			eAuth.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		try {
			DccdOrganisation organisation = DccdUserService.getService().getOrganisationById(oid);
			if (organisation != null) {
				// construct the response
				java.io.StringWriter sw = new StringWriter();
				
				sw.append(XmlStringUtil.XML_INSTRUCTION_STR);
				sw.append("<organisation>");
				sw.append(XmlStringUtil.getXMLElementString("id", organisation.getId()));
				sw.append(XmlStringUtil.getXMLElementStringOptional("city", organisation.getCity()));
				sw.append(XmlStringUtil.getXMLElementStringOptional("country", organisation.getCountry()));
				
				// more details
				sw.append(XmlStringUtil.getXMLElementStringOptional("address", organisation.getAddress()));
				sw.append(XmlStringUtil.getXMLElementStringOptional("postalcode", organisation.getPostalCode()));
				sw.append(XmlStringUtil.getXMLElementString("accountState", organisation.getState().toString())); //account state and not a location
				
				sw.append("</organisation>");
				
				return responseXmlOrJson(sw.toString());
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		} catch (UserServiceException e) {			
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	private boolean isAllowed(final DccdUser requestingUser, final String oid) {
		if (requestingUser.hasRole(Role.ADMIN) || requestingUser.getOrganization().equals(oid)) 
			return true; 
		else 
			return false;
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
}
