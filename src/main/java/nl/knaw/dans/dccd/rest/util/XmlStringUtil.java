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

import org.apache.commons.lang.StringEscapeUtils;

public class XmlStringUtil {

	public static final String XML_INSTRUCTION_STR = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
	
	public static String getXMLElementStringOptional(final String tagname, final String text)
	{
		if (text != null && !text.trim().isEmpty())
			return getXMLElementString(tagname, text);
		else
			return "";
	}

	public static String getXMLElementString(final String tagname, final String text)
	{
		return "<"+tagname+">" + StringEscapeUtils.escapeXml(text) + "</"+tagname+">";
	}
}
