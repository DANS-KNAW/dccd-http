#!/usr/bin/python
# -*- coding: utf-8 -*-
"""Extract project information and files from the DCCD archive

Usage:
    extract_project.py --pid PID -U username [-P password] [--url URL] [--dir DIR]
    extract_project.py ( -h | --help)

Options:
    --pid PID         The project ID, like dccd:123
    -U username       username, must be an account with admin privilege!
    -P password       Password; prompted for when not given as option
    --url URL         The url of the DCCD server, defaults to 'http://localhost:8080/dccd-rest/rest'
    --dir DIR         The directory where the files are saved, defaults to './projects', note that it makes a subdir with the pid
    -h --help         Show this screen

Example:
    ./extract_dccd_project.py --pid "dccd:123" -U admintestuser --url "http://localhost:1402/dccd-rest/rest"

Requires:
    docopt
    requests

"""
import docopt
import requests
import os
from requests.auth import HTTPBasicAuth
import getpass
import urllib

chunk_size = 4*1024*1024 # in bytes, for download

def downloadProjectMetadata(restURL, username, password, sid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/project/" + sid, auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, sid, r.url)

    xmlfilename = 'project_metadata.xml'
    with open(os.path.join(output_dir, xmlfilename), "wb") as f:
        f.write(r.content)

def downloadOrganisation(restURL, username, password, oid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/organisation/" + urllib.quote(oid.encode('utf8')), auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, oid, r.url)

    filename = 'organisation.xml'
    with open(os.path.join(output_dir, filename), "wb") as f:
        f.write(r.content)

def downloadPermission(restURL, username, password, sid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/project/" + sid + "/permission", auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, sid, r.url)

    xmlfilename = 'permission.xml'
    with open(os.path.join(output_dir, xmlfilename), "wb") as f:
        f.write(r.content)

def downloadUser(restURL, username, password, uid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/user/" + uid, auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, uid, r.url)

    xmlfilename = 'user.xml'
    with open(os.path.join(output_dir, xmlfilename), "wb") as f:
        f.write(r.content)

def getOwnerOrganisationId(restURL, username, password, sid):
    # get project info needed for downloading
    headers = {'accept': 'application/json'}
    r = requests.get(restURL + "/project/" + sid, auth=(username, password), headers=headers)
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, sid, r.url)
        print "Status: " + str(r.status_code) + "; response: " + r.text
    else:
        project = r.json()['project']
        organisationId = project['ownerOrganizationId']
        return organisationId

def getOwnerId(restURL, username, password, sid):
    # get permission info and extract user ID
    headers = {'accept': 'application/json'}
    r = requests.get(restURL + "/project/" + sid + "/permission", auth=(username, password),
                     headers=headers)
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, sid, r.url)
        print "Status: " + str(r.status_code) + "; response: " + r.text
    else:
        project = r.json()['permission']
        ownerId = project['ownerId']
        return ownerId

def downloadTridas(restURL, username, password, sid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/project/" + sid + "/tridas", auth=HTTPBasicAuth(username, password), stream=True)
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, sid, r.url)

    # The TRiDaS XML won't be huge, just save it
    filename = 'tridas.xml'
    with open(os.path.join(output_dir, filename), "wb") as f:
        #f.write(r.content)
        for chunk in r.iter_content(chunk_size):
            f.write(chunk)

def downloadAssociatedFile(restURL, username, password, sid, filename, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/project/" + sid + "/associated/" + urllib.quote(filename.encode('utf8')), auth=HTTPBasicAuth(username, password), stream=True)
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, filename: %s, url=%s" % (r.status_code, sid, filename, r.url)

    with open(os.path.join(output_dir, filename), "wb") as f:
        #f.write(r.content)
        for chunk in r.iter_content(chunk_size):
            f.write(chunk)

def downloadOriginalFile(restURL, username, password, sid, filename, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/project/" + sid + "/originalvalues/" + urllib.quote(filename.encode('utf8')), auth=HTTPBasicAuth(username, password), stream=True)
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, filename: %s, url=%s" % (r.status_code, sid, filename, r.url)

    with open(os.path.join(output_dir, filename), "wb") as f:
        #f.write(r.content)
        for chunk in r.iter_content(chunk_size):
            f.write(chunk)

def downloadAssociatedFiles(restURL, username, password, sid, output_dir):
        headers = {'accept': 'application/json'}
        r = requests.get(restURL + "/project/" + sid + "/associated", auth=(username, password),
                         headers=headers)
        if r.status_code != requests.codes.ok:
            print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, sid, r.url)
            print "Status: " + str(r.status_code) + "; response: " + r.text
        else:
            associatedFiles = r.json()['files']
            if associatedFiles is not None:
                assoc_dir = os.path.join(output_dir, "associated")
                if not os.path.exists(assoc_dir):
                    os.makedirs(assoc_dir)

                associatedFileOrList = associatedFiles['file']
                if type(associatedFileOrList) is list:
                    # more than one
                    for file in associatedFileOrList:
                        downloadAssociatedFile(restURL, username, password, sid, file, assoc_dir)
                else:
                    # only one
                    file = associatedFileOrList
                    #print "One associated file: %s" % file
                    downloadAssociatedFile(restURL, username, password, sid, file, assoc_dir)


def downloadOriginalFiles(restURL, username, password, sid, output_dir):
        headers = {'accept': 'application/json'}
        r = requests.get(restURL + "/project/" + sid + "/originalvalues", auth=(username, password),
                         headers=headers)
        if r.status_code != requests.codes.ok:
            print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, sid, r.url)
            print "Status: " + str(r.status_code) + "; response: " + r.text
        else:
            originalFiles = r.json()['files']
            if originalFiles is not None:
                orig_dir = os.path.join(output_dir, "originalvalues")
                if not os.path.exists(orig_dir):
                    os.makedirs(orig_dir)

                originalFileOrList = originalFiles['file']
                if type(originalFileOrList) is list:
                    # more than one
                    for file in originalFileOrList:
                        downloadOriginalFile(restURL, username, password, sid, file, orig_dir)
                else:
                    # only one
                    file = originalFileOrList
                    downloadOriginalFile(restURL, username, password, sid, file, orig_dir)

def downloadAdministrativeData(restURL, username, password, sid, output_dir):
        admin_dir = os.path.join(output_dir, "administrative")
        if not os.path.exists(admin_dir):
            os.makedirs(admin_dir)

        downloadProjectMetadata(restURL, username, password, sid, admin_dir)
        organisationId = getOwnerOrganisationId(restURL, username, password, sid)
        downloadOrganisation(restURL, username, password, organisationId, admin_dir)

        downloadPermission(restURL, username, password, sid, admin_dir)
        ownerId = getOwnerId(restURL, username, password, sid)
        downloadUser(restURL, username, password, ownerId, admin_dir)

def extractProject(restURL, username, password, sid, extractionDir):
        output_dir = os.path.join(extractionDir, sid.replace(":", '_'))
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)

        downloadTridas(restURL, username, password, sid, output_dir)
        downloadAdministrativeData(restURL, username, password, sid, output_dir)
        downloadAssociatedFiles(restURL, username, password, sid, output_dir)
        downloadOriginalFiles(restURL, username, password, sid, output_dir)

if __name__ == '__main__':
    try:
         # Parse arguments, use file docstring as a parameter definition
        arguments = docopt.docopt(__doc__)

        extractionDir = os.path.join('.', 'projects')
        if arguments["--dir"]:
            extractionDir = arguments["--dir"]
        # create output directory if needed
        if not os.path.exists(extractionDir):
            os.makedirs(extractionDir)

        restURL = 'http://localhost:8080/dccd-rest/rest'
        if arguments["--url"]:
            restURL = arguments["--url"]

        if arguments["--pid"]:
            sid = arguments["--pid"]

        if arguments["-U"]:
            username = arguments["-U"]

        if arguments["-P"]:
            password = arguments["-P"]
        else:
            password = getpass.getpass()

        print "Extracting: %s by: %s from: %s" % (sid, username, restURL)
        print "Saving in: " + extractionDir
        extractProject(restURL, username, password, sid, extractionDir)
        print "Done."

    # invalid options
    except docopt.DocoptExit as e:
        print e.message
