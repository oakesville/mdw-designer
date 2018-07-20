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
package com.centurylink.mdw.plugin.designer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.cli.Discover;
import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.plugin.CodeTimer;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.File;
import com.centurylink.mdw.plugin.designer.model.Folder;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;

public class Discoverer {
    private URL url;
    private boolean latestVersionsOnly;
    private IProgressMonitor progressMonitor;
    private int topLevelFolders;
    private String groupId;
    private HttpHelper httpHelper;

    public Discoverer(URL url, HttpHelper httpHelper) {
        this.url = url;
        this.httpHelper = httpHelper;
    }

    public Discoverer(URL url) {
        this.url = url;
    }

    public Discoverer(String groupId) {
        this.groupId = groupId;
    }

    public Folder getAssetTopFolder(boolean latestVersionsOnly, IProgressMonitor progressMonitor)
            throws DiscoveryException, IOException, InterruptedException {
        this.latestVersionsOnly = latestVersionsOnly;
        this.progressMonitor = progressMonitor;
        progressMonitor.worked(10);
        CodeTimer timer = new CodeTimer("crawl for assets");
        Folder topFolder = new Folder(MdwPlugin.getSettings().getMdwReleasesUrl());
        if (groupId != null) {
            Discover discover = new Discover(groupId, latestVersionsOnly);
            parseJsonPacakges(topFolder,
                    discover.run(new SwtProgressMonitor(progressMonitor)).getPackages().toString());
        } else if (url != null) {
            topFolder = new Folder(url.toString());
            crawlForPackageFiles(topFolder);
            removeEmptyFolders(topFolder);
        }
        timer.stopAndLog();
        return topFolder;
    }

    private void removeEmptyFolders(Folder folder) {
        List<Folder> emptyFolders = new ArrayList<>();
        for (WorkflowElement child : folder.getChildren()) {
            if (child instanceof Folder) {
                Folder childFolder = ((Folder)child);
                if (childFolder.getChildren() == null)
                    emptyFolders.add(childFolder);
            }
        }
        for (Folder emptyFolder : emptyFolders) {
            if (emptyFolder.getParent() instanceof Folder) {
                Folder parent = emptyFolder.getParent();
                parent.getChildren().remove(emptyFolder);
            }
        }
    }

    /**
     * Crawls to find any package XML or JSON files.
     */
    private void crawlForPackageFiles(Folder parent)
            throws DiscoveryException, IOException, InterruptedException {
        if (progressMonitor.isCanceled())
            throw new InterruptedException();
        String parentUrl = getFullUrl(parent);
        if (httpHelper == null)
            httpHelper = new HttpHelper(new URL(parentUrl));
        httpHelper.setConnectTimeout(MdwPlugin.getSettings().getHttpConnectTimeout());
        httpHelper.setReadTimeout(MdwPlugin.getSettings().getHttpReadTimeout());

        if (!parent.hasParent()) // topLevel
            httpHelper.setFollowHtmlHeadMetaRefresh(true);

        String content = httpHelper.get();
        if (content.startsWith("{")) {
            parseJsonPacakges(parent, content);
        }
        else {
            if (content.startsWith("<!"))
                content = content.substring(content.indexOf('\n') + 1);
            if (content.contains("&nbsp;"))
                content = content.replaceAll("&nbsp;", "")
                        .replace("<HR size=\"1\" noshade=\"noshade\">", "");
            if (!parent.hasParent() && httpHelper.getRedirect() != null)
                parent.setName(httpHelper.getRedirect().toString());
            try {
                List<String> links = parseDirectoryResponse(content);
                if (!parent.hasParent())
                    topLevelFolders = links.size();
                if (latestVersionsOnly) {
                    List<String> latestLinks = new ArrayList<>();
                    String latestDir = null;
                    for (String link : links) {
                        if (!link.startsWith("6") && !link.endsWith("-SNAPSHOT/")) // snapshots excluded from
                                                          // "latest only"
                        {
                            if (link.matches("[0-9.]*/")) {
                                if ((latestDir == null || latestDir.compareTo(link) < 0))
                                    latestDir = link;
                            }
                            else {
                                latestLinks.add(link);
                            }
                        }
                    }

                    if (latestDir != null)
                        latestLinks.add(latestDir);

                    links = latestLinks;
                }

                for (String link : links) {
                    if (!link.startsWith("6") && link.endsWith("/") && (MdwPlugin.getSettings().isIncludePreviewBuilds()
                            || !link.endsWith("-SNAPSHOT/"))) {
                        // directory
                        if (!parent.hasParent())
                            progressMonitor.subTask("Scanning " + link);
                        Folder child = new Folder(link.substring(0, link.length() - 1));
                        if (link.matches("[0-9.]*/"))
                            parent.addChild(0, child);
                        else
                            parent.addChild(child);
                        httpHelper = null;
                        crawlForPackageFiles(child);
                        if (!parent.hasParent()) // topLevel
                            progressMonitor.worked(80 / topLevelFolders);
                    }
                    else if (link.endsWith(".xml") || link.endsWith(".json")) {
                        // XML or JSON file
                        File child = new File(parent, link);
                        parent.addChild(0, child);
                        child.setUrl(new URL(getFullUrl(child)));
                    }
                }
            }
            catch (InterruptedException iex) {
                throw iex;
            }
            catch (Exception ex) {
                throw new DiscoveryException("Error crawling: " + parentUrl, ex);
            }
        }
    }

