# Introduction #

This document contains instructions on how to setup the development environment for purcforms in eclipse.


# Setup #

  * Check out the trunk which has the url: https://purcforms.googlecode.com/svn/trunk/

  * Import the four projects into eclipse. These projects are in the folders: SharedLib, FormDesigner, FormRunner and QueryBuilder.


Purcforms trunk is currently using GWT version 2.0.3 and the necessary GWT files are already part of your check out.
If you have any other version of GWT as a plugin in your eclipse, you may get problems while trying to run any of these projects. If that's your case, you may need to first uninstall the plugin. We also have branches for purcforms using GWT 1.5.3 and GWT 1.7.1 as https://purcforms.googlecode.com/svn/branches/GWT 1.5.3 and https://purcforms.googlecode.com/svn/branches/GWT 1.7.1 respectively.


## Running with GWT version 2.0.3 (trunk) ##

To run the Form Designer, right click the project and select Run As, then under the eclipse run configurations, run the configuration called FormDesigner.

To run the Query Builder, right click the project and select Run As, then under the eclipse run configurations, run the configuration called QueryBuilder.

The Form Runner is not useful to run on its own. It needs to be embedded in another application for you to have meaningful results.

You could also run the ant target called "dev" in the ant build file in each project.

You can run in debug mode by right clicking a project and instead of Run As, select Debug As, then select the appropriate configuration.


## Compiling to JavaScript with GWT version 2.0.3 (trunk) ##

To compile the Form Designer project into JavaScript, run the ant target called "dev" in the ant build file under the FormDesigner folder.

To compile the Query Builder project into JavaScript, run the ant target called "dev" in the ant build file under the QueryBuilder folder.


To compile the Form Runner project into JavaScript, run the ant target called "dev" in the ant build file under the FormRunner folder.


## Running with GWT versions 1.5.3 and 1.7.1 ##

To run the Form Designer, right click the project and select Run As, then under the eclipse run configurations, run the configuration called FormDesigner.

To run the Query Builder, right click the project and select Run As, then under the eclipse run configurations, run the configuration called QueryBuilder.

The Form Runner is not useful to run on its own. It needs to be embedded in another application for you to have meaningful results.


You can run in debug mode by right clicking a project and instead of Run As, select Debug As, then select the appropriate configuration.


## Compiling to JavaScript with GWT versions 1.5.3 and 1.7.1 ##

To compile the Form Designer project into JavaScript, run or double click the file "FormDesigner-compile.cmd" which is in the FormDesigner folder. The output files will be under www/org.purc.purcforms.FormDesigner

To compile the Query Builder project into JavaScript, run or double click the file "QueryBuilder-compile.cmd" which is in the QueryBuilder folder. The output files will be under www/org.purc.purcforms.QueryBuilder

To compile the Form Runner project into JavaScript, run or double click the file "FormRunner-compile.cmd" which is in the FormRunner folder. The output files will be under www/org.purc.purcforms.FormRunner