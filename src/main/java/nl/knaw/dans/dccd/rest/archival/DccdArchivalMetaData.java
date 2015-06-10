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
package nl.knaw.dans.dccd.rest.archival;

/**
 * Metadata not in the TRiDaS file but needed for the DCCD Archive
 * Note that TRiDaS already has all dendrochronological metadata and data 
 * 
 * Maybe it will be something like this: 
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 * <dccdmetadata>
 *  <state></state>
 *  <owner>
 *    <id></id>
 *  </owner>
 *  <permission>
 *   <defaultLevel></defaultLevel>
 *  </permission> 
 *  <language></language>
 *  <values>
 *   <format></format>
 *  </values>
 * </dccdmetadata>
 *
 * Default values can be used right now, 
 * But absolutely minimal for uploading is the values file format 
 * and the language, because you need to specify them in the GUI as well. 
 * 
 * @author paulboon
 *
 */
public class DccdArchivalMetaData {
	private String valuesFormat;
	private String language; // main language used in TRiDaS and vocabulary
	
	public String getValuesFormat() {
		return valuesFormat;
	}

	public void setValuesFormat(String valuesFormat) {
		this.valuesFormat = valuesFormat;
	}
	
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
}
