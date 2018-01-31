package net.huatech.autoconstruction.utils;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class SXmlTest {
    private static SXml sXml;

    static {
        try {
            sXml = new SXml("C:\\workspace\\repositories\\HtAutoConstruction\\resources\\tpl\\page-iml.xml");
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void querySelector() {
        Element element = null;
        try {
            element = sXml.querySelector("//orderEntry[@type='library'][@level='application_server_libraries']");
        } catch (JDOMException e) {
            e.printStackTrace();
        }

        assertNotNull(element);
        assertEquals("orderEntry", element.getName());
        assertEquals("library", element.getAttributeValue("type"));
        assertEquals("PROVIDED", element.getAttributeValue("scope"));
        assertEquals("Tomcat 6.0.9", element.getAttributeValue("name"));
        assertEquals("application_server_libraries", element.getAttributeValue("level"));
    }

    @Test
    public void querySelectorAll() {
        List<Element> elements = null;
        try {
            elements = sXml.querySelectorAll("//component//orderEntry");
        } catch (JDOMException e) {
            e.printStackTrace();
        }

        assertNotNull(elements);
        assertEquals(4, elements.size());
    }

    @Test
    public void saveToPath() {
        String tmpdir = System.getProperty("java.io.tmpdir");
        String filePath = tmpdir + File.separator + "saveToPath.xml";
        try {
            sXml.save(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File file = new File(filePath);
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(file.isFile());
    }

    @Test
    public void saveToFile() {
        String tmpdir = System.getProperty("java.io.tmpdir");
        String filePath = tmpdir + File.separator + "saveToPath.xml";
        File before = new File(filePath);
        try {
            sXml.save(before);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File after = new File(filePath);
        assertNotNull(after);
        assertTrue(after.exists());
        assertTrue(after.isFile());
    }
}