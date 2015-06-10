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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.verify;
import static org.easymock.EasyMock.isA;

import java.util.ArrayList;

import javax.ws.rs.core.HttpHeaders;

import nl.knaw.dans.common.lang.service.exceptions.ServiceException;
import nl.knaw.dans.dccd.application.services.DccdUserService;
import nl.knaw.dans.dccd.authn.UsernamePasswordAuthentication;
import nl.knaw.dans.dccd.model.DccdUser;
import nl.knaw.dans.dccd.model.DccdUserImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.jersey.core.util.Base64;

/** 
 * Authentication Unit testing with Mocking of the (user) service
 * 
 * A bare minimal amount of testing, but it's a starting point for the other classes.
 * 
 * Somehow Mockito and Powermock won't pull together the static mocking
 * 	      so using EasyMock instead of Mockito...
 * 	      
 * @author paulboon
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { DccdUserService.class})
public class AbstractResourceTest {
	private AbstractResource resource;
    private HttpHeaders requestHeadersMock;

    @Before
    public void setUp()
    {
    	requestHeadersMock = createMock(HttpHeaders.class);
        resource = new TestResource();
        resource.setRequestHeaders(requestHeadersMock);
    }

    @Test
    public void authenticateWithNullHeaders() throws ServiceException
    {
        expect(requestHeadersMock.getRequestHeader(isA(String.class))).andStubReturn(null);
        replay(requestHeadersMock);
        
        DccdUser user = resource.authenticate();
        
        verify(requestHeadersMock);
        assertNull(user);
    }

    @Test
    public void authenticateWithIncorrectUsernamePasswordPattern() throws ServiceException
    {
        ArrayList<String> authHeader = new ArrayList<String>();
        authHeader.add("Basic dXNlcm5hbWUgcGFzc3dvcmQ="); // wrong formatted username password?
        // could get correct one from commandline 
        // $ echo -n "normaltestuser:testtest" | base64

        expect(requestHeadersMock.getRequestHeader(isA(String.class))).andStubReturn(authHeader);
        replay(requestHeadersMock);
        
        DccdUser user = resource.authenticate();
        
        verify(requestHeadersMock);
        assertNull(user);
    }

    @Test
    public void authenticateSuccesfull() throws Exception
    {
    	DccdUser user = new DccdUserImpl();
        setUpUserService(user);
        
        ArrayList<String> authHeader = new ArrayList<String>();
        byte[] encoded = Base64.encode("name:passwd");
                
        authHeader.add("Basic " + new String(encoded, "US-ASCII")); // username:password
        expect(requestHeadersMock.getRequestHeader(isA(String.class))).andStubReturn(authHeader);
        replay(requestHeadersMock);
        
        DccdUser user2 = resource.authenticate();

    	verify(DccdUserService.class);
        assertEquals(user, user2);
    }

    @Test
    public void authenticateFail() throws Exception
    {
        setUpUserService(null); // authentication fails, we are not testing authentication itself!
        
        ArrayList<String> authHeader = new ArrayList<String>();
        byte[] encoded = Base64.encode("name:passwd");
                
        authHeader.add("Basic " + new String(encoded, "US-ASCII")); // username:password
        expect(requestHeadersMock.getRequestHeader(isA(String.class))).andStubReturn(authHeader);
        replay(requestHeadersMock);
        
        DccdUser user = resource.authenticate();

    	verify(DccdUserService.class);
        assertNull(user);	
    }
    
    private void setUpUserService(final DccdUser user)
    {
    	mockStatic(DccdUserService.class);	
    	
    	DccdUserService serviceMock = createMock(DccdUserService.class);	
    	expect(DccdUserService.getService()).andStubReturn(serviceMock);
    	replay(DccdUserService.class);
    	
    	UsernamePasswordAuthentication auth = createMock(UsernamePasswordAuthentication.class);
    	//auth.setCredentials(password);
    	auth.setCredentials(isA(String.class));
    	expectLastCall().once();
    	//auth.setUserId(username);
    	auth.setUserId(isA(String.class));
    	expectLastCall().once();
    	
    	//DccdUser userMock = new DccdUserImpl();
    	//expect(auth.getUser()).andStubReturn(userMock);
    	expect(auth.getUser()).andStubReturn(user);
    	replay(auth);
    	
    	expect(serviceMock.newUsernamePasswordAuthentication()).andStubReturn(auth);
    	serviceMock.authenticate(isA(UsernamePasswordAuthentication.class));
    	expectLastCall().once();
    	
    	replay(serviceMock);
    }
    
    // an implementation of the abstract class to test
    class TestResource extends AbstractResource {};
}
