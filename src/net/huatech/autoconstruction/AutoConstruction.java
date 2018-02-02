package net.huatech.autoconstruction;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import net.huatech.autoconstruction.utils.Config;
import net.huatech.autoconstruction.utils.SXml;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class AutoConstruction extends AnAction {
    private String basePath = null;

    public AutoConstruction() {
        super("auto construct");
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        basePath = anActionEvent.getData(PlatformDataKeys.PROJECT).getBasePath();

        try {
            // page
            disposePage();

            // modules
            List<String> moduleFolders = disposeModule();

            // idea
            disposeIdea(moduleFolders);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JDOMException e) {
            e.printStackTrace();
        }
    }


    /**
     * 处理HTEIP-page，包括：
     * 1. 生成项目的iml文件
     * 2. 修正build.properties
     * 3. 重命名build.xml文件
     * 4. 修正web.xml
     */
    private void disposePage() throws IOException, JDOMException {
        createPageIml(basePath);
        createPageBuildProperties(basePath + File.separator + "HTEIP-page" + File.separator + "build.properties");
        renameBuildXml(basePath + File.separator + "HTEIP-page");
        repairWebXml(basePath);
    }


    /**
     * 处理各模块，包括：
     * 1. 生成模块iml文件
     * 2. 生成bin目录
     * 3. 修正build.properties文件
     * 4. 重命名build.xml文件
     *
     * @return
     */
    private List<String> disposeModule() throws IOException, JDOMException {
        File baseFolder = new File(basePath);
        List<String> moduleFolders = new ArrayList<String>();
        if (baseFolder.exists()) {
            File[] folders = baseFolder.listFiles();
            for (File folder : folders) {
                if (folder.isDirectory()
                        && folder.getName().indexOf("HTEIP") == 0
                        && !folder.getName().equals("HTEIP-jar")
                        && !folder.getName().equals("HTEIP-page")) {
                    createModuleIml(folder.getName());
                    createBin(folder.getPath());
                    createModuleBuildProperties(folder.getPath() + File.separator + "build.properties");
                    renameBuildXml(folder.getPath());

                    moduleFolders.add(folder.getName());
                }
            }
        }
        return moduleFolders;
    }


    /**
     * 处理idea配置文件，包括
     * 1. 打包配置文件
     * 2. lib配置文件
     * 3. ant脚本文件
     * 4. misc
     * 5. 模块配置文件
     * 6. 版本控制配置文件
     *
     * @param moduleFolders
     */
    private void disposeIdea(List<String> moduleFolders) throws JDOMException, IOException {
        copyXml(getClass().getResource("/tpl/artifacts.xml"),
                basePath + File.separator + ".idea" + File.separator + "artifacts" + File.separator + "HTEIP_page_war_exploded.xml");

        copyXml(getClass().getResource("/tpl/page-lib.xml"),
                basePath + File.separator + ".idea" + File.separator + "libraries" + File.separator + "page_lib.xml");

        createAntXml(moduleFolders);

        copyXml(getClass().getResource("/tpl/misc.xml"),
                basePath + File.separator + ".idea" + File.separator + "misc.xml");

        createModulesXml(moduleFolders);

        copyXml(getClass().getResource("/tpl/vcs.xml"),
                basePath + File.separator + ".idea" + File.separator + "vcs.xml");
    }

    private void createPageIml(String basePath) throws JDOMException, IOException {
        // 若HTEIP-page.iml存在，则不创建
        File pageIml = new File(basePath + File.separator + "HTEIP-page" + File.separator + "HTEIP-page.iml");

        SXml sXml = new SXml(getClass().getResource("/tpl/page-iml.xml"));
        sXml.querySelector("//orderEntry[@type='library'][@level='application_server_libraries']")
                .setAttribute("name", Config.getTomcatName());
        sXml.save(pageIml);
    }

    private void createPageBuildProperties(String path) throws IOException {
        FileWriter fileWriter = new FileWriter(path);
        fileWriter.write("eip.web.path=WebRoot" + System.lineSeparator() + "eip.jar.path=../HTEIP-jar" + System.lineSeparator() + "init.modules=all");
        fileWriter.close();
    }

    private void renameBuildXml(String path) {
        File file = new File(path + File.separator + "build.xml");
        if (file.exists()) {
            file.renameTo(new File(path + File.separator + file.getParentFile().getName() + "-build.xml"));
        }
    }

    private void createModuleIml(String module) throws IOException, JDOMException {
        // 若module.iml存在，则不创建
        File moduleIml = new File(basePath + File.separator + module + File.separator + module + ".iml");

        SXml sXml = new SXml(getClass().getResource("/tpl/moudle-iml.xml"));
        sXml.querySelector("//orderEntry[@type='library'][@level='application_server_libraries']")
                .setAttribute("name", Config.getTomcatName());
        sXml.save(basePath + File.separator + module + File.separator + module + ".iml");
    }

    private void createBin(String path) {
        File file = new File(path + File.separator + "bin");
        if (!file.exists()) {
            file.mkdir();
        }
    }

    private void createModuleBuildProperties(String path) throws IOException {
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
    }

    private void copyXml(URL source, String target) throws JDOMException, IOException {
        File file = new File(target);

        new SXml(source).save(file);
    }

    private void createAntXml(List<String> moduleFolders) throws JDOMException, IOException {
        // 若ant.xml已存在则不重复创建
        File antXml = new File(basePath + File.separator + ".idea" + File.separator + "ant.xml");

        // 创建ant.xml
        SXml sXml = new SXml(getClass().getResource("/tpl/ant.xml"));
        Element component = sXml.querySelector("//component");
        for (String folder : moduleFolders) {
            component.addContent(new Element("buildFile")
                    .setAttribute("url", "file://$PROJECT_DIR$/" + folder + "/" + folder + "-build.xml"));
        }
        sXml.save(antXml);
    }

    private void createModulesXml(List<String> moduleFolders) throws JDOMException, IOException {
        File modulesXml = new File(basePath + File.separator + ".idea" + File.separator + "modules.xml");

        SXml sXml = new SXml(getClass().getResource("/tpl/modules.xml"));
        Element modules = sXml.querySelector("//modules");
        for (String folder : moduleFolders) {
            modules.addContent(new Element("module")
                    .setAttribute("fileurl", "file://$PROJECT_DIR$/" + folder + "/" + folder + ".iml")
                    .setAttribute("filepath", "$PROJECT_DIR$/" + folder + "/" + folder + ".iml"));
        }
        sXml.save(modulesXml);
    }

    private void repairWebXml(String basePath) {
        try {
            /*File webXml = new File(basePath + File.separator + "HTEIP-page" + File.separator + "WebRoot"
                    + File.separator + "WEB-INF" + File.separator + "web.xml");
            SXml sXml = new SXml(webXml);
            sXml.querySelector("//url-pattern[text()='*']").setText("/*");
            sXml.save(webXml);*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
