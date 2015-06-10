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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import nl.knaw.dans.common.lang.file.UnzipUtil;
//import nl.knaw.dans.common.lang.util.FileUtil;
import nl.knaw.dans.dccd.util.FileUtil;

/*

The file and directory structure inside the 'zip' would look like this. 

projectfolder/
|
+-- metadata.xml
|
+-- data/
   |
   +-- tridas.xml
   |
   +-- associated/
   |  |
   |  +-- project.pdf
   |
   +-- values/
      |
      +-- heidelberg.fh
*/
public class FileExtractor {
	private static String tmpBasePathName = System.getProperty("java.io.tmpdir");
	
	public static final String DATA_FOLDER_NAME = "data"; // TODO get from options
	
	public static File getArchivalMetaDataFile(File zipFolder) throws IOException
	{
		File metadataFile = null;
		
		// TODO getMetadataFile
		// the 'only' file (xml) in the root should be the metadata file
	    File[] files = getXMLfiles(zipFolder);
		
	    if (files.length > 0)
	    {
	    	metadataFile = files[0];
	    	// TODO warn if there is more than one
	    }
	    
		return metadataFile;
	}
	
	public static File getDataFolder(File zipFolder) throws IOException
	{
		// find a subfolder with the name 'data' and return it
		File dataFolder = null;
		File[] subDirs = getFolders(zipFolder);
		for(File sub : subDirs) {
			if (sub.getName().compareTo(DATA_FOLDER_NAME) == 0)
			{
				dataFolder = sub;
			}
		}
		return dataFolder;
	}
	
    public static List<File> unzip(final InputStream inputStream, File tempDir) throws IOException
    {
        try
        {
            return UnzipUtil.unzip(new ZipInputStream(new BufferedInputStream(inputStream)), tempDir.getPath());
        }
        catch (final ZipException exception)
        {
            //logger.error("unzip problem", exception);
            throw new IOException("Failed to unzip deposited file");
        }
        catch (final IOException exception)
        {
            //logger.error("unzip problem", exception);
            throw new IOException( "Failed to unzip deposited file");
        }
    }

    public static File createTempDir() throws IOException
    {
        final File basePath = new File(tmpBasePathName);
        final String prefix = "dccd-rest-unzip";
        if (!basePath.exists())
        {
            if (!basePath.mkdir())
                throw new IOException("please create location for temporary unzip directories: " + basePath.getAbsolutePath());
        }
        try
        {
            return FileUtil.createTempDirectory(basePath, prefix);
        }
        catch (final IOException exception)
        {
            throw new IOException("Could not create temp dir: " + basePath.getAbsolutePath() + "/" + prefix);
        }
    }
    
	// Only checking for directory
    // TODO put in the FileUtils and reuse (like in DccdProjectImporter)
	public static File[] getFolders(File dir)
	{
	    File[] files = dir.listFiles(new FileFilter() {
	    	public boolean accept(File f) { 
	    		return f.isDirectory();
	    	}
	    });
	    
	    return files;
	}
	
	/**
	 * Note: could make it more general when it accepts a list of possible extensions
	 * 
	 * @param dir
	 * @param extension
	 * @return
	 */
	public static File[] getFilesWithExtension(File dir, final String extension)
	{
	    File[] files = dir.listFiles(new FileFilter() {
	    	public boolean accept(File f) { 
	    		return (f.isFile() && 
	    				(getExtension(f).compareToIgnoreCase(extension)==0));
	    	}
	    });
	    
	    return files;
	}
	
	public static String getExtension(File f)
	{
		String extension = "";
		String fileName = f.getName();
		int dotPos = fileName.lastIndexOf('.');
		if (dotPos > 0 && 
			dotPos < fileName.length()-1) // handle names ending with a dot
			extension = fileName.substring(dotPos+1);
		//System.out.println("extension: " + extension);	
		return extension;
	}

	/**
	 * find file with same name, but case-insensitive
	 * 
	 * @param dir
	 * @param filename
	 * @return
	 */
	public static File findFileNoCase(File dir, final String filename)
	{		
	    File[] files = dir.listFiles(new FileFilter() {
	    	public boolean accept(File f) { 
	    		return (f.isFile() && 
	    				(f.getName()).compareToIgnoreCase(filename)==0);
	    	}
	    });
		
	    // NOTE if we have more we should warn?
	    return files[0];
	}
	
	// Only checking the extension
	public static File[] getXMLfiles(File dir)
	{
	    File[] files = FileExtractor.getFilesWithExtension(dir, "xml");
	    
	    return files;
	 }
}
