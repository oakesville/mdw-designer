/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.designer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ActionCancelledException;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.ImporterExporterJson;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.designer.utils.RestfulServer;
import com.centurylink.mdw.model.value.process.PackageVO;

/**
 * Command-line exporter for saving workflow packages to XML-formatted files.
 */
public class Exporter {

    public static final String EXPORT = "export";

    private DesignerDataAccess dataAccess;

    Exporter(DesignerDataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    /**
     * For VCS assets only.
     */
    public Exporter(String assetRoot) throws IOException {
        try {
            RestfulServer restfulServer = new RestfulServer("jdbc://dummy", null, "http://dummy");
            VersionControl versionControl = new VersionControlGit();
            versionControl.connect(null, null, null, new File(assetRoot));
            restfulServer.setVersionControl(versionControl);
            restfulServer.setRootDirectory(new File(assetRoot));
            dataAccess = new DesignerDataAccess(restfulServer, null, EXPORT, false);
        }
        catch (Exception ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    public static void main(String[] args) {
        if (args.length == 1 && "-h".equals(args[0])) {
            System.out.println("Incorrect Usage: ");
            System.exit(0);
        }
        if (args.length != 2 && args.length != 3) {
            System.err.println("arguments: <jdbcUrl|fileBasedRootDir> <packageName> <xmlFile>");
            System.err.println("(-h for example usage)");
            System.exit(-1);
        }

        String arg0 = args[0]; // either jdbcUrl or local file path
        String packageName = args[1];
        String outFile = args[2];

        try {
            boolean local = !arg0.startsWith("jdbc:");
            RestfulServer restfulServer = new RestfulServer(local ? "jdbc://dummy" : arg0, null,
                    "http://dummy");
            DesignerDataAccess dataAccess;

            if (local) {
                VersionControl versionControl = new VersionControlGit();
                versionControl.connect(null, null, null, new File(arg0));
                restfulServer.setVersionControl(versionControl);
                restfulServer.setRootDirectory(new File(arg0));
                dataAccess = new DesignerDataAccess(restfulServer, null, EXPORT, false);
            }
            else {
                dataAccess = new DesignerDataAccess(restfulServer, null, EXPORT, true);
            }

            System.out.println(
                    "Exporting with arguments: " + arg0 + " " + packageName + " " + outFile);
            Exporter exporter = new Exporter(dataAccess);
            long before = System.currentTimeMillis();
            String xml = exporter.exportPackage(packageName, true, outFile.endsWith(".json"));
            File outputFile = new File(outFile);
            if (outputFile.exists()) {
                System.out.println("Overwriting existing file: " + outputFile);
            }
            else if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
                throw new IOException("Cannot create directory: " + outputFile.getParentFile());
            }
            exporter.writeFile(outputFile, xml.getBytes());
            long afterExport = System.currentTimeMillis();
            System.out.println("Time taken for export: " + ((afterExport - before) / 1000) + " s");
        }
        catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    private boolean isLocal() {
        return dataAccess.isVcsPersist();
    }

    public String exportPackage(String packageName, boolean includeTaskTemplates, boolean isJson)
            throws DataAccessException, RemoteException, ActionCancelledException, XmlException,
            JSONException {

        int schemaVersion = dataAccess.getDatabaseSchemaVersion();
        PackageVO packageVo = dataAccess.getPackage(packageName);
        if (isJson) {
            List<PackageVO> packages = new ArrayList<>();
            packages.add(dataAccess.loadPackage(packageVo.getId(), true));
            ImporterExporterJson jsonExporter = new ImporterExporterJson();
            return jsonExporter.exportPackages(packages);
        }
        else {
            String xml = dataAccess.exportPackage(packageVo.getId(), schemaVersion,
                    includeTaskTemplates, null);
            if (!isLocal()) {
                packageVo.setExported(true);
                dataAccess.savePackage(packageVo);
            }
            return xml;
        }
    }

    public void writeFile(File file, byte[] contents) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(contents);
        }
    }
}
