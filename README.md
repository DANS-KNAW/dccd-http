DCCD RESTfull API
=================
This document describes dccd-rest; a service that provides a RESTfull interface to the DCCD archive and thus exposes machine readable information. 
Note that this is a 'non public' interface that runs on the server and is used by 'tools', but at some point parts might be made publicly available. 

We distingues two types of information sources for a 'dendro' Project or Object in the DCCD archive:

1. The TRiDaS data which is stored in a TRiDaS xml file. 
   The TRiDaS documentation and schema can be found at [tridas.org](tridas.org). 
   To improve readability I will quote the TRiDaS schema when necessary.

2. Archival metadata, is what is not in the TRiDaS file, but needed for archiving it and is stored in the Fedora metadata xml. 


The API supports http GET requests for projects and when needed it uses Basic authentication (should be with secure http). 
The result is valid XML and in case of the dendrological data/metadata we use TRiDaS. 
Any additional/associated file wil be downloaded as is. 

Note on identifiers. 
The Fedora identifier (sid) is being used to specify a project. This is a string with a prefixed (namespace) followed by a colon and then a number. For dccd project number 23 it is "dccd:23" for instance. 
Having a url like http://dendro.dans.knaw.nl/dccd-rest/rest/project/dccd:23 
seems to be allright. 
True Persistent Identifier (PERSID) might be usefull for the Open Archives Initiative Protocol for Metadata Harvesting (OAI-PMH). 

Note that the DCCD webapplication will provide 'landing page' for specific projects. For instance when we have a project with sid 'dccd:23'
the (html) page is

	http://dendro.dans.knaw.nl/project/dccd:23 

while the XML (rest) response can be found at 

	http://dendro.dans.knaw.nl/dccd-rest/rest/project/dccd:23 

Note that currently the REST interface is not exposed to the outside world and only available on the server via localhost:8080. If you have an account on that server you can use an ssh tunnel.  


Installation
------------
You need to build it from the source code using Maven (commandline: mvn clean install). 
After building, deploy the dccd-rest.war file on the server that also has the DCCD archive deployed. Have a look at the examples below to test if the service is running correctly.  


API Resources
-------------

## Project

GET

- project/ 

  will return the list of projects, using offset and limit as get params. Only Published/Archived projects are listed. 
  no authentication needed. The Web GUI does not have this functionality; it lists objects (with their projects) instead. 
  
  For harversting like query the modFrom and modUntil can be used. 


  GET Parameters

  - offset
  
    The  number of results to 'skip' alowing 'paging'. 

  - limit
    
    The maximal number of results to retrieve alowing 'paging'.
  
  - modFrom 
    
    specifies that projecst must have been modified on or after the given  date. UTC ISO standard. 
     
  - modUntil 
    
    specifies that projecst must have been modified on or before the given  date. UTC ISO standard. 


- project/{sid}

  no authentication needed
  
  Returns the project's public metadata and only published/archived projects can be requested.

- project/{sid}/tridas

  authentication 
  
  This will download the full TRiDaS file when the user has download permission (values level). 
  
  When there is no download permission it will return (partial) TRiDaS with only the allowed parts. This corresponds with what can be seen on the web interface. 
  Notes and place holders will be added as genericField elements with the names: dccd.incompleteTridasNote and dccd.incompleteTridas.entityPlaceholder.  

- project/{sid}/tridas/{level}

  authentication 
  
  This will download the TRiDaS file up to and including the entities of the given level. 
  The permissions of the user on the project will also be taken into account, possibly resulting in less levels then requested. 

  Allowed levels from top to bottom: 
  project | object | element | sample | radius | series | values

- project/{sid}/associated

  authentication (but no extra permission needed)

  Returns a list of the filenames of all associated files.
  
  Example: 
  
    ```
    <?xml version="1.0" encoding="UTF-8"?>
    <files>
        <file>report.pdf</file>
    </files> 
    ```
  
- project/{sid}/associated/{filename}

  authentication (permission for download needed)
  
  Returns the associated file

- project/{sid}/originalvalues

  authentication (but no extra permission needed)

  Returns a list of the filenames of all original value files.
  
  There can be just one TRiDaS file including the original values, but there can also be other (non-TriDaS) files containing the values and those have been converted to TRiDaS at upload. 
  
- project/{sid}/originalvalues/{filename}

  authentication (permission for download needed)
  
  Returns the original values file

