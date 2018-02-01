package net.huatech.autoconstruction;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import net.huatech.autoconstruction.utils.Config;
import net.huatech.autoconstruction.utils.SXml;
import org.jdom.Element;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
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
        copyXml(getClass().getResource("/tpl/artifacts.xml"),
                basePath + File.separator + ".idea" + File.separator + "artifacts" + File.separator + "HTEIP_page_war_exploded.xml");

        // 创建libraries
        copyXml(getClass().getResource("/tpl/page-lib.xml"),
                basePath + File.separator + ".idea" + File.separator + "libraries" + File.separator + "page_lib.xml");

        // 创建ant.xml
        createAntXml(basePath, moduleFolders);

        // 创建misc.xml
        copyXml(getClass().getResource("/tpl/misc.xml"),
                basePath + File.separator + ".idea" + File.separator + "misc.xml");

        // 创建modules.xml
        createModulesXml(basePath, moduleFolders);

        // 创建vcs.xml
        copyXml(getClass().getResource("/tpl/vcs.xml"),
                basePath + File.separator + ".idea" + File.separator + "vcs.xml");

        // 修复web.xml
        repairWebXml(basePath);

        // 解压zip
        unzipJars(basePath);
    }


    private void createPageIml(String basePath) {
        try {
            // 若HTEIP-page.iml存在，则不创建
            File pageIml = new File(basePath + File.separator + "HTEIP-page" + File.separator + "HTEIP-page.iml");
            if (pageIml.exists()) {
                return;
            }

            SXml sXml = new SXml(getClass().getResource("/tpl/page-iml.xml"));
            sXml.querySelector("//orderEntry[@type='library'][@level='application_server_libraries']")
                    .setAttribute("name", Config.getTomcatName());
            sXml.save(pageIml);
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
                file.renameTo(new File(path + File.separator + file.getParentFile().getName() + "-build.xml"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createModuleIml(String basePath, String module) {
        try {
            // 若module.iml存在，则不创建
            File moduleIml = new File(basePath + File.separator + module + File.separator + module + ".iml");
            if (moduleIml.exists()) {
                return;
            }

            SXml sXml = new SXml(getClass().getResource("/tpl/moudle-iml.xml"));
            sXml.querySelector("//orderEntry[@type='library'][@level='application_server_libraries']")
                    .setAttribute("name", Config.getTomcatName());
            sXml.save(basePath + File.separator + module + File.separator + module + ".iml");
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
                if (string.indexOf("eip.web.path") != -1) {
                    stringBuffer.append("eip.web.path=../HTEIP-page/WebRoot" + System.lineSeparator());
                    continue;
                }
                if (string.indexOf("eip.jar.path") != -1) {
                    stringBuffer.append("eip.jar.path=../HTEIP-jar" + System.lineSeparator());
                    continue;
                }
                if (string.indexOf("module.webroot.path") != -1) {
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

    private void copyXml(URL source, String target) {
        try {
            File file = new File(target);
            if (file.exists()) {
                return;
            }

            new SXml(source).save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createAntXml(String basePath, List<String> moduleFolders) {
        try {
            // 若ant.xml已存在则不重复创建
            File antXml = new File(basePath + File.separator + ".idea" + File.separator + "ant.xml");
            if (antXml.exists()) {
                return;
            }

            // 创建ant.xml
            SXml sXml = new SXml(getClass().getResource("/tpl/ant.xml"));
            Element component = sXml.querySelector("//component");
            for (String folder : moduleFolders) {
                component.addContent(new Element("buildFile")
                        .setAttribute("url", "file://$PROJECT_DIR$/" + folder + "/" + folder + "-build.xml"));
            }
            sXml.save(antXml);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createModulesXml(String basePath, List<String> moduleFolders) {
        try {
            File modulesXml = new File(basePath + File.separator + ".idea" + File.separator + "modules.xml");
            if (modulesXml.exists()) {
                return;
            }

            SXml sXml = new SXml(getClass().getResource("/tpl/modules.xml"));
            Element modules = sXml.querySelector("//modules");
            for (String folder : moduleFolders) {
                modules.addContent(new Element("module")
                        .setAttribute("fileurl", "file://$PROJECT_DIR$/" + folder + "/" + folder + ".iml")
                        .setAttribute("filepath", "$PROJECT_DIR$/" + folder + "/" + folder + ".iml"));
            }
            sXml.save(modulesXml);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void repairWebXml(String basePath) {
        try {
            File webXml = new File(basePath + File.separator + "HTEIP-page" + File.separator + "WebRoot"
                    + File.separator + "WEB-INF" + File.separator + "web.xml");
            SXml sXml = new SXml(webXml);
            sXml.querySelector("//url-pattern[text()='*']").setText("/*");
            sXml.save(webXml);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unzipJars(String basePath) {
        try {
            // todo 重新实现
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