    public String getFullUrl(Folder folder) {
        String fullUrl = folder.getName();
        StringBuilder folderName;
        Folder parent;
        Folder lFolder = folder;
        while ((parent = lFolder.getParent()) != null) {
            if (parent.getName().endsWith("/"))
                folderName = new StringBuilder(parent.getName()).append(fullUrl);
            else
                folderName = new StringBuilder(parent.getName()).append("/").append(fullUrl);
            lFolder = parent;
            fullUrl = folderName.toString();
        }
        return fullUrl;
    }

    public String getFullUrl(File file) {
        String fullUrl = getFullUrl((Folder) file.getParent());
        if (fullUrl.endsWith("/"))
            fullUrl += file.getName();
        else
            fullUrl += "/" + file.getName();
        return fullUrl;
    }

    /**
     * Use SAX for quick processing.
     */
    private List<String> parseDirectoryResponse(String content)
            throws IOException, SAXException, ParserConfigurationException {
        final List<String> urls = new ArrayList<>();
        InputStream xmlStream = new ByteArrayInputStream(content.getBytes());
        InputSource src = new InputSource(xmlStream);
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(src, new DefaultHandler() {
            boolean inHtml;
            boolean inUl;
            boolean inLi;
            boolean inA;
            String a;
            String href;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attrs)
                    throws SAXException {
                if (qName.equals("html"))
                    inHtml = true;
                else if (qName.equals("ul") || qName.equals("tr")) // New Apache
                                                                   // format
                                                                   // uses "tr"
                                                                   // and "td",
                                                                   // instead of
                                                                   // "ul" and
                                                                   // "li"
                                                                   // elements
                    inUl = true;
                else if (qName.equals("li") || qName.equals("td"))
                    inLi = true;
                else if (qName.equals("a"))
                    inA = true;

                if (inHtml && inUl && inLi && inA && href == null)
                    href = attrs.getValue("href");
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {

                if (qName.equals("a") && inHtml && inUl && inLi && href != null
                        && (href.equals(a) || href.equals(a + "/")
                                || href.substring(href.lastIndexOf("/", href.length() - 2) + 1)
                                        .equals(a))) {
                    if (!href.startsWith("/") && !href.startsWith("../")) // parent
                                                                          // directories
                        urls.add(href);
                    else if (!href.substring(href.lastIndexOf("/", href.length() - 2) + 1)
                            .startsWith("/")) // For new Apache directory
                                              // listing format
                        urls.add(href.substring(href.lastIndexOf("/", href.length() - 2) + 1));
                }

                if (qName.equals("html"))
                    inHtml = false;
                else if (qName.equals("ul") || qName.equals("tr"))
                    inUl = false;
                else if (qName.equals("li") || qName.equals("td"))
                    inLi = false;
                else if (qName.equals("a")) {
                    inA = false;
                    href = null;
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                if (inHtml && inUl && inLi && inA)
                    a = new String(ch).substring(start, start + length).trim();
                else
                    a = null;
            }
        });

        return urls;
    }

    private void parseJsonPacakges(Folder parent, String content) throws DiscoveryException {
        try {
            JSONArray jsonArray = (JSONArray) (new JSONObject(content)).get("packages");
            TreeMap<String, List<String>> sortedPackages = new TreeMap<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = new JSONObject(jsonArray.get(i).toString());
                String artifactName = jsonObj.getString("name");
                if (jsonObj.has("artifact"))
                    artifactName = jsonObj.getString("artifact");
                else
                    artifactName = artifactName.replace("com.centurylink.mdw.","").replace('.', '-');
                List<String> packages;
                if (sortedPackages.get(artifactName) == null)
                    packages = new ArrayList<>();
                else
                    packages = sortedPackages.get(artifactName);
                packages.add(jsonObj.getString("name") + " v" + jsonObj.getString("version"));
                sortedPackages.put(artifactName, packages);
            }
            for (Map.Entry<String, List<String>> entry : sortedPackages.entrySet()) {
                Folder artifact = new Folder(entry.getKey());
                for (String pkgName : entry.getValue()) {
                    File pkg = new File(artifact, pkgName);
                    artifact.addChild(pkg);
                }
                parent.addChild(artifact);
            }
        }
        catch (Exception ex) {
            throw new DiscoveryException("Error parsing the pacakges: ", ex);
        }
    }
}
