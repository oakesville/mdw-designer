# Steps for Building and Publishing MDW Designer Plug-In:

In com.centurylink.mdw.designer:
 - Update mdwDesignerVersion in gradle.properties

Commit and push these changes to Git remote

Perform Jenkins Builds (http://<host>:<port>/jenkins):
 - MDW55 - Build
 - MDW6-Build (to pull the latest docs)
 - Designer Build
 - Designer Publish (or Designer Preview)

Publish to the Update Site on GitHub:
 - Download the updateSite artifacts from the Jenkins workspace:
   http://<host>:<port>jenkins/job/Designer%20Build/ws/com.centurylink.mdw.designer.ui/updateSite/
 - Copy mdw-designer-core-${mdwDesignerVersion}.jar to the maven repository directory (/prod/ecom2/local/apps/tomcat/tomcat7-mdwDiscovery/webapps/maven/repository/com/centurylink/mdw/mdw-designer-core/${mdwDesignerVersion})
 - Commit them to mdw6/docs/designer/updateSite on GitHub (and remove any old ones).    
 - Other plugins required in com.centurylink.mdw.designer.feature/feature.xml should already be present.
   If versions have changed, the newer versions may need to be uploaded (esp. cucumber.eclipse).
    
Test updating Eclipse (Mars/Neon) to the new build.
  Update Site URL: http://centurylinkcloud.github.io/mdw/designer/updateSite

(If RCP is to be included in this build)
Build com.centurylink.mdw.designer.rcp according to the instructions in its build.md.
  
On GitHub:
  - Close any open issues delivered with this build.
  - Create a milestone marker for the next upcoming build.
  - Assign any undelivered issues for this build's milestone to the next build's milestone.
  - Close this build's milestone in GitHub. 

Release Notes
  - If you are doing it first time then install ruby (https://github.com/CenturyLinkCloud/mdw#documentation) and do following in root of your workspace dir 
  `gem install github_changelog_generator`
  - github_changelog_generator --exclude-labels internal,wontfix,duplicate,invalid,documentation --no-pull-request --future-release v9.1.6
  - commit and push generated CHANGELOG.md to GitHub 
  - git commit CHANGELOG.md -m "Designer Release notes"
  - git push
  - Create the release on GitHub (https://github.com/CenturyLinkCloud/mdw-designer/releases/new), copy the notes from CHANGELOG.md
  
Update support items delivered with this build to Resolved status.

