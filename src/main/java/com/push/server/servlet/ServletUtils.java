package com.push.server.servlet;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author linyh
 */
@Slf4j
public class ServletUtils {

    private static Properties webProperties = new Properties();

    private static Map<String, IServlet> servletMapping = new HashMap<String, IServlet>();

//    private static InputStream baseURL = ServletUtils.class.getResourceAsStream("");

    public static final String WEB_INF = "/WEB-INF";

    public static synchronized void init() throws IOException {

        if (AbstractServlet.successInit) {
            log.info("已经初始化过一次了,不要重复初始化servlet");
        }
        InputStream in = null;
        try {
            // 获取 resources/web.properties 下的文件流
            log.info("开启文件流");
//            FileInputStream fis = new FileInputStream(getResource("web.properties", ""));
            in = ServletUtils.class.getResourceAsStream("/web.properties");

            log.info("读取文件流");
            webProperties.load(in);

            for (Object k : webProperties.keySet()) {

                log.info("读取内容");
                String key = k.toString();
                if (key.endsWith(".url")) {
                    String servletName = key.replaceAll("\\.url$", "");
                    String url = webProperties.getProperty(key);
                    String className = webProperties.getProperty(servletName + ".className");
                    //单实例，多线程
                    IServlet obj = (IServlet) Class.forName(className).newInstance();
                    servletMapping.put(url, obj);
                }

            }
            if (servletMapping.size() == 0) {
                log.info("没有读取到servlet映射配置");
                throw new RuntimeException("没有读取到servlet映射配置");
            } else {
                AbstractServlet.successInit = true;
            }

        } catch (Exception e) {
            log.info("读取servlet配置文件失败: [{}]", e.getMessage());
            throw new RuntimeException("读取servlet配置文件失败");
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private static Map<String, File> fileMap = new ConcurrentHashMap<>();

    public static IServlet findUriMapping(String uri) {
        return servletMapping.get(uri);
    }

//    public static File getResource(String fileName, String prefix) throws Exception {
//
//        // 获取文件路径
////        String filePath = ServletUtils.class.getClassLoader().getResource(fileName).getPath();
//
//        String fileUtl = ServletUtils.class.getResource("/").getFile();
//
////        String basePath = baseURL.toURI().toString();
////        log.info("basePath: [{}]", basePath);
////        int start = basePath.indexOf("classes/");
////        basePath = (basePath.substring(0, start) + "/" + "classes/").replaceAll("/+", "/");
////
////        String path = basePath + prefix + "/" + fileName;
////        path = !path.contains("file:") ? path : path.substring(9);
////        path = path.replaceAll("//", "/");
////
////        log.info("读取路径为: [{}]", path);
////        return new File(path);
////        System.out.println(filePath);
//        System.out.println(fileUtl);
//        return null;
//    }

    public static File getResource(String fileName, String prefix) throws Exception {

        if (fileMap.containsKey(fileName)) {
            return fileMap.get(fileName);
        }

        if (!fileName.startsWith("/")) {
            fileName = "/" + fileName;
        }

        InputStream in = null;
        try {
            in = ServletUtils.class.getResourceAsStream(prefix + fileName);

            File file = createFile(in);

            File newFile = fileMap.putIfAbsent(fileName, file);

            if (newFile != null) {
                file = null;
                return newFile;
            } else {
                return file;
            }

        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

//    public static void main(String[] args) throws Exception {
////        getResource("chatIndex.html", "WEB-INF");
////        File file = new File("/tmp/fileDir/" + "chatIndex.html");
////
////        file.mkdir();
//
//        InputStream in = ServletUtils.class.getResourceAsStream("/WEB-INF/chatIndex.html");
//        createFile(in);
//    }

    public static File createFile(InputStream inputStream) throws IOException {
        File tmp = File.createTempFile("storeFile", ".tmp", new File("/tmp"));
        OutputStream os = new FileOutputStream(tmp);
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        while ((bytesRead = inputStream.read(buffer, 0, 8192)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        return tmp;
    }

}
