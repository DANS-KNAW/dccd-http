package nl.knaw.dans.dccd.rest;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;

import nl.knaw.dans.common.lang.service.exceptions.ServiceException;
import nl.knaw.dans.dccd.application.services.DccdUserService;
import nl.knaw.dans.dccd.application.services.UserServiceException;
import nl.knaw.dans.dccd.model.DccdOrganisation;
import nl.knaw.dans.dccd.model.DccdUser;

@Path("/user")
public class UserResource extends AbstractResource {

	/**
	 * Get a full list of all users (members)
	 * 
	 * @return
	 */
	@GET
	@Path("/")
	public Response getUsers()
	{
		// authenticate user
		DccdUser requestingUser = null;
		try {
			requestingUser = authenticate();
			if (requestingUser == null)
				return Response.status(Status.UNAUTHORIZED).build();
		} catch (ServiceException eAuth) {
			eAuth.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		// OK, now get the organisations from the UserRepo...
		try {
			List<DccdUser> users = retrieveUsers();
			
			// construct the response
			java.io.StringWriter sw = new StringWriter();
			
			sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); // XML instruction
			sw.append("<users>");
			for (DccdUser user : users) {
				sw.append("<user>");
				sw.append("<id>" + StringEscapeUtils.escapeXml(user.getId()) + "</id>");
				sw.append("<displayname>" + StringEscapeUtils.escapeXml(user.getDisplayName()) + "</displayname>");
				sw.append("<lastname>" + StringEscapeUtils.escapeXml(user.getSurname()) + "</lastname>");				
				sw.append("<email>" + user.getEmail() + "</email>"); // no escape needed
				sw.append("<organisation>" + StringEscapeUtils.escapeXml(user.getOrganization()) + "</organisation>");
				
				// optional values below...
				//Title	
				//Initials
				//Prefixes	
				//Function
				//Telephone	
				//DAI	
				
				//String country = user.getCountry();
				//if (country != null && !country.trim().isEmpty()) 
				//{
				//	sw.append("<country>" + country + "</country>");
				//}
				sw.append("</user>");
			}
			sw.append("</users>");
			
			return responseXmlOrJson(sw.toString());
			
		} catch (UserServiceException e) {			
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	private List<DccdUser> retrieveUsers() throws UserServiceException {
		List<DccdUser> users = new ArrayList<DccdUser>();

		//if (isAdmin())
		//{
		//	users = DccdUserService.getService().getAllUsers();
		//}
		//else
		//{
		users = DccdUserService.getService().getActiveNormalUsers();//.getActiveOrganisations();
		//}
		return users;
	}
}
