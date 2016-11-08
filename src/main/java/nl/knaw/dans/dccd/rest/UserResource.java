package nl.knaw.dans.dccd.rest;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import nl.knaw.dans.common.lang.service.exceptions.ServiceException;
import nl.knaw.dans.dccd.application.services.DccdUserService;
import nl.knaw.dans.dccd.application.services.UserServiceException;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.model.DccdUser.Role;

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

		// OK, now get the users from the UserRepo...
		try {
			List<DccdUser> users = retrieveUsers(requestingUser);
			
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
				if (requestingUser.hasRole(Role.ADMIN))
				{
					// administrative
					sw.append(getXMLElementString("accountState", user.getState().toString())); //account state and not a location
					if (!user.getRoles().isEmpty()) {
						sw.append("<roles>");
						for (Role role : user.getRoles()) {
							sw.append(getXMLElementString("role", role.toString()));
						}
						sw.append("</roles>");
					}
				}
				sw.append("</user>");
			}
			sw.append("</users>");
			
			return responseXmlOrJson(sw.toString());
			
		} catch (UserServiceException e) {			
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Path("/{uid}/")
	public Response getUserByid(@PathParam("uid") String uid) {
		// authenticate requesting user
		DccdUser requestingUser = null;
		try {
			requestingUser = authenticate();
			if (requestingUser == null || !isAllowed(requestingUser, uid))
				return Response.status(Status.UNAUTHORIZED).build();
		} catch (ServiceException eAuth) {
			eAuth.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		try {
			DccdUser user = DccdUserService.getService().getUserById(uid);
			
			if (user != null) {
				// construct the response
				java.io.StringWriter sw = new StringWriter();
				
				sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"); // XML instruction
				sw.append("<user>");
				
				sw.append("<id>" + StringEscapeUtils.escapeXml(user.getId()) + "</id>");
				sw.append("<displayname>" + StringEscapeUtils.escapeXml(user.getDisplayName()) + "</displayname>");
				sw.append("<lastname>" + StringEscapeUtils.escapeXml(user.getSurname()) + "</lastname>");				
				sw.append("<email>" + user.getEmail() + "</email>"); // no escape needed
				sw.append("<organisation>" + StringEscapeUtils.escapeXml(user.getOrganization()) + "</organisation>");
				
				// restricted more detailed personal information 
				sw.append(getXMLElementStringOptional("title", user.getTitle()));
				sw.append(getXMLElementStringOptional("initials", user.getInitials()));// actually required on registration
				sw.append(getXMLElementStringOptional("prefixes", user.getPrefixes()));
				sw.append(getXMLElementStringOptional("function", user.getFunction()));
				sw.append(getXMLElementStringOptional("telephone", user.getTelephone()));
				sw.append(getXMLElementStringOptional("dai", user.getDigitalAuthorIdentifier()));
				
				// administrative
				sw.append(getXMLElementString("lastLoginDate", getDateTimeFormattedAsString(user.getLastLoginDate())));		
				sw.append(getXMLElementString("accountState", user.getState().toString())); //account state and not a location
				sw.append(getXMLUserRolesStringOptional(user));

				sw.append("</user>");
				return responseXmlOrJson(sw.toString());
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		} catch (UserServiceException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	private String getXMLUserRolesStringOptional(final DccdUser user)
	{
		java.io.StringWriter sw = new StringWriter();
		if (!user.getRoles().isEmpty()) {
			sw.append("<roles>");
			for (Role role : user.getRoles()) {
				sw.append(getXMLElementString("role", role.toString()));
			}
			sw.append("</roles>");
		}
		return sw.toString();
	}
	
	private String getDateTimeFormattedAsString(final DateTime d) {
		// convert to UTC and format as ISO
		DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
		DateTime dUtc = d.toDateTime(DateTimeZone.UTC);
		return fmt.print(dUtc);
	}
	
	private String getXMLElementStringOptional(final String tagname, final String text)
	{
		if (text != null && !text.trim().isEmpty())
			return getXMLElementString(tagname, text);
		else
			return "";
	}

	private String getXMLElementString(final String tagname, final String text)
	{
		return "<"+tagname+">" + StringEscapeUtils.escapeXml(text) + "</"+tagname+">";
	}
	
	private List<DccdUser> retrieveUsers(final DccdUser requestingUser) throws UserServiceException {
		List<DccdUser> users = new ArrayList<DccdUser>();

		if (requestingUser.hasRole(Role.ADMIN))
		{
			users = DccdUserService.getService().getAllUsers();
		}
		else
		{
			users = DccdUserService.getService().getActiveNormalUsers();
		}
		return users;
	}
	
	private boolean isAllowed(final DccdUser requestingUser, final String uid) {
		if (requestingUser.hasRole(Role.ADMIN) || requestingUser.getId().equals(uid)) 
			return true; 
		else 
			return false;
	}
}
