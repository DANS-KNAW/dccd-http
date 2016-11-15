#!/usr/bin/python
# -*- coding: utf-8 -*-
"""Extract project information and files from the DCCD archive
Only archived (published) projcets, no Drafts.

Usage:
    extract_dccd_project.py --pid PID -U username [-P password] [--url URL] [--dir DIR]
    extract_dccd_project.py ( -h | --help)

Options:
    --pid PID         The project ID, like dccd:123
    -U username       Username, must be an account with admin privilege!
    -P password       Password; prompted for when not given as option
    --url URL         The url of the EASY server, defaults to 'http://localhost:8080/dccd-rest/rest'
    --dir DIR         The directory where the files are saved, defaults to './projects', note that it makes a subdir with the pid
    -h --help         Show this screen

Example:
    ./extract_dccd_project.py --pid "dccd:594" -U dccduseradmin --url "http://localhost:1402/dccd-rest/rest"

Requires:
    docopt
    requests

"""
import docopt
import requests
import os
from requests.auth import HTTPBasicAuth
import getpass

def downloadProjectMetadata(restURL, username, password, sid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/project/" + sid, auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, sid, r.url)

    xmlfilename = 'project_metadata.xml'
    with open(os.path.join(output_dir, xmlfilename), "wb") as f:
        f.write(r.content)

def downloadOrganisation(restURL, username, password, sid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/organisation/" + sid, auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, sid, r.url)

    xmlfilename = 'organisation.xml'
    with open(os.path.join(output_dir, xmlfilename), "wb") as f:
        f.write(r.content)

def downloadPermission(restURL, username, password, sid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/project/" + sid + "/permission", auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s" % (r.status_code, sid)

    xmlfilename = 'permission.xml'
    with open(os.path.join(output_dir, xmlfilename), "wb") as f:
        f.write(r.content)

def downloadUser(restURL, username, password, sid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/user/" + sid, auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s, url=%s" % (r.status_code, sid, r.url)

    xmlfilename = 'user.xml'
    with open(os.path.join(output_dir, xmlfilename), "wb") as f:
        f.write(r.content)

def getOwnerOrganisationId(restURL, username, password, sid):
    # get project info needed for downloading
    headers = {'accept': 'application/json'}
    r = requests.get(restURL + "/project/" + projectId, auth=(userName, userPasswd), headers=headers)
    if r.status_code != requests.codes.ok:
        print "Error; could not retrieve data!"
        print "Status: " + str(r.status_code) + "; response: " + r.text
    else:
        project = r.json()['project']
        organisationId = project['ownerOrganizationId']
        return organisationId

def getOwnerId(restURL, username, password, sid):
    # get permission info and extract user ID
    headers = {'accept': 'application/json'}
    r = requests.get(restURL + "/project/" + projectId + "/permission", auth=(userName, userPasswd),
                     headers=headers)
    if r.status_code != requests.codes.ok:
        print "Error; could not retrieve data!"
        print "Status: " + str(r.status_code) + "; response: " + r.text
    else:
        project = r.json()['permission']
        ownerId = project['ownerId']
        return ownerId

def downloadTridas(restURL, username, password, sid, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/project/" + sid + "/tridas", auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s" % (r.status_code, sid)

    # The TRiDaS XML won't be huge, just save it
    xmlfilename = 'tridas.xml'
    with open(os.path.join(output_dir, xmlfilename), "wb") as f:
        f.write(r.content)

def downloadAssociatedFile(restURL, username, password, sid, filename, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/project/" + sid + "/associated/" + filename, auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s" % (r.status_code, sid)

    with open(os.path.join(output_dir, filename), "wb") as f:
        f.write(r.content)

def downloadOriginalFile(restURL, username, password, sid, filename, output_dir):
    # print "Retrieving: " + sid + " ..."
    r = requests.get(restURL + "/project/" + sid + "/original/" + filename, auth=HTTPBasicAuth(username, password))
    if r.status_code != requests.codes.ok:
        print "Error %s; could not retrieve data for id: %s" % (r.status_code, sid)

    with open(os.path.join(output_dir, filename), "wb") as f:
        f.write(r.content)

def downloadAssociatedFiles(restURL, username, password, projectId, output_dir):
        headers = {'accept': 'application/json'}
        r = requests.get(restURL + "/project/" + projectId + "/associated", auth=(userName, userPasswd),
                         headers=headers)
        if r.status_code != requests.codes.ok:
            print "Error; could not retrieve data!"
            print "Status: " + str(r.status_code) + "; response: " + r.text
        else:
            associatedFiles = r.json()['files']
            if associatedFiles is not None:
                assoc_dir = os.path.join(output_dir, "associated")
                if not os.path.exists(assoc_dir):
                    os.makedirs(assoc_dir)

                associatedFileOrList = associatedFiles['file']
                if type(associatedFileOrList) is list:
                    # more then one
                    #print "More then one associated file"
                    for file in associatedFileOrList:
                        #print "associated file: %s" % file
                        downloadAssociatedFile(restURL, userName, userPasswd, projectId, file, assoc_dir)
                else:
                    # only one
                    file = associatedFileOrList
                    #print "One associated file: %s" % file
                    downloadAssociatedFile(restURL, userName, userPasswd, projectId, file, assoc_dir)


def downloadOriginalFiles(restURL, username, password, projectId, output_dir):
        headers = {'accept': 'application/json'}
        r = requests.get(restURL + "/project/" + projectId + "/original", auth=(userName, userPasswd),
                         headers=headers)
        if r.status_code != requests.codes.ok:
            print "Error; could not retrieve data!"
            print "Status: " + str(r.status_code) + "; response: " + r.text
        else:
            originalFiles = r.json()['files']
            if originalFiles is not None:
                orig_dir = os.path.join(output_dir, "original")
                if not os.path.exists(orig_dir):
                    os.makedirs(orig_dir)

                originalFileOrList = originalFiles['file']
                if type(originalFileOrList) is list:
                    # more then one
                    #print "More then one associated file"
                    for file in originalFileOrList:
                        #print "associated file: %s" % file
                        downloadOriginalFile(restURL, userName, userPasswd, projectId, file, orig_dir)
                else:
                    # only one
                    file = originalFileOrList
                    #print "One associated file: %s" % file
                    downloadOriginalFile(restURL, userName, userPasswd, projectId, file, orig_dir)

def downloadAdministrativeData(restURL, username, password, projectId, output_dir):
        admin_dir = os.path.join(output_dir, "administrative")
        if not os.path.exists(admin_dir):
            os.makedirs(admin_dir)

        downloadProjectMetadata(restURL, userName, userPasswd, projectId, admin_dir)
        organisationId = getOwnerOrganisationId(restURL, userName, userPasswd, projectId)
        downloadOrganisation(restURL, userName, userPasswd, organisationId, admin_dir)

        downloadPermission(restURL, userName, userPasswd, projectId, admin_dir)
        ownerId = getOwnerId(restURL, userName, userPasswd, projectId)
        downloadUser(restURL, userName, userPasswd, ownerId, admin_dir)

def extractProject(restURL, username, password, projectId, extractionDir):
        output_dir = os.path.join(extractionDir, projectId.replace(":", '_'))
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)

        downloadTridas(restURL, userName, userPasswd, projectId, output_dir)
        downloadAdministrativeData(restURL, userName, userPasswd, projectId, output_dir)
        downloadAssociatedFiles(restURL, userName, userPasswd, projectId, output_dir)
        downloadOriginalFiles(restURL, userName, userPasswd, projectId, output_dir)

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
            projectId = arguments["--pid"]

        if arguments["-U"]:
            userName = arguments["-U"]

        if arguments["-P"]:
            userPasswd = arguments["-P"]
        else:
            userPasswd = getpass.getpass()

        print "Extracting: %s by: %s from: %s" % (projectId, userName, restURL)
        print "Saving in: " + extractionDir
        extractProject(restURL, userName, userPasswd, projectId, extractionDir)
        print "Done."

    # invalid options
    except docopt.DocoptExit as e:
        print e.message
