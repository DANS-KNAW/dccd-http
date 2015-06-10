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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;

/**
 * An utility class to convert data to other formats.
 * 
 * @author Georgi Khomeriki
 * @author Roshan Timal
 */
public class XmlToJsonConverter
{

    /**
     * Throw an AssertionError if this class or one of it's subclasses is ever
     * instantiated.
     */
    protected XmlToJsonConverter()
    {
        throw new AssertionError("Instantiating utility class...");
    }

    /**
     * Converts a byte array containing XML to JSON.
     * 
     * @param xml
     *            The byte array containing XML.
     * @return String containing JSON representation of the given XML.
     * @throws IOException
     *             Thrown if reading input as InputStream goes wrong.
     * @throws XMLStreamException
     *             Thrown if XML parsing goes wrong.
     */
    public static String convert(byte[] xml) throws IOException, XMLStreamException
    {
        return convert(new String(xml));
    }

    /**
     * Converts a String containing XML to JSON.
     * 
     * @param xml
     *            The XML containing String.
     * @return String containing JSON representation of the given XML.
     * @throws IOException
     *             Thrown if reading input as InputStream goes wrong.
     * @throws XMLStreamException
     *             Thrown if XML parsing goes wrong.
     */
    public static String convert(String xml) throws IOException, XMLStreamException
    {
        String json = "";

        InputStream input = new ByteArrayInputStream(xml.getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        JsonXMLConfig config = new JsonXMLConfigBuilder().autoArray(true).prettyPrint(true).build();
        try
        {
            XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(input);

            XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(output);

            writer.add(reader);

            reader.close();
            writer.close();

            json = new String(output.toByteArray());
        }
        finally
        {
            output.close();
            input.close();
        }

        return json;
    }
}
