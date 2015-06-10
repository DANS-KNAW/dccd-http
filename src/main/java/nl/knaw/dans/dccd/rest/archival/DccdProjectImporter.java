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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import nl.knaw.dans.dccd.application.services.DataServiceException;
import nl.knaw.dans.dccd.application.services.DccdDataService;
import nl.knaw.dans.dccd.application.services.TreeRingDataFileService;
import nl.knaw.dans.dccd.application.services.TreeRingDataFileServiceException;
import nl.knaw.dans.dccd.model.DccdTreeRingData;
import nl.knaw.dans.dccd.model.EntityTree;
import nl.knaw.dans.dccd.model.Project;
import nl.knaw.dans.dccd.model.ProjectAssociatedFileDetector;
import nl.knaw.dans.dccd.model.ProjectTreeRingDataFileDetector;
import nl.knaw.dans.dccd.model.entities.DerivedSeriesEntity;
import nl.knaw.dans.dccd.model.entities.Entity;
import nl.knaw.dans.dccd.model.entities.MeasurementSeriesEntity;
import nl.knaw.dans.dccd.model.entities.ValuesEntity;
import nl.knaw.dans.dccd.repository.xml.TridasLoadException;
import nl.knaw.dans.dccd.repository.xml.XMLFilesRepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tridas.interfaces.ITridasSeries;
import org.tridas.schema.TridasDerivedSeries;
import org.tridas.schema.TridasGenericField;
import org.tridas.schema.TridasMeasurementSeries;
import org.tridas.schema.TridasValues;

/**
 * 
 * 
 * @author paulboon
 *
 */
public class DccdProjectImporter {
	private static final Logger LOGGER   = LoggerFactory.getLogger(DccdProjectImporter.class);

