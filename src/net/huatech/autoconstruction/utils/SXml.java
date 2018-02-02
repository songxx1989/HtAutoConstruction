package net.huatech.autoconstruction.utils;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class SXml {
    SAXBuilder saxBuilder = new SAXBuilder();
    Document document;

    public SXml(String filePath) throws JDOMException, IOException {
        this(new File(filePath));
    }

    public SXml(File file) throws JDOMException, IOException {
        document = saxBuilder.build(file);
    }

    public SXml(URL url) throws JDOMException, IOException {
        document = saxBuilder.build(url);
    }


    /**
     * 查找单个元素
     *
     * @param selectors
     * @return
     * @throws JDOMException
     */
    public Element querySelector(String selectors) throws JDOMException {
        return (Element) XPath.selectSingleNode(document.getRootElement(), selectors);
    }


    /**
     * 查找多个元素
     *
     * @param selectors
     * @return
     * @throws JDOMException
     */
    public List<Element> querySelectorAll(String selectors) throws JDOMException {
        return (List<Element>) XPath.selectNodes(document.getRootElement(), selectors);
    }


    /**
     * 保存xml
     * @param targetPath
     * @throws IOException
     */
    public void save(String targetPath) throws IOException {
        save(new File(targetPath));
    }


    /**
     * 保存xml
     * @param targetFile
     * @throws IOException
     */
    public void save(File targetFile) throws IOException {
        File parentFile = targetFile.getParentFile();
        if(!parentFile.exists()){
            parentFile.mkdir();
        }

        FileWriter fileWriter = new FileWriter(targetFile);
        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.output(document, fileWriter);
    }
}