- project/{sid}/permission

  authentication (you must have ADMIN role or be the owner of the project)
  
  Returns the projects permission information, indicating who can view or download certain information.
  
  Example: 
  
    ```
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <permission>
      <projectId>dccd:4151</projectId>
      <ownerId>paulboon</ownerId>
      <defaultLevel>MINIMAL</defaultLevel>
      <userPermissions>
        <userPermission><userId>pwb48</userId><level>ELEMENT</level></userPermission>
      </userPermissions>
    </permission>
    ```
  
  Permission levels:

    ```
    MINIMAL (minimal, only 'open access' public information is available)
    PROJECT (view down to TRiDaS 'Project' entity level)
    OBJECT (view down to TRiDaS 'Object' entity level)
    ELEMENT (view down to TRiDaS 'Element' entity level)
    SAMPLE (view down to TRiDaS 'Sample' entity level)
    RADIUS (view down to TRiDaS 'Radius' entity level)
    SERIES (view down to TRiDaS 'Series' entity level)
    VALUES (maximal = download))
    ```

## MyProject

Authentication always needed

GET 

- myproject/ 
  
  just your own projects and lets you also get to your drafts 
authentication needed

- myproject/{sid}

  your project, with more info then the public info (what you want if you are the owner)



- myproject/query

  Allows to search for projects (of your own).  You are allowed to find anything regardless of Status or permission. 
  
  Result
  
  The root element now is obviously 'projects' and it has the same result list parameters as the 'object/query' resource. 
  The 'project' list elements have the same public project data as described by the 'object/query' resource. Because we query our own data her we get a bit more: 
  
  - state
  
    The 'archival' state of this project; either DRAFT or PUBLISHED. 
  
  - permission
    
    with a defaultLevel


POST

- myproject/ 
  
  Upload and store (deposit) a project in the archive. It will be in a Draft state when it succeeds. The uploaded file must be a zip with an optional metadata xml file in the root (topmost) directory and all data files in a 'data' subdirectory. 
  The must at least be a TRiDaS xml file in this data directory. Assocuated files must be in a subdirectory 'associated' and value files in a subdirectory 'values'. 
  The file and directory structure would look like this. 

    ```
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
     ```       
  
  The metadata content is limited now; only the language and the value files format. The format is used when importing and converting (non TRiDaS) value files. The following is an example of a metadata file. 

    ```  
    <?xml version="1.0" encoding="UTF-8"?>
    <dccdmetadata>
        <language>en</language>
        <values>
            <format>Heidelberg</format>
        </values>
    </dccdmetadata> 
    ```
      
  Result
  
  The project sid as plain text
  
  
DELETE

- myproject/{sid}
  
  Removes the project from the archive. 
  Only allowed on a Draft version
  

## Object

GET

- object/query
  
  Allows to search for objects; what the GUI makes available via advanced search. 
  And respects the permission level, so you can't infer knowledge that you are not supposed to know by doing smart querying. 

  GET Parameters
  
  - q 
    
    free text query

  - offset
  
    The  number of results to 'skip' alowing 'paging'. 

  - limit
    
    The maximal number of results to retrieve alowing 'paging'.

  - category 
    
    TRiDaS Project Category

  - labname

    The name of the laboratory

  - object.type 
    
    TRiDaS Object Type
    
  - object.creator
  
    TRiDaS Object creator  

  - element.taxon 
    
    TRiDaS Element Taxon

  - element.type 
    
    TRiDaS Element Type

  - year ranges 
    astronomical years so 1BC = 0 and 2BC = -1. YearFrom and YearTo are interpreted as inclusive range bundaries. 
    
    - deathYearFrom
    - deathYearTo
	- firstYearFrom
	- firstYearTo
	- lastYearFrom
	- lastYearTo
	- pithYearFrom
	- pithYearTo

  
   
  Result
  
  The list of the 'paged' results are wrapped in a root element 'objects' with the standard result list parameters: 
  
  - total

    Total number of results (results in this response)

  - offset
    
    The offset of the request

  - limit
    
    The limit of the request
    
  For each 'object' we have the following elements: 
  
  - sid

  - title

  - identifier

  - project
    
    This contains the project specific information. 
    
    The publicly available 'open access' data is: 
    
    - sid
    - title
    - identifier
    - category
    - laboratories
    - types
    
    Administrative (should be open, access rights related)
    - ownerOrganisationId
    - permission defaultLevel
    
    If available and allowed by the permission level 
    
    - location with lat and lng (always WGS84)
    - timeRange with the first and last of all years (astronomical)
    - objectType
    - elementType
    - taxon


  
