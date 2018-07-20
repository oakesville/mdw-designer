## MDW Designer

1. Designer Projects:       
     - com.centurylink.mdw.designer (gradle parent)
     - com.centurylink.mdw.designer.core
     - com.centurylink.mdw.designer.feature
     - com.centurylink.mdw.designer.rcp
     - com.centurylink.mdw.designer.ui

2. Build:
   - Configure
     - These projects use the MDW 5.5 versions of mdw-common and mdw-schemas, so to build you need these MDW 5.5 projects or their jars available locally.
     - Assuming you've got the MDW 5.5 source projects, in com.centurylink.mdw.designer/gradle.properties, set mdwVersion and mdwOutputDir to point to this location.
   - Build Designer
     - (One time) Run an MDW 5.5 build in its workspace and then in com.centurylink.mdw.designer, run the gradle task getMdwCommon to copy in the 5.5 dependencies.
     - (Subsequently) When changes are made to common code in MDW 5.5 and an Eclipse build is performed in that workspace, running devGetMdwCommon will incrementally copy these.
     - Now after refreshing the projects and Gradle refresh, an Eclipse build of the Designer projects should show no errors.
3. Run:     
   - Debug Designer
     - To run through Eclipse, right-click on project com.centurylink.mdw.designer.ui and select Debug As > Eclipse Application.
