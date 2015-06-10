Python tests
============

A collection of Python scripts to test the DCCD archive via it's RESTfull interface. 

A typical test cycle will consist of the following steps

1. Create projects test data
  
   create_testdata.py

2. Ingest (upload) projects test data into archive
  
   batchingest.py

3. Do some checking

   list_myprojects.py


4. Delete projects test data from archive
  
   batchdelete.py


Note that the testdata folder contains
project folders that have not been created with a script.
Better create extra test projects in a separate 'data' folder. 

For organisations and users there is only a get request so you can use 
list_organisations.py and list_users.py to check. 
