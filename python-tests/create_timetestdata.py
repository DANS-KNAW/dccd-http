#!/usr/bin/python

"""Create Temporal test data for the DCCD archive

For each 'test' project it creates a directory and file structure with the DCCD project data that can be uploaded to the DCCD archive using its RESTfull interface.  

Requires:
	docopt
	
Usage:
	create_timetestdata.py [-d DIR]
	create_timetestdata.py ( -h | --help)
	
Options:
	-h --help         Show this screen
	-d DIR            Output directory [default: timedata]
	
"""
from docopt import docopt
import zipfile
import os, sys
import random

#print "Create test data to use for batch ingestion";
# Hardcode everything for now!

outputDirName = "timedata"

""" Construct the metadata xml """
def constructMetadata():
	template = """<?xml version="1.0" encoding="UTF-8"?>
<dccdmetadata>
    <language>nl</language>
    <values>
        <format>Heidelberg</format>
    </values>
</dccdmetadata>
"""
	return template

""" Construct the TRiDaS xml """
def constructTridas(title, lat, lng, firstYear, lastYear):
	template = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<tridas:tridas xmlns:tridas="http://www.tridas.org/1.2.2" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:gml="http://www.opengis.net/gml">
    <tridas:project>
        <tridas:title>%(title)s</tridas:title>
        <tridas:identifier domain="stichtingring.nl">P:1993041</tridas:identifier>
        <tridas:createdTimestamp>2010-06-30T11:11:41Z</tridas:createdTimestamp>
        <tridas:lastModifiedTimestamp>2010-06-30T11:11:41Z</tridas:lastModifiedTimestamp>
        <tridas:type lang="en" normal="dating" normalId="1522" normalStd="DCCD">datering</tridas:type>
        <tridas:laboratory>
            <tridas:identifier domain="www.cultureelerfgoed.nl">1</tridas:identifier>
            <tridas:name acronym="NLROB">Rijkdsdienst voor het Oudheidkundig Bodemonderzoek</tridas:name>
            <tridas:address>
                <tridas:cityOrTown>Amersfoort</tridas:cityOrTown>
                <tridas:country>Nederland</tridas:country>
            </tridas:address>
        </tridas:laboratory>
        <tridas:category lang="en" normal="archaeology" normalId="1552" normalStd="DCCD">archeologie</tridas:category>
        <tridas:investigator>E. Jansma</tridas:investigator>
        <tridas:period>1993</tridas:period>
        <tridas:object>
            <tridas:title>TEST object</tridas:title>
            <tridas:identifier domain="stichtingring.nl">O:1993041:VEL</tridas:identifier>
            <tridas:createdTimestamp>2010-06-30T12:35:13Z</tridas:createdTimestamp>            <tridas:lastModifiedTimestamp>2010-06-30T12:35:13Z</tridas:lastModifiedTimestamp>
            <tridas:type lang="en" normal="Military camp" normalId="367" normalStd="DCCD">Legerplaats</tridas:type>
            <tridas:location>
                <tridas:locationGeometry>
                    <gml:Point srsName="WGS 84">
                        <gml:pos>%(lng)s %(lat)s</gml:pos>
                    </gml:Point>
                </tridas:locationGeometry>
            </tridas:location>
            
