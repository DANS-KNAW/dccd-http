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
package nl.knaw.dans.dccd.rest.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * An utility class for converting URL objects to a byte array.
 * 
 * @author Georgi Khomeriki
 * @author Roshan Timal
 * @author paulboon
 */
public class UrlConverter
{

    /**
     * Throw an AssertionError if this class is instantiated.
     */
    protected UrlConverter()
    {
        throw new AssertionError("Instantiating utility class...");
    }

    /**
     * Given an URL and size (in bytes) this method will return the byte array.
     * 
     * @param url
     *            The URL that points to a file.
     * @param size
     *            The size of the file (in bytes).
     * @return An byte array that represents the file.
     * @throws IOException
     *             If something goes wrong while parsing the URL.
     */
    public static byte[] toByteArray(URL url, long size) throws IOException
    {
        InputStream input = url.openStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[Integer.parseInt("" + size)];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1)
        {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }

    /**
     * When you don't know the size
     * 
     * Note that this code is copied from AbstractBinaryUnit.readUrl
     * 
     * @param url
     *            The URL that points to a file.
     * @return An byte array that represents the file.
     * @throws IOException
     */
    public static byte[] toByteArray(URL url) throws IOException
    {
        InputStream inStream = null;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try
        {
            inStream = url.openStream();
            BufferedInputStream bis = new BufferedInputStream(inStream);

            int result = bis.read();
            while (result != -1)
            {
                byte b = (byte) result;
                buf.write(b);
                result = bis.read();
            }
        }
        finally
        {
            if (inStream != null)
            {
                inStream.close();
            }
        }

        return buf.toByteArray();
    }
}