## Organisation

GET

- organisation/ 

  Returns a list of all organisations. If you are not authenticated or no Admin, only the organisations that are active. 
  For each organisation the id (a unique name) and optionally the city and or country is returned. Note that this information is also available on the webinterface without the need to be logged in. 
  If the requester has admin rights (role) then all oragnisation (also non-active) with their account state are returned. 
  
  Returns
  
    - id
    - city
    - country
    - accountState (only if admin)
    With values:
    ```
    REGISTERED
    CONFIRMED_REGISTRATION
    ACTIVE
    BLOCKED
    ```
  
- organisation/{oid} 
  
  Returns
  
  same as above, but also optional
  
    - address
    - postalcode
 


  Returns detailed organisation information of that specific organisation. You can only get your own detailed information or you must have admin rights. 


## User

GET

This need authentication, you must be a registered user (member) of the DCCD, otherwise spambots will like this resource as well. 

- user/ 

  Returns a list of all users that are active and not admin.   
  For each user the id (a unique name) and other required properties are returned. 
  If the requester has admin rights (role) then all users (also non-active and admin) with their roles and account state are returned. 
  
    - id
    - displayname
    - lastname
    - email
    - organisation
    - accountState (only if admin)
    - roles (only if admin)
      With one or more values:
    ```
    USER  
    ADMIN
    ```
  In practice this means you have either USER or USER and ADMIN.
  
- user/{uid} 

  Returns detailed user information of that specific user. You can only get your own detailed information or you must have admin rights. 
  
  same as above, but also optional
  
    - title
    - initials
    - prefixes
    - function
    - telephone
    - dai
  
  And
  
    - lastLoginDate (only if admin)

Result details
--------------
###Restrictions on visibility of information

An unauthenticated GET request should only give information that is permitted to be publicly available or 'open access'. The REST interface should be simmilar to the web interface because at some point the REST interface might be open to the outside world. On the website there is a difference between what information any site vistor (public) can see, and what a 'logged in' member can see. The RESTful API will give a bit more than the 'open access' information that can be seen on the search result page, but will always honour the socalled default permissionlevel. 
   
Extra besides the project level information 'textual' visible on the search result page:

information                       | permission level needed
----------------------------------|--------------
Project.description               | Project
location en Object.type           | Object 
Element.type en Element.taxon     | Element 
interpretation years (time range) | Series    

   
## project information
 The TRiDaS project: "A project is defined by a laboratory and encompasses dendrochronological 
research of a particular object or group of objects."
   
- sid
    
   Uniquely identifies the project in the 'dccd' archive
   
   Example: 
   
      <sid>dccd:37</sid>
 
 - state (only for 'myproject', for others it is not usefull to know)
 
   The 'archival' state of this project; either DRAFT, PUBLISHED or DELETED. 
   
 - stateChanged
    
   The last time the content in the archive changed, mostly it is the 'archival publication' time. 
   The UTC time format used is xxx
   
   Example: 
   
       <stateChanged>2013-09-11T14:18:21.811Z</stateChanged>
        
 - title
 
   TRiDaS project title: "Title or name of this entity.  This should be a 'human readable' name by which the entity is referred.".
   Note that DCCD allows it to contain newlines. 
   
   Example: 
   
       <title>Velsen I</title>
      
 - identifier
    
   TRiDaS project identifier:"Identifier for this entity which in combination with the domain should be unique. ". 
   DCCD extra restriction is that this is unique within the archive when 'published'. 
   
   Note that the 'domain' is not given!
   Maybe we should put it in as an attribute, just as TRiDaS does?
   
   Example: 
   
       <identifier>P:1993041</identifier> 
    
 - category
    
   TRiDaS project category: "Category of research this project falls into. ". 
   DCCD forces this to be a term from  a list of allowed terms; the DCCD vocabulary. 
   
   Example: 
   
       <category>archeologie</category>
      
 - investigator
    
   The TRiDaS project investigator: "Principal investigator of this project.". 
      
   Note that this is not the same as member that uploaded it; project 'owner', but it could be. 
   
   Example: 
   
       <investigator>E. Jansma</investigator>
      
 - laboratories
    
   The list of TRiDaS laboratory information: "The dendrochronological research laboratory where this work was done.". 
   Every laboratory element has its 'internal' information concatinated to a comma separted list. 
   
   Examnple (only one lab.): 
   
       <laboratories>
            <laboratory>Rijkdsdienst voor het Oudheidkundig
                Bodemonderzoek,NLROB,Amersfoort,Nederland</laboratory>
        </laboratories>
        
 - types
    
   The list of TRiDaS project types: "The type of entity this is.". 
   DCCD forces this to be a term from  a list of allowed terms; the DCCD vocabulary.
   
   Example:
     
       <types>
            <type>datering</type>
       </types>
     
     
 - location
    
   If allowed 'object' level permission and the first object has a location (point) specified this will be available as location with lat and lng (always WGS84 in DCCD)
   
   Example: 
   
       <location>
            <lat>51.803</lat>
            <lng>32.303</lng>
       </location>
 
 - time range
   
   If allowed 'series' permission. 
   The first and last of all years availabe for any of the series; firstYear, pithYear, lastYear or deathYear. Years in 'astronomical year numbering' (1BC = 0). 
   
   Example: 
   
       <timeRange>
           <firstYear>-48</firstYear>
           <lastYear>79</lastYear>
       </timeRange>   


