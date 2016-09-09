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
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.Status;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import nl.knaw.dans.common.lang.service.exceptions.ServiceException;
import nl.knaw.dans.dccd.application.services.DccdUserService;
import nl.knaw.dans.dccd.authn.UsernamePasswordAuthentication;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.rest.util.XmlToJsonConverter;

import com.sun.jersey.core.util.Base64;

public abstract class AbstractResource {
	public static final int DEFAULT_LIST_LIMIT = 10;

	private String defaultMediaType = MediaType.APPLICATION_XML; // should be
																	// XML or
																	// JSON

	public String getDefaultMediaType() {
		return defaultMediaType;
	}

	public boolean hasDefaultMediaType() {
		return defaultMediaType != null && !defaultMediaType.isEmpty();
	}

	/**
	 * Query arguments.
	 */
	public static final String LIMIT_PARAM = "limit";
	public static final String OFFSET_PARAM = "offset";
	public static final String MOD_FROM = "modFrom";
	public static final String MOD_TO = "modTo";

	/**
	 * With each request the headers of that request are injected into the
	 * requestHeaders parameter.
	 */
	@Context
	private HttpHeaders requestHeaders;

	/**
	 * With each request URI info is injected into the uriInfo parameter.
	 */
	@Context
	private UriInfo uriInfo;

	/**
	 * Getter for the request headers.
	 * 
	 * @return The request headers.
	 */
	protected HttpHeaders getRequestHeaders() {
		return requestHeaders;
	}

    /**
     * Setter for the request headers. This is practical for testing in
     * particular.
     * 
     * @param requestHeaders
     *            The new request headers.
     */
    protected void setRequestHeaders(HttpHeaders requestHeaders)
    {
        this.requestHeaders = requestHeaders;
    }
    
	/**
	 * Checks whether the requested Media Type indicates XML.
	 * 
	 * @return True if the requested Media Type is XML.
	 */
	protected boolean wantsXml() {
		List<MediaType> mediaTypes = getRequestHeaders()
				.getAcceptableMediaTypes();
		return mediaTypes.isEmpty()
				|| mediaTypes.contains(MediaType.TEXT_XML_TYPE)
				|| mediaTypes.contains(MediaType.APPLICATION_XML_TYPE)
				|| mediaTypes.contains(MediaType.APPLICATION_XHTML_XML_TYPE)
				|| mediaTypes.contains(MediaType.TEXT_HTML_TYPE);
	}

	/**
	 * Checks whether the requested Media Type indicates JSON.
	 * 
	 * @return True if the requested Media Type is JSON.
	 */
	protected boolean wantsJson() {
		List<MediaType> mediaTypes = getRequestHeaders()
				.getAcceptableMediaTypes();
		return mediaTypes.contains(MediaType.APPLICATION_JSON_TYPE);
	}

	/**
	 * Translates a content String to a proper response.
	 * 
	 * @param content
	 *            The content String.
	 * @return The proper Response containing the content.
	 */
	protected Response responseXmlOrJson(String content) {
		try {
			if (wantsXml()) {
				return Response.ok(content, MediaType.APPLICATION_XML).build();
			} else if (wantsJson()) {
				return Response.ok(XmlToJsonConverter.convert(content),
						MediaType.APPLICATION_JSON).build();
			} else {
				if (hasDefaultMediaType()) {
					return Response.ok(content, getDefaultMediaType()).build();
				} else {
					return Response.notAcceptable(variantXmlJson()).build();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} catch (XMLStreamException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * @return media types we can handle
	 */
	private List<Variant> variantXmlJson() {
		return Variant.mediaTypes(MediaType.TEXT_XML_TYPE,
				MediaType.APPLICATION_XML_TYPE,
				MediaType.APPLICATION_XHTML_XML_TYPE, MediaType.TEXT_HTML_TYPE,
				MediaType.APPLICATION_JSON_TYPE).build();
	}

	/*
	 * Authentication can be refactored into a separate class could try make it
	 * generic, so it can be with any User/Service?
	 */

	protected static final String AUTHENTICATION_TYPE = "Basic ";
	protected static final String AUTHENTICATION_HDR_SEP = ":";

	/**
	 * Use information (credentials) in request header to authenticate the user
	 * 
	 * @return Authenticated user, but null if authentication failed
	 * @throws ServiceException
	 */
	protected DccdUser authenticate() throws ServiceException {
		DccdUser user = null;
		List<String> authHeaders = requestHeaders
				.getRequestHeader(HttpHeaders.AUTHORIZATION);
		if (authHeaders != null && !authHeaders.isEmpty()) {
			String authHeader = authHeaders.get(0);
			if (authHeader.startsWith(AUTHENTICATION_TYPE)) {
				String decodedAuthHeader = Base64.base64Decode(authHeader
						.substring(AUTHENTICATION_TYPE.length()));

				if (decodedAuthHeader.contains(AUTHENTICATION_HDR_SEP)) {
					String[] auth = decodedAuthHeader
							.split(AUTHENTICATION_HDR_SEP);
					String username = auth[0];
					String password = auth[1];

					UsernamePasswordAuthentication authentication = DccdUserService
							.getService().newUsernamePasswordAuthentication();
					authentication.setUserId(username);
					authentication.setCredentials(password);
					DccdUserService.getService().authenticate(authentication);
					user = (DccdUser) authentication.getUser();
				}
			}
		}
		return user;
	}
}
