package net.huatech.autoconstruction;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class AutoConstruction extends AnAction {
    // 配置项
    // 1 tomcat
    String TOMCAT_NAME = "Tomcat 6.0.9";

    public AutoConstruction() {
        super("auto construct");
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getData(PlatformDataKeys.PROJECT);
        String basePath = project.getBasePath();

        // 生成page.iml
        createPageIml(basePath);

        // 修改HTEIP-page/build.properties
        createPageBuildProperties(basePath + File.separator + "HTEIP-page" + File.separator + "build.properties");

        // 重命名build.xml
        renameBuildXml(basePath + File.separator + "HTEIP-page");

        // 2 生成模块iml
        File baseFolder = new File(basePath);
        List<String> moduleFolders = new ArrayList<String>();
        if (baseFolder.exists()) {
            File[] folders = baseFolder.listFiles();
            for (File folder : folders) {
                if (folder.isDirectory()
                        && folder.getName().indexOf("HTEIP") == 0
                        && !folder.getName().equals("HTEIP-jar")
                        && !folder.getName().equals("HTEIP-page")) {
                    createModuleIml(basePath, folder.getName());
                    createBin(folder.getPath());
                    createModuleBuildProperties(folder.getPath() + File.separator + "build.properties");
                    renameBuildXml(folder.getPath());

                    moduleFolders.add(folder.getName());
                }
            }
        }

        // 创建artifacts
        createArtifacts(basePath);

        // 创建libraries
        createLibraries(basePath);

        // 创建ant.xml
        createAntXml(basePath, moduleFolders);

        // 创建misc.xml
        createMiscXml(basePath);

        // 创建modules.xml
        createModulesXml(basePath, moduleFolders);

        // 创建vcs.xml
        createVcsXml(basePath);

        // 创建workspace.xml
//        createWorkspaceXml(basePath);


        // 修复web.xml
        repairWebXml(basePath);

        // 解压zip
        unzipJars(basePath);
    }


    /**
     * 生成page.iml文件
     *
     * @param basePath 项目路径
     * @throws JDOMException
     * @throws IOException
     */
    private void createPageIml(String basePath) {
        try {
            URL pageImlTpl = getClass().getResource("/tpl/page-iml.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(pageImlTpl);
            Element rootElement = document.getRootElement();
            List<Element> components = rootElement.getChildren();
            for (Element component : components) {
                if (!"NewModuleRootManager".equals(component.getAttributeValue("name"))) {
                    continue;
                }

                List<Element> orderEntrys = component.getChildren("orderEntry");
                for (Element orderEntry : orderEntrys) {
                    if (!"library".equals(orderEntry.getAttributeValue("type"))
                            || !"application_server_libraries".equals(orderEntry.getAttributeValue("level"))) {
                        continue;
                    }

                    orderEntry.setAttribute("name", TOMCAT_NAME);
                }
            }

            FileWriter fileWriter = new FileWriter(basePath + File.separator + "HTEIP-page" + File.separator + "HTEIP-page.iml");
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(document, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createPageBuildProperties(String path) {
        try {
            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write("eip.web.path=WebRoot" + System.lineSeparator() + "eip.jar.path=../HTEIP-jar" + System.lineSeparator() + "init.modules=all");
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renameBuildXml(String path) {
        try {
            File file = new File(path + File.separator + "build.xml");
            if (file.exists()) {
                String target = path + File.separator + new File(file.getParent()).getName() + "-build.xml";
                file.renameTo(new File(target));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createModuleIml(String basePath, String module) {
        try {
            URL moduleImlTpl = getClass().getResource("/tpl/moudle-iml.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(moduleImlTpl);
            Element rootElement = document.getRootElement();
            Element component = rootElement.getChild("component");

            List<Element> orderEntrys = component.getChildren("orderEntry");
            for (Element orderEntry : orderEntrys) {
                if (!"library".equals(orderEntry.getAttributeValue("type"))
                        || !"application_server_libraries".equals(orderEntry.getAttributeValue("level"))) {
                    continue;
                }

                orderEntry.setAttribute("name", TOMCAT_NAME);
            }

            FileWriter fileWriter = new FileWriter(basePath + File.separator + module + File.separator + module + ".iml");
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(document, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createBin(String path) {
        try {
            File file = new File(path + File.separator + "bin");
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createModuleBuildProperties(String path) {
        try {
            StringBuffer stringBuffer = new StringBuffer();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
            String string = null;
            while ((string = bufferedReader.readLine()) != null) {
                if (string.indexOf("eip.web.path=") == 0) {
                    stringBuffer.append("eip.web.path=../HTEIP-page/WebRoot" + System.lineSeparator());
                    continue;
                }
                if (string.indexOf("eip.jar.path=") == 0) {
                    stringBuffer.append("eip.jar.path=../HTEIP-jar" + System.lineSeparator());
                    continue;
                }
                if (string.indexOf("module.webroot.path=") == 0) {
                    stringBuffer.append("module.webroot.path=WebRoot" + System.lineSeparator());
                    continue;
                }

                stringBuffer.append(string + System.lineSeparator());
            }
            bufferedReader.close();

            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write(stringBuffer.toString());
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createArtifacts(String basePath) {
        try {
            File artifacts = new File(basePath + File.separator + ".idea" + File.separator + "artifacts");
            if (!artifacts.exists()) {
                artifacts.mkdir();
            }

            URL artifactsTpl = getClass().getResource("/tpl/artifacts.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(artifactsTpl);

            FileWriter fileWriter = new FileWriter(artifacts.getPath() + File.separator + "HTEIP_page_war_exploded.xml");
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(document, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createLibraries(String basePath) {
        try {
            File libraries = new File(basePath + File.separator + ".idea" + File.separator + "libraries");
            if (!libraries.exists()) {
                libraries.mkdir();
            }

            URL pageLibTpl = getClass().getResource("/tpl/page-lib.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(pageLibTpl);

            FileWriter fileWriter = new FileWriter(libraries.getPath() + File.separator + "page_lib.xml");
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(document, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createAntXml(String basePath, List<String> moduleFolders) {
        try {
            URL antTpl = getClass().getResource("/tpl/ant.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(antTpl);
            Element rootElement = document.getRootElement();
            Element component = rootElement.getChild("component");

            for (String folder : moduleFolders) {
                Element buildFile = new Element("buildFile");
                buildFile.setAttribute("url", "file://$PROJECT_DIR$/" + folder + "/" + folder + "-build.xml");
                component.addContent(buildFile);
            }

            FileWriter fileWriter = new FileWriter(basePath + File.separator + ".idea"
                    + File.separator + "ant.xml");
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(document, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createMiscXml(String basePath) {
        try {
            URL miscTpl = getClass().getResource("/tpl/misc.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(miscTpl);

            FileWriter fileWriter = new FileWriter(basePath + File.separator + ".idea"
                    + File.separator + "misc.xml");
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(document, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createModulesXml(String basePath, List<String> moduleFolders) {
        try {
            URL modulesTpl = getClass().getResource("/tpl/modules.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(modulesTpl);
            Element rootElement = document.getRootElement();
            Element component = rootElement.getChild("component");
            Element modules = component.getChild("modules");

            for (String folder : moduleFolders) {
                Element buildFile = new Element("module");
                buildFile.setAttribute("fileurl", "file://$PROJECT_DIR$/" + folder + "/" + folder + ".iml");
                buildFile.setAttribute("filepath", "$PROJECT_DIR$/" + folder + "/" + folder + ".iml");
                modules.addContent(buildFile);
            }

            FileWriter fileWriter = new FileWriter(basePath + File.separator + ".idea"
                    + File.separator + "modules.xml");
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(document, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createVcsXml(String basePath) {
        try {
            URL miscTpl = getClass().getResource("/tpl/vcs.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(miscTpl);

            FileWriter fileWriter = new FileWriter(basePath + File.separator + ".idea"
                    + File.separator + "vcs.xml");
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(document, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createWorkspaceXml(String basePath) {
        try {
            File projectFolder = new File(basePath);
            String projectName = projectFolder.getName();

            URL tomcatTpl = getClass().getResource("/tpl/tomcat.xml");
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(tomcatTpl);
            Element rootElement = document.getRootElement();
            Element configuration = rootElement.getChild("configuration");
            Element serverSettings = configuration.getChild("server-settings");
            Element option = serverSettings.getChild("option");
            option.setAttribute("value", projectName);

            File workspace = new File(basePath + File.separator + ".idea" + File.separator + "workspace.xml");
            SAXBuilder workspaceSaxBuilder = new SAXBuilder();
            Document workspaceDocument = workspaceSaxBuilder.build(workspace);
            Element workspaceRootElement = workspaceDocument.getRootElement();
            Element runManager = (Element) XPath.selectSingleNode(workspaceRootElement, "//component[@name='RunManager']");
            runManager.setAttribute("selected", "Tomcat Server.tomcat6");
            runManager.addContent(configuration.clone());
            workspaceRootElement.removeChildren("component");
            workspaceRootElement.addContent(runManager.clone());

            FileWriter fileWriter = new FileWriter(basePath + File.separator + ".idea" + File.separator + "workspace2.xml");
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.output(workspaceDocument, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void repairWebXml(String basePath) {
        try {
            File webXml = new File(basePath + File.separator + "HTEIP-page" + File.separator + "WebRoot"
                    + File.separator + "WEB-INF" + File.separator + "web.xml");
            FileInputStream fileInputStream = new FileInputStream(webXml);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();
            String temp = null;
            while ((temp = bufferedReader.readLine()) != null) {
                if (temp.indexOf("<url-pattern>*</url-pattern>") != -1) {
                    temp = "<url-pattern>/*</url-pattern>";
                }

                stringBuffer.append(temp);
                stringBuffer.append(System.lineSeparator());
            }
            bufferedReader.close();


            FileOutputStream fileOutputStream = new FileOutputStream(basePath + File.separator + "HTEIP-page" + File.separator + "WebRoot"
                    + File.separator + "WEB-INF" + File.separator + "web.xml");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
            outputStreamWriter.append(stringBuffer);
            outputStreamWriter.flush();
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unzipJars(String basePath) {
        try {
            /*File source = new File(basePath + File.separator + "HTEIP-jar");
            File[] jars = source.listFiles();
            File target = new File(basePath + File.separator + "HTEIP-page" + File.separator + "WebRoot");

            for (File file : jars) {
               unzip(file.getPath(), target.getPath());
            }*/

            /*File buildXml = new File(basePath + File.separator + "HTEIP-page" + File.separator + "HTEIP-page-build.xml");
            org.apache.tools.ant.Project project = new org.apache.tools.ant.Project();
            project.fireBuildStarted();
            project.init();
            ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
            projectHelper.parse(project, buildXml);
            project.executeTarget(project.getDefaultTarget());
            project.fireBuildFinished(null);*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