- ownerOrganizationId
   
   Example: 
   
       <ownerOrganizationId>Stichting RING</ownerOrganizationId>

- language
   
   The language code of the primary language used in this project
   
   Example: 
   
       <language>nl</language>

- permission defaultLevel
   
   The most detailed level (TRiDaS entity) which you are allowed to see the information from. 
   Note that besides the default level specific users can have a less restrictive level (with more detail), but that is not shown here. 
   
   Example: 
   
       <permission><defaultLevel>values</defaultLevel></permission>
       
- taxons taxon
   
   If allowed 'element' permission. Indicates the species of wood producing plant of elements in this project and the terms are from a controlled vocabulary. 
   Only the distinctive values, no duplicates. 
   
   Example: 
   
       <taxons><taxon>Quercus</taxon></taxons>
       
- elementTypes elementType

   
   If allowed 'element' permission. Indicates the type of elements in this project and the terms are from a controlled vocabulary. 
   Only the distinctive values, no duplicates. 

   Example: 
   
       <elementTypes><elementType>Onbekend</elementType></elementTypes>

- objectTypes objectType

   If allowed 'object' permission. Indicates the type of objects in this project and the terms are from a controlled vocabulary. 
   Only the distinctive values, no duplicates. 
   
   Example: 
   
       <objectTypes><objectType>Legerplaats</objectType></objectTypes>

- description
   
   If allowed 'project' permission. 
   Description of this project
   
   Example: 
   
       <description></description>

When authenticated and the user has the ADMIN role also the project state is given. 
Only then DRAFT projects can be returned.

   Example: 
   
       <state>PUBLISHED</state>
       
       it's either PUBLISHED or DRAFT


## object information
   
Contains the information of the project, but also the object specific information.
   
 - sid

   Uniquely identifies the object in the 'dccd' archive
   
   Example: 
   
       <sid>dccd:37/TF2</sid>. 
       Note that the first part corresponds to the project sid. 
 
 - title
   TRiDaS object title.
    
 - identifier
   TRiDaS object identifier. No extra restictions. 
   

## DCCD Vocabularies used

### project.type
It is about the purpose or kind of dendrochronological research project. 

en              | nl                | fr
----------------|-------------------|----------------
anthropology    | antropologie      | l'anthropologie
climatology     | klimatologie      | climatologie
dating          | datering          | datation
ecology         | ecologie          | écologie
entomology      | entomologie       | entomologie
forest dynamics | bosdynamiek       | dynamique forestrière
forest management studies | bosbeheer studies | études de gestion forestière
forestry        | bosbouw           | foresterie
hydrology       | hydrologie        | hydrologie
geomorphology   | geomorfologie	     | géomorphologie
glaciology      | glaciologie       | glaciologie
palaeo-ecology  | paleo-ecologie    | paléo-écologie
provenancing    | herkomst bepaling | provenance
pyrochronology  | pyrochronologie   | pyrochronologie
wood technology | hout technologie  | technologie du bois
wood biology    | hout biologie	     | biologie du bois
other: go to project.comments | anders: ga naar commentaar (project.commentaar) | autre: allez à commentaires (project.comments)


### project.category
It is about the 'things' being studied.