<tridas:element>
                <tridas:title>Onbekend</tridas:title>
                <tridas:identifier domain="stichtingring.nl">E:1993041:VEL:0001</tridas:identifier>
                <tridas:createdTimestamp>2010-06-30T14:33:57Z</tridas:createdTimestamp>
         <tridas:lastModifiedTimestamp>2010-06-30T14:33:57Z</tridas:lastModifiedTimestamp>
                <tridas:type lang="en" normal="Unknown" normalId="417" normalStd="DCCD">Onbekend</tridas:type>
                <tridas:description>Sp 170; Vnr 390</tridas:description>
                <tridas:taxon lang="la" normal="Quercus" normalId="1319" normalStd="DCCD_Taxon">Quercus</tridas:taxon>
                <tridas:sample>
                    <tridas:title>0001</tridas:title>
                    <tridas:identifier domain="stichtingring.nl">S:1993041:VEL:0001</tridas:identifier>
                   <tridas:createdTimestamp>2010-05-18T18:37:07Z</tridas:createdTimestamp>
               <tridas:lastModifiedTimestamp>2010-05-18T18:37:07Z</tridas:lastModifiedTimestamp>
                    <tridas:type lang="en" normal="various" normalId="1579" normalStd="DCCD">Various</tridas:type>
                    <tridas:samplingDate certainty="approximately">1987-06-01</tridas:samplingDate>
                    <tridas:radius>
                        <tridas:title>4223</tridas:title>
                        <tridas:identifier domain="stichtingring.nl">R:1993041:VEL:0001:1</tridas:identifier>
                        <tridas:createdTimestamp>2010-06-30T15:00:59Z</tridas:createdTimestamp>
                        <tridas:lastModifiedTimestamp>2010-06-30T15:00:59Z</tridas:lastModifiedTimestamp>
                        <tridas:woodCompleteness>
                            <tridas:ringCount>43</tridas:ringCount>
                            <tridas:pith presence="absent"/>
                            <tridas:heartwood presence="unknown"/>
                            <tridas:sapwood presence="unknown">
<tridas:nrOfSapwoodRings>6</tridas:nrOfSapwoodRings>
<tridas:lastRingUnderBark presence="absent"></tridas:lastRingUnderBark>
<tridas:missingSapwoodRingsToBark>0</tridas:missingSapwoodRingsToBark>
                            </tridas:sapwood>
                            <tridas:bark presence="present"/>
                        </tridas:woodCompleteness>
                        <tridas:measurementSeries id="M3841">
                            <tridas:title>VEL00011</tridas:title>
                            <tridas:identifier domain="stichtingring.nl">M:1993041:VEL:0001:1</tridas:identifier>
                            <tridas:createdTimestamp>2010-08-29T15:35:31Z</tridas:createdTimestamp>
                            <tridas:lastModifiedTimestamp>2010-08-29T15:35:31Z</tridas:lastModifiedTimestamp>
                            <tridas:measuringDate certainty="after">1993-01-01</tridas:measuringDate>
                            <tridas:woodCompleteness>
<tridas:ringCount>43</tridas:ringCount>
<tridas:pith presence="absent"/>
<tridas:heartwood presence="incomplete"/>
<tridas:sapwood presence="complete">
    <tridas:nrOfSapwoodRings>6</tridas:nrOfSapwoodRings>
    <tridas:lastRingUnderBark presence="present"></tridas:lastRingUnderBark>
    <tridas:missingSapwoodRingsToBark>0</tridas:missingSapwoodRingsToBark>
</tridas:sapwood>
<tridas:bark presence="present"/>
                            </tridas:woodCompleteness>
                            <tridas:analyst>Pauline van Rijn</tridas:analyst>
                            <tridas:dendrochronologist>Esther Jansma</tridas:dendrochronologist>
                            <tridas:measuringMethod normalTridas="measuring platform">Measuring platform</tridas:measuringMethod>
                            
<tridas:interpretation>
<tridas:firstYear suffix="%(firstYearSuffix)s">%(firstYear)s</tridas:firstYear>
<tridas:lastYear suffix="%(lastYearSuffix)s">%(lastYear)s</tridas:lastYear>
<tridas:statFoundation>
<tridas:statValue>8.3</tridas:statValue>
<tridas:type>t-score</tridas:type>
<tridas:usedSoftware>Corina 2.10</tridas:usedSoftware>
</tridas:statFoundation>
<tridas:provenance>Somewhere</tridas:provenance>
</tridas:interpretation>
        
