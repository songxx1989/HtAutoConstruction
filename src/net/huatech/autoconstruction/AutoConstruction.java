package net.huatech.autoconstruction;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class AutoConstruction extends AnAction {
    public AutoConstruction() {
        super("auto construct");
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getData(PlatformDataKeys.PROJECT);
        String basePath = project.getBasePath();

        // 1 生成page.iml
        createPageIml(basePath);
        rewritePageBuildProperties(basePath + File.separator + "HTEIP-page" + File.separator + "build.properties");
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
                    rewriteModuleBuildProperties(folder.getPath() + File.separator + "build.properties");
                    renameBuildXml(folder.getPath());

                    moduleFolders.add(folder.getName());
                }
            }
        }

        // 3 创建artifacts
        createArtifacts(basePath);

        // 4 创建libraries
        createLibraries(basePath);

        // 5 创建ant.xml
        createAntXml(basePath, moduleFolders);

        // 6 改写misc.xml
        rewriteMiscXml(basePath);

        // 7 改写modules.xml
        rewriteModulesXml(basePath, moduleFolders);

        // 改写workspace.xml
        rewriteWorkspaceXml(basePath);

    }

    private void rewriteWorkspaceXml(String basePath) {
        try {
            Document config = new SAXReader().read(getClass().getResource("/config_template.xml"));
            Element tomcatConfig = config.elementByID("tomcatConfig");

            Document workspace = new SAXReader().read(new File(basePath + File.separator + ".idea" + File.separator + "workspace.xml"));
            List list = workspace.selectSingleNode("//component").getParent().elements();
            list.add(tomcatConfig);

            XMLWriter writer = null;
            SAXReader reader = new SAXReader();
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            writer = new XMLWriter(new FileWriter(new File(basePath + File.separator + ".idea" + File.separator + "workspace.xml")), format);
            writer.write(workspace);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createPageIml(String basePath) {
        try {
            XMLWriter writer = null;
            SAXReader reader = new SAXReader();

            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");

            String filePath = basePath + File.separator + "HTEIP-page" + File.separator + "HTEIP-page.iml";
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }

            Document _document = DocumentHelper.createDocument();
            Element _module = _document.addElement("module")
                    .addAttribute("type", "JAVA_MODULE")
                    .addAttribute("version", "4");

            Element _facetManager = _module.addElement("component")
                    .addAttribute("name", "FacetManager");

            Element _facet = _facetManager.addElement("facet")
                    .addAttribute("type", "web")
                    .addAttribute("name", "web");

            Element _configuration = _facet.addElement("configuration");
            _configuration.addElement("descriptors")
                    .addElement("deploymentDescriptor")
                    .addAttribute("name", "web.xml")
                    .addAttribute("url", "file://$MODULE_DIR$/WebRoot/WEB-INF/web.xml");
            _configuration.addElement("webroots")
                    .addElement("root")
                    .addAttribute("url", "file://$MODULE_DIR$/WebRoot")
                    .addAttribute("relative", "/");

            Element _newModuleRootManager = _module.addElement("component");
            _newModuleRootManager.addElement("output")
                    .addAttribute("url", "file://$MODULE_DIR$/WebRoot/WEB-INF/classes");
            _newModuleRootManager.addElement("exclude-output");
            _newModuleRootManager.addElement("content")
                    .addAttribute("url", "file://$MODULE_DIR$")
                    .addElement("sourceFolder")
                    .addAttribute("url", "file://$MODULE_DIR$/src")
                    .addAttribute("isTestSource", "false");

            _newModuleRootManager.addElement("orderEntry")
                    .addAttribute("type", "inheritedJdk");
            _newModuleRootManager.addElement("orderEntry")
                    .addAttribute("type", "sourceFolder")
                    .addAttribute("forTests", "false");
            _newModuleRootManager.addElement("orderEntry")
                    .addAttribute("type", "library")
                    .addAttribute("scope", "PROVIDED")
                    .addAttribute("name", "Tomcat 6.0.9") // todo 作为配置项
                    .addAttribute("level", "application_server_libraries");
            _newModuleRootManager.addElement("orderEntry")
                    .addAttribute("type", "library")
                    .addAttribute("name", "lib")
                    .addAttribute("level", "project");

            writer = new XMLWriter(new FileWriter(file), format);
            writer.write(_document);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createModuleIml(String basePath, String module) {
        try {
            XMLWriter writer = null;
            SAXReader reader = new SAXReader();

            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");

            String filePath = basePath + File.separator + module + File.separator + module + ".iml";
            File file = new File(filePath);
            Document _document = DocumentHelper.createDocument();

            Element _module = _document.addElement("module")
                    .addAttribute("type", "JAVA_MODULE")
                    .addAttribute("version", "4");

            Element _component = _module.addElement("component")
                    .addAttribute("name", "NewModuleRootManager");

            _component.addElement("output")
                    .addAttribute("url", "file://$MODULE_DIR$/bin");
            _component.addElement("exclude-output");
            _component.addElement("content")
                    .addAttribute("url", "file://$MODULE_DIR$")
                    .addElement("sourceFolder")
                    .addAttribute("url", "file://$MODULE_DIR$/src")
                    .addAttribute("isTestSource", "false");
            _component.addElement("orderEntry")
                    .addAttribute("type", "inheritedJdk");
            _component.addElement("orderEntry")
                    .addAttribute("type", "sourceFolder")
                    .addAttribute("forTests", "false");
            _component.addElement("orderEntry")
                    .addAttribute("type", "library")
                    .addAttribute("scope", "PROVIDED")
                    .addAttribute("name", "Tomcat 6.0.9")
                    .addAttribute("level", "application_server_libraries");
            _component.addElement("orderEntry")
                    .addAttribute("type", "library")
                    .addAttribute("name", "lib")
                    .addAttribute("level", "project");

            writer = new XMLWriter(new FileWriter(file), format);
            writer.write(_document);
            writer.close();
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

    private void rewritePageBuildProperties(String path) {
        try {
            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write("eip.web.path=WebRoot" + System.lineSeparator() + "eip.jar.path=../HTEIP-jar" + System.lineSeparator() + "init.modules=all");
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rewriteModuleBuildProperties(String path) {
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

    private void createArtifacts(String basePath) {
        try {
            File artifacts = new File(basePath + File.separator + ".idea" + File.separator + "artifacts");
            if (!artifacts.exists()) {
                artifacts.mkdir();
            }

            File artifactsXml = new File(artifacts.getPath() + File.separator + "HTEIP_page_war_exploded.xml");

            XMLWriter writer = null;
            SAXReader reader = new SAXReader();
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            Document _document = DocumentHelper.createDocument();

            Element _artifact = _document.addElement("component")
                    .addAttribute("name", "ArtifactManager")
                    .addElement("artifact")
                    .addAttribute("type", "exploded-war")
                    .addAttribute("name", "HTEIP-page:war exploded");

            _artifact.addElement("output-path")
                    .addText("$PROJECT_DIR$/HTEIP-page/WebRoot");

            Element _root = _artifact.addElement("root")
                    .addAttribute("id", "root");

            Element _element = _root.addElement("element")
                    .addAttribute("id", "directory")
                    .addAttribute("name", "WEB-INF");

            _element.addElement("element")
                    .addAttribute("id", "directory")
                    .addAttribute("name", "classes")
                    .addElement("element")
                    .addAttribute("id", "module-output")
                    .addAttribute("name", "HTEIP-page");
            _element.addElement("element")
                    .addAttribute("id", "directory")
                    .addAttribute("name", "lib")
                    .addElement("element")
                    .addAttribute("id", "library")
                    .addAttribute("level", "project")
                    .addAttribute("name", "lib");

            _root.addElement("element")
                    .addAttribute("id", "javaee-facet-resources")
                    .addAttribute("facet", "HTEIP-page/web/Web");

            writer = new XMLWriter(new FileWriter(artifactsXml), format);
            writer.write(_document);
            writer.close();

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

            File libXml = new File(libraries.getPath() + File.separator + "lib.xml");

            XMLWriter writer = null;
            SAXReader reader = new SAXReader();
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            Document _document = DocumentHelper.createDocument();

            Element _library = _document.addElement("component")
                    .addAttribute("name", "libraryTable")
                    .addElement("library")
                    .addAttribute("name", "lib");
            _library.addElement("CLASSES")
                    .addElement("root")
                    .addAttribute("url", "file://$PROJECT_DIR$/HTEIP-page/WebRoot/WEB-INF/lib");
            _library.addElement("JAVADOC");
            _library.addElement("SOURCES")
                    .addElement("root")
                    .addAttribute("url", "file://$PROJECT_DIR$/HTEIP-page/WebRoot/WEB-INF/lib");
            _library.addElement("jarDirectory")
                    .addAttribute("url", "file://$PROJECT_DIR$/HTEIP-page/WebRoot/WEB-INF/lib")
                    .addAttribute("recursive", "false");
            _library.addElement("jarDirectory")
                    .addAttribute("url", "file://$PROJECT_DIR$/HTEIP-page/WebRoot/WEB-INF/lib")
                    .addAttribute("recursive", "false")
                    .addAttribute("type", "SOURCES");

            writer = new XMLWriter(new FileWriter(libXml), format);
            writer.write(_document);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createAntXml(String basePath, List<String> moduleFolders) {
        try {
            File libXml = new File(basePath + File.separator + ".idea" + File.separator + "ant.xml");

            XMLWriter writer = null;
            SAXReader reader = new SAXReader();
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            Document _document = DocumentHelper.createDocument();

            Element _component = _document.addElement("project")
                    .addAttribute("version", "4")
                    .addElement("component")
                    .addAttribute("name", "AntConfiguration");

            _component.addElement("buildFile")
                    .addAttribute("url", "file://$PROJECT_DIR$/HTEIP-page/HTEIP-page-build.xml");

            for (String folder : moduleFolders) {
                _component.addElement("buildFile")
                        .addAttribute("url", "file://$PROJECT_DIR$/" + folder + "/" + folder + "-build.xml");
            }

            writer = new XMLWriter(new FileWriter(libXml), format);
            writer.write(_document);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rewriteMiscXml(String basePath) {
        try {
            File libXml = new File(basePath + File.separator + ".idea" + File.separator + "misc.xml");
            if (libXml.exists()) {
                libXml.delete();
            }

            XMLWriter writer = null;
            SAXReader reader = new SAXReader();
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            Document _document = DocumentHelper.createDocument();

            Element _project = _document.addElement("project")
                    .addAttribute("version", "4");

            _project.addElement("component")
                    .addAttribute("name", "ProjectRootManager")
                    .addAttribute("version", "2")
                    .addAttribute("languageLevel", "JDK_1_6")
                    .addAttribute("default", "false")
                    .addAttribute("project-jdk-name", "1.6")
                    .addAttribute("project-jdk-type", "JavaSDK")
                    .addElement("output")
                    .addAttribute("url", "file://$PROJECT_DIR$");

            _project.addElement("component")
                    .addAttribute("name", "SvnBranchConfigurationManager")
                    .addElement("option")
                    .addAttribute("name", "mySupportsUserInfoFilter")
                    .addAttribute("value", "true");

            writer = new XMLWriter(new FileWriter(libXml), format);
            writer.write(_document);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rewriteModulesXml(String basePath, List<String> moduleFolders) {
        try {
            File libXml = new File(basePath + File.separator + ".idea" + File.separator + "modules.xml");

            XMLWriter writer = null;
            SAXReader reader = new SAXReader();
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            Document _document = DocumentHelper.createDocument();

            Element _modules = _document.addElement("project")
                    .addAttribute("version", "4")
                    .addElement("component")
                    .addAttribute("name", "ProjectModuleManager")
                    .addElement("modules");

            _modules.addElement("module")
                    .addAttribute("fileurl", "file://$PROJECT_DIR$/HTEIP-page/HTEIP-page.iml")
                    .addAttribute("filepath", "$PROJECT_DIR$/HTEIP-page/HTEIP-page.iml");

            for (String folder : moduleFolders) {
                _modules.addElement("module")
                        .addAttribute("fileurl", "file://$PROJECT_DIR$/" + folder + "/" + folder + ".iml")
                        .addAttribute("filepath", "$PROJECT_DIR$/" + folder + "/" + folder + ".iml");
            }

            writer = new XMLWriter(new FileWriter(libXml), format);
            writer.write(_document);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