	private static DccdArchivalMetaData loadMetadata(File projectFolder) {
		DccdArchivalMetaData metadata = new DccdArchivalMetaData();
		
		// set defaults, should be configurable?
		metadata.setValuesFormat("Heidelberg");
		metadata.setLanguage("en");
		
		// get parent folder and find the file
		try {
			File metaDataFile = FileExtractor.getArchivalMetaDataFile(projectFolder.getParentFile());
			
			// parse the xml, always a small file
			// no xsd and no validation etc. 
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(metaDataFile);
		 
			XPath xPath =  XPathFactory.newInstance().newXPath();

			try {
				String language = xPath.compile("/dccdmetadata/language").evaluate(doc);
				if(language != null && !language.trim().isEmpty())
				{
					metadata.setLanguage(language.trim());
					LOGGER.info("found language: " + metadata.getLanguage());
				}
			} catch (XPathExpressionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				String format = xPath.compile("/dccdmetadata/values/format").evaluate(doc);
				if(format != null && !format.trim().isEmpty())
				{ 
					metadata.setValuesFormat(format.trim());
					LOGGER.info("found format: " + metadata.getValuesFormat());
				}
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			// ignore, and use defaults
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return metadata;
	}
	
	private static boolean validateMetadata(DccdArchivalMetaData metadata)
	{
		// test if Language is usefull
		// how to validate, we only use the language? 
		//Locale locale = new Locale(metadata.getLanguage()); 
		//String language = locale.getLanguage();
		//if (language.isEmpty())
		//	return false;
		
		// test if format is ok
		List<String> readingFormats = TreeRingDataFileService.getReadingFormats();
		if (!readingFormats.contains(metadata.getValuesFormat()))
			return false;
		
		// If we get here all is fine
		return true;
	}
	
	/**
	 * 
	 * @param projectFolder
	 * @param userId
	 * 
	 * @return The SID of the project
	 * 
	 * @throws IOException
	 */
	public static String importProject(File projectFolder, String userId) throws IOException
	{
		String associatedFolderName = "associated"; // TODO get from options
		String valuesFolderName ="values"; // TODO get from options
		
		// try load metadata
		DccdArchivalMetaData metaData = loadMetadata(projectFolder);
		if (!validateMetadata(metaData)) 
		{
			throw new IOException("wrong metadata"); // Hmm, should be differnt kind of Exception
		}
		Locale tridasLanguage = new Locale(metaData.getLanguage()); // Locale.ENGLISH; // default
		String formatString = metaData.getValuesFormat();// "Heidelberg"; 
		
		
		// get the tridas File
		// assume it is the only xml file in the given folder
		File[] xmlFiles = FileExtractor.getXMLfiles(projectFolder);
		if (xmlFiles.length == 0)
		{
			LOGGER.error("No tridas found in folder: " + projectFolder);
			return "";
		}
		
		File tridasFile = xmlFiles[0]; // always take the first one
		System.out.println("tridas file: " + tridasFile.getPath());
		
		// get the subfolders for values and associated
		File associatedFolder = null;
		File valuesFolder = null;
		File[] subDirs = FileExtractor.getFolders(projectFolder);
		for(File sub : subDirs) {
			if (sub.getName().compareTo(associatedFolderName) == 0)
			{
				associatedFolder = sub;
			}
			else if (sub.getName().compareTo(valuesFolderName) == 0)
			{
				valuesFolder = sub;
			}	
		}
		
		if (associatedFolder == null)
		{
			LOGGER.warn("No associated folder found in folder: " + projectFolder);
			//return;			
		}

		if (valuesFolder == null)
		{
			LOGGER.warn("No values folder found in folder: " + projectFolder);
			//return;			
		}
		
		
		// Note
		// lets see how the CombinedUpload in DCCD is doing it, 
		// maybe it can be refactored into a an import service??

		// Import 
		
		LOGGER.info("Tridas File import..");
		Project project = importTridasFile(tridasFile, tridasLanguage, userId);
		LOGGER.info("done");
		
		LOGGER.info("Value Files import...");
		importValueFiles(project, valuesFolder, formatString); 
		// Value entities might have been added => 
		// Recreate the whole entity tree no matter what is already there!
		EntityTree entityTree = project.entityTree;
		entityTree.buildTree(project.getTridas());
		LOGGER.info("done");
			
		LOGGER.info("Associated Files import...");
		importAssociatedFiles(project, associatedFolder);
		LOGGER.info("done");
		

		LOGGER.info("Storing project...");
		// Store the project
		try
		{
			DccdDataService.getService().storeProject(project);
		}
		catch (DataServiceException e)
		{
			LOGGER.error("Failed to store project", e);
			throw (new IOException(e));
		}
		LOGGER.info("done");
		
		try
		{
			logProjectImport(project, projectFolder);
		}
		catch (FileNotFoundException e)
		{
			LOGGER.error("Failed to log importing of project", e);
			throw (new IOException(e));
		}
		
		return project.getSid();//getDmoStoreId().toString();//getSid();
	}
	
	// add log line to the project import log file
	private static void logProjectImport(Project project, File projectFolder) throws FileNotFoundException
	{
		//System.out.println(project.getSid() + "\t" + projectFolder.getPath());
		// TODO flush the file to make sure it is actually written
		
		//PrintWriter outFile = new PrintWriter(importLogFile.getAbsolutePath()); //opens file
		//outFile.println("what you want to write"); //writes to file
		//outFile.close(); //closes the file
	}
	
	@SuppressWarnings("unused")
	private void initLogForProjectImport()
	{
		// TODO open the file for writing in text mode?
		//
		//projectsRootFolder
		//File logFile = new File(projectFolder.getAbsolutePath() + File.separatorChar + tridasFileName);
        //FileWriter log = new FileWriter(logFile);
        //log.write("gnarf");
        //log.close();		
	}
	
	private static void importAssociatedFiles(Project project, File folder) throws IOException
	{
		List<String> projectAssociatedFileNames = ProjectAssociatedFileDetector.getProjectAssociatedFileNames(project);
		
		for(String fileName : projectAssociatedFileNames)
		{
			File file = new File(folder.getPath() + File.separatorChar + fileName);
			try
			{
				project.addAssociatedFile(file);
			}
			catch (IOException e)
			{
				throw (new IOException(e));
			}
		}
	}
	
	private static void importValueFiles(Project project, File folder, String formatString) throws IOException
	{
		List<MeasurementSeriesEntity> list = project.getMeasurementSeriesEntities();
		
		// but only series with external file ref's
		for(MeasurementSeriesEntity measurementSeries : list)
		{
			// check the genericFields
			TridasMeasurementSeries tridasSeries = (TridasMeasurementSeries)measurementSeries.getTridasAsObject();
			if (tridasSeries.isSetGenericFields())
			{
				// Ok we have potential candidates
				List<TridasGenericField> fields = tridasSeries.getGenericFields();
				for(TridasGenericField field : fields)
				{
					if (field.isSetValue() && field.isSetName() &&
							ProjectTreeRingDataFileDetector.isTreeRingDataFileIndicator(field.getName()))						
					{
						// We need to get the treering data for this series

						// load and convert
						File file = FileExtractor.findFileNoCase(folder, field.getValue());

						DccdTreeRingData data;
						try
						{
							data = TreeRingDataFileService.load(file, formatString);
							addDataToMeasurementSeriesEntity (measurementSeries, data);
							
							project.addOriginalFile(file);
							
							// Change fieldname to indicate the file has been uploaded" 
							field.setName(Project.DATAFILE_INDICATOR_UPLOADED);
						}
						catch (TreeRingDataFileServiceException e)
						{
							throw (new IOException(e));
						}
						catch (IOException e)
						{
							throw (new IOException(e));
						}
						
						break; // only one field should match, the first in this case!
					}
				}
			}
		}
		
		// Derived series
		// TODO refactor into two functions
		List<DerivedSeriesEntity> derivedList = project.getDerivedSeriesEntities();
		
		// but only series with external file ref's
		for(DerivedSeriesEntity derivedSeries : derivedList)
		{
			// check the genericFields
			TridasDerivedSeries tridasSeries = (TridasDerivedSeries)derivedSeries.getTridasAsObject();
			if (tridasSeries.isSetGenericFields())
			{
				// Ok we have potential candidates
				List<TridasGenericField> fields = tridasSeries.getGenericFields();
				for(TridasGenericField field : fields)
				{
					if (field.isSetValue() && field.isSetName() &&
							ProjectTreeRingDataFileDetector.isTreeRingDataFileIndicator(field.getName()))						
					{
						// We need to get the treering data for this series!
						// load and convert
						File file = FileExtractor.findFileNoCase(folder, field.getValue());
						
						DccdTreeRingData data;
						try
						{
							data = TreeRingDataFileService.load(file, formatString);
							addDataToDerivedSeriesEntity (derivedSeries, data);

							project.addOriginalFile(file);
							
							// Change fieldname to indicate the file has been uploaded" 
							field.setName(Project.DATAFILE_INDICATOR_UPLOADED);
						}
						catch (TreeRingDataFileServiceException e)
						{
							throw (new IOException(e));
						}
						catch (IOException e)
						{
							throw (new IOException(e));
						}
						break; // only one field should match, the first in this case!
					}
				}
			}
		}
		
	}
	
	/**
	 * add's the the treering data to the MeasurementSeriesEntity
	 * Used when combining all the project data
	 *
	 * @param measurementSeries
	 * @param treeringData
	 */
	private static void addDataToMeasurementSeriesEntity (MeasurementSeriesEntity measurementSeries,
													DccdTreeRingData treeringData)
	{
		List<TridasValues> tridasValuesList = treeringData.getTridasValuesForMeasurementSeries();

		// Note that each TridasValues instance has a group (or list) of value instances
		LOGGER.debug("Found groups of values: " + tridasValuesList.size());
		
		if (tridasValuesList.isEmpty())
		{
			LOGGER.warn("NO measurement series values found in uploaded TreeRingData: " + treeringData.getFileName());
			return; // nothing to do
		}
		
		addValuesToSeriesEntity ((Entity)measurementSeries, tridasValuesList);
	}

	private static void addDataToDerivedSeriesEntity (DerivedSeriesEntity derivedSeries,
													DccdTreeRingData treeringData)
	{
		List<TridasValues> tridasValuesList = treeringData.getTridasValuesForDerivedSeries();
		
		// Note that each TridasValues instance has a group (or list) of value instances
		LOGGER.debug("Found groups of values: " + tridasValuesList.size());
		
		if (tridasValuesList.isEmpty())
		{
			LOGGER.warn("NO derived series values found in uploaded TreeRingData: " + treeringData.getFileName());
			return; // nothing to do
		}
		
		addValuesToSeriesEntity ((Entity)derivedSeries, tridasValuesList);
	}
	
	private static void addValuesToSeriesEntity (Entity series,
			List<TridasValues> tridasValuesList)
	{
		// Get a list of all the (empty) values subelements 
		// and fill them with the given ones, then if any are left create new ones

		// Get all 'empty' values (placeholders) and try to fill those
		List<TridasValues> placeholderTridasValues = getEmptyTridasValues(series);
		
		// calculate the number of values to add to the placeholders
		int numberOfValuesToAdd = tridasValuesList.size();
		int numberOfPlaceholders = placeholderTridasValues.size();
		int numberOfValuesToAddToPlaceholders = numberOfValuesToAdd; // only fill what we have
		if (numberOfValuesToAdd > numberOfPlaceholders)
			numberOfValuesToAddToPlaceholders = numberOfPlaceholders; // fill all placeholders
		
		// add to placeholders
		for(int i=0; i < numberOfValuesToAddToPlaceholders; i++)
		{
			placeholderTridasValues.get(i).getValues().addAll(tridasValuesList.get(i).getValues());
		}
		
		// if there is any left to add, create new entities for them
		if (numberOfValuesToAdd > numberOfPlaceholders)
		{
			for(int i=numberOfPlaceholders; i < numberOfValuesToAdd; i++)
			{
				TridasValues tridasValues = tridasValuesList.get(i);
				ValuesEntity valuesEntity = new ValuesEntity(tridasValues);
				series.getDendroEntities().add(valuesEntity);
				
				// Add to the tridas
				// Note: we can't use series.connectTridasObjectTree(), 
				// because that would duplicate existing values
				Object tridasAsObject = series.getTridasAsObject();
				// should implement ITridasSeries
				((ITridasSeries)tridasAsObject).getValues().add(tridasValues);
			}
		}
	}
	
	private static List<TridasValues> getEmptyTridasValues(Entity series)
	{
		List<TridasValues> emptyValues = new ArrayList<TridasValues>();
		
		// assume series entity and filter subentities for Values entity
		List<Entity> subEntities = series.getDendroEntities();
		for(Entity subEntity : subEntities)
		{
			if (subEntity instanceof ValuesEntity)
			{
				TridasValues valuesFromEntity = (TridasValues) subEntity.getTridasAsObject();
				if (!valuesFromEntity.isSetValues())
				{
					emptyValues.add(valuesFromEntity);
				}
			}
		}
		
		return emptyValues;
	}
	
	private static Project importTridasFile(File tridasFile, Locale tridasLanguage, String userId) throws IOException
	{
		// parse the tridas and make it into a project
		FileInputStream fis;
		Project newProject = null;
		try
		{
			fis = new FileInputStream(tridasFile.getAbsolutePath());
			newProject = XMLFilesRepositoryService.createDendroProjectFromTridasXML(fis, userId);
			fis.close();
			// store the (original) xml filename
			newProject.setFileName(tridasFile.getName());
			// Note:  build the complete tree of entities, we can use for searching,
			// maybe this should be done by: XMLFilesRepositoryService.getDendroProjectFromTridasXML
			// and then copy the tree also?
			newProject.entityTree.buildTree(newProject.getTridas());
			// store the user(ID) that uploaded it
			newProject.setOwnerId(userId);
			newProject.setTridasLanguage(tridasLanguage);
			
			newProject.addOriginalFile(tridasFile);
		}
		catch (FileNotFoundException e)
		{
			throw (new IOException(e));
		}
		catch (IOException e)
		{
			throw (new IOException(e));
		}
		catch (TridasLoadException e)
		{
			throw (new IOException(e));
		}

		return newProject;
	}
}