en                 | nl                 | de                | fr
-------------------|--------------------|-------------------|----
archaeology        | archeologie        | Archäologie       | archéologie
built heritage     | gebouwd erfgoed    | Baudenkmalpflege  | patrimoine immobilier
furniture          | meubilair          | Möbel             | mobilier
mobilia            | mobilia            | mobilia	 | patrimoine mobilier
musical instrument | muziek instrument  | Musikinstrument   | instrument de musique
painting           | schilderij         | Gemälde           | 	peinture
palaeo-vegetation  | paleo-vegetatie    | Paläovegetation   | paléo-végétation
ship archaeology   | scheepsarcheologie | Schiffarchäologie | archéologie navale
standing trees     | staande boom       | stehende Bäume    | arbre sur pied
woodcarving        | houtsnijwerk       | Holzschnitzarbeit | sculpture sur bois
other              | anders             | andere            | autre

### object.type and element.type

### element.taxon


Example usage
-------------
On the commandline using curl, assuming you have deployed it at http://localhost:8080 and you have a user with id 'normaltestuser' and a password 'testtest'. 

    $ curl "http://localhost:8080/dccd-rest/rest/project/?offset=0&limit=20"

    $ curl http://localhost:8080/dccd-rest/rest/project/dccd:1

    $ curl -u normaltestuser:testtest http://localhost:8080/dccd-rest/rest/project/dccd:23/tridas

    $ curl -u normaltestuser:testtest http://localhost:8080/dccd-rest/rest/project/dccd:23/associated

    $ curl -u normaltestuser:testtest http://localhost:8080/dccd-rest/rest/project/dccd:23/associated/associated.pdf

    $ curl -u normaltestuser:testtest http://localhost:8080/dccd-rest/rest/myproject

    $ curl -u normaltestuser:testtest -H "Accept: application/json" http://localhost:8080/dccd-rest/rest/myproject/query?q=gouda

    $ curl -u normaltestuser:testtest -H "Accept: application/json" http://localhost:8080/dccd-rest/rest/object/query?q=name

    $ curl -u normaltestuser:testtest -H "Accept: application/json" http://localhost:8080/dccd-rest/rest/object/query?category=archaeology
    
    $ curl -H "Accept: application/json"  "http://localhost:8080/dccd-rest/rest/project/?modFrom=2013-01-02T09%3a25%3a25.297Z&modUntil=2013-06-02T09%3a25%3a25.297Z"


Upload and delete is usefull for testing

	$ curl -u normaltestuser:testtest -i -F file=@data/testimport-tridasonly.zip http://localhost:8080/dccd-rest/rest/myproject

And if that returns the sid "dccd:38", you can delete it

	$ curl -u normaltestuser:testtest -X DELETE http://localhost:8080/dccd-rest/rest/myproject/dccd:38


TIPs
----
- You can see in the python-test directory that python is easy to use for consuming the RESTfull interface. These scripts use the 'requests' package ([http://www.python-requests.org/en/latest/](http://www.python-requests.org/en/latest/)). 

- Converting the XML data to CSV, so managers can import it in MSExcel, can be done with the 'xmlutils' python package ([https://pypi.python.org/pypi/xmlutils](https://pypi.python.org/pypi/xmlutils)). Generating a list of users could be done by first getting the user information with the RESTfull interface and then convert it: 

	$ curl -u normaltestuser:testtest http://localhost:8080/dccd-rest/rest/user > dccd-users.xml
	$ xml2csv --input "dccd-users.xml" --output "dccd-users.csv" --tag "user"



Response type
------------- 
  
You can specify the response type (JSON or XML) with curl by adding:

    -H "Accept: application/json" 

or

    -H "Accept: application/xml"

Default is XML


Notes on JSON:

* The JSON seems to be more readable and therefore better when testing on the commandline. 

* If there is only one project the json output will not use a list (with bracket [] notation). 
Clients that receive and parse the json must be aware of this. 



Wishlist
--------
Note that there are no administrative functions, and we want to keep it that way. 
Things that we might want are listed below. 
   
* Get tridas data up to a given level 
  for instance element, or object.

* PUT on
  myproject/{sid}
  With the metadata specifying a change of stat. Only Draft to Archived would be first allowed. This would then Validate and archive the project. 
  A change of permissions on the project could also be specified by the matadata file. 
  

* GET on

  project/{id}/originals

  project/{id}/values/{format}