<tridas:values>                 
<tridas:variable normalTridas="ring width"/>
<tridas:unit normalTridas="1/100th millimetres"/>
<tridas:value value="54"/>
</tridas:values>                 
                        </tridas:measurementSeries>
                </tridas:radius>
        </tridas:sample>
</tridas:element>
 
        </tridas:object>
    </tridas:project>
</tridas:tridas>	
"""
	#NOTE years all AD now!
	firstYearSuffix = 'AD'
	if firstYear <= 0 :
		firstYear = -firstYear + 1
		firstYearSuffix = 'BC'
	lastYearSuffix = 'AD'
	if lastYear <= 0 :
		lastYear = -lastYear + 1
		lastYearSuffix = 'BC'
	return template % {'title': title,'lat':str(lat),'lng':str(lng), 'firstYearSuffix':firstYearSuffix, 'firstYear':str(firstYear),'lastYearSuffix':lastYearSuffix,'lastYear':str(lastYear)}

"""
This function will recursively zip up a directory tree, compressing the files, and recording the correct relative filenames in the archive. The archive entries are the same as those generated by zip -r output.zip source_dir. 

Code from http://stackoverflow.com/questions/1855095/how-to-create-a-zip-archive-of-a-directory-in-python
"""
def make_zipfile(output_filename, source_dir):
	relroot = os.path.abspath(os.path.join(source_dir, ".."))
	#with zipfile.ZipFile(output_filename, "w", zipfile.ZIP_DEFLATED) as zip:
	# DISABLE compression
	# somehow the RESTfull interface with (Java) unzip can't handle
	# the compressed zip from Python...
	with zipfile.ZipFile(output_filename, "w") as zip:
		for root, dirs, files in os.walk(source_dir):
			# add directory (needed for empty dirs)
			zip.write(root, os.path.relpath(root, relroot))
			for file in files:
				filename = os.path.join(root, file)
				if os.path.isfile(filename): # regular files only
					arcname = os.path.join(os.path.relpath(root, relroot), file)
					zip.write(filename, arcname)
		zip.close()
					              
"""
 Create the project directory structure with the (xml) files
 
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
        
Use the index to vary the 'details' of the project
In this case the project name and the location on the map
"""
def createProject(i):	
	# Rome
	lat = 41.893 + i*0.01
	lng = 12.483 + i*0.02

	# astronomical years (0 = 1BC)
	firstYear = -1500 + random.randint(0, 3000)
	lastYear = firstYear + 100 + random.randint(0, 50)
	projectName = "timetest" + str(i)
	
	projectFolderName = projectName
	# project folder
	pPath = os.path.join(outputDirName, projectFolderName)
	os.mkdir( pPath, 0755 )
	# metadata file
	metadataFile = open (os.path.join(pPath,"metadata.xml"), 'a')
	metadataFile.write(constructMetadata())
	metadataFile.close()
	# data folder
	dPath = os.path.join(pPath, "data")
	os.mkdir( dPath, 0755 )
	# tridas file
	tridasFile = open (os.path.join(dPath,"tridas.xml"), 'a')
	tridasFile.write(constructTridas(projectName, lat, lng, firstYear, lastYear))
	tridasFile.close()
	# associated and values when needed, but not yet!
	print "Created project in folder: " + projectFolderName
	#
	# create the zip file
	zipFilename = os.path.join(outputDirName, projectName+".zip")
	make_zipfile(zipFilename, pPath)

""" main """
def main(argv={}):
	#print(argv)
	#return
	
	global outputDirName

	if argv['-d']:
		outputDirName = argv['DIR']

	# create dir in not existent
	if not os.path.exists(outputDirName):
		os.mkdir(outputDirName, 0755 )
    
    # create the project folders
	numProjects = 1000
	print "Starting creation of %d projects..."%numProjects
	for i in range(0, numProjects):
		print "Creating project (%d of %d) ..."%(i+1, numProjects) 
		createProject(i)
	print "Done"


if __name__ == '__main__':
    main(docopt(__doc__))
    