************** COPYRIGHT AND CONFIDENTIALITY INFORMATION *********************
**                                                                          **
** Copyright (c) 2013 Qeo                                           **
** All Rights Reserved                                                      **
**                                                                          **
** This program contains proprietary information which is a trade           **
** secret of Qeo and/or its affiliates and also is protected as     **
** an unpublished work under applicable Copyright laws. Recipient is        **
** to retain this program in confidence and is not permitted to use or      **
** make copies thereof other than as permitted in a written agreement       **
** with Qeo, UNLESS OTHERWISE EXPRESSLY ALLOWED BY APPLICABLE LAWS. **
**                                                                          **
******************************************************************************

I. CONTENTS
===========
This tarball contains the following directories:
- com.Qeo.Qeo.0.17.0.docset  	: XCode documentation files
- Qeo.Bundle      						: UI registration dialogs
- Qeo.framework  						: Objective-C API for Qeo
- qeo-codegen							: files necessary to run the Qeo Code Generator
- sample-qsimplechat					: files necessary to compile and run the QSimpleChat sample App

It also contains README.txt which is this file.

II. DOCUMENTATION
=================
How to Install the Qeo documentation:
------------------------------------
Copy the com.Qeo.Qeo.0.17.0.docset file to ~/Library/Developer/Shared/Documentation/DocSets.

Note: The documentation can be viewed from within XCode. Go to help menu -> Documentation and API Reference.
	  Qeo documentation is versionized, this means that you can have multiple versions of the help next to each other.
	  
How to Remove the Qeo documentation:
-----------------------------------
Go to ~/Library/Developer/Shared/Documentation/DocSets and delete the com.Qeo.Qeo.0.17.0.docset 
Note: If the documentation was still selected in XCode, deleting it can cause XCode to crash.  
      We therefore recommend closing XCode before deleting the docset. 

III. IMPORTING THE FRAMEWORK
============================
The Qeo framework is composed of 2 sets of files:
   - Qeo.framework
   - Qeo.bundle

Qeo.framework:
-------------
To add it to your Xcode project:
   1. select your project in the source tree then select your target.
   2. Go to the "Build Phases" section, 
   3. Extend "Link Binary With Libraries" and click the "+" icon.
   4. From the popup dialog click the "Add Other..." button and navigate to the Qeo.framework file.
   5. Click "Open" to add the framework to your project.
   
Qeo.bundle:
----------
  To add it to your Xcode project: 
   1. Select the framework group in the source tree and right click on it to get a popup.
   2. Select Add Files to "..." and navigate to the Qeo.bundle file. Click "Add" button to add it to your project.
   Note: Verify that the Qeo.bundle file is also added to the "Copy Bundle Resources" section
         under your project's target "Build Phases". 
		 
Addtional Frameworks:
--------------------
You need two additional frameworks from the standard set of available frameworks in XCode:
   1. In the "Build Phases" section, click the "+" icon 
   2. In the list, select the following frameworks to add:
	  - AVFoundation.framework
	  - Coregraphics.framework
   3. Click "Open" to add the frameworks to your project

IV. INSTALL AND RUN THE QSIMPLECHAT SAMPLE APPLICATION
======================================================
About QSimpleChat:
-----------------
QSimpleChat is a basic chat application that allows you to send and receive chat messages from users running the App in your Realm.

How to Install the App:
----------------------
Proceed as follows:
   1. Open the QSimpleChat XCode project located in the sample-qsimplechat folder.
   2. In the project are references to the Qeo bundle and the Qeo framework. Remove these references, then add both the framework and the bundle as described above.
   3. Rebuild and run the App.

V. USING THE CODE GENERATOR
===========================
About the Code Generator:
------------------------
You can represent a Qeo topic using the xml-based Qeo Data Model (QDM). The purpose of the Qeo code generator is to translate a QDM file into one or more obj-c source files.  
These source files can then be used in your Xcode projects. The executable is located in qeo-codegen/bin. It generates a .h and .m file for your QDM.

Usage: 
-----
The usage of the command is:
qeo-codegen [options ...] [file ...]

The following options are available:
* -a,--abbrev <namespace::prefix=abbreviation>		Abbreviate namespace::prefix as abbreviation
* -h,--help											Display this help.
* -l,--language <arg>                           	Specify the language for which to generate code. Specify "objectiveC"
* -o,--output <arg> 								Specify the output directory for the generated code. Default is the current directory.

Example:
-------
Below is an example:
qeo-codegen -l objectiveC <MyQdmFile.xml> 

This command generates MyQdmFile.h and MyQdmFile.m. Since no input or output path is specified, the MyQdmFile.xml must be located in the /bin folder. The generated files are also saved in that folder.

More Information: 
----------------
For more information, refer to:
* http://wiki.qeo-app-development.com/display/QeoCusDoc/Code+Generator+User+Guide

* http://wiki.qeo-app-development.com/display/QeoCusDoc/Qeo+Data+Model+%28QDM%29+Developer+Guide




