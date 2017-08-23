package cn.weijie.asgard.tool;

import java.net.URL;

class PathUtils {

    private PathUtils() {}

    /**
     * "file:/home/weijie/cn/demo" -> "/home/weijie/cn/demo"
     * "jar:file:/home/weijie/foo.jar!cn/demo" -> "/home/weijie/foo.jar"
     */
    static String getRootPath(URL url) {
        String fileUrl = url.getFile();
        int pos = fileUrl.indexOf('!');

        if (-1 == pos) {
            return fileUrl;
        }

        return fileUrl.substring(5, pos);
    }

    /**
     * "cn.weijie.demo" -> "cn/weijie/demo"
     * @param name classpath
     * @return directory path
     */
    static String dotToSplash(String name) {
        return name.replaceAll("\\.", "/");
    }

    /**
     * "Apple.class" -> "Apple"
     */
    static String trimExtension(String name) {
        int pos = name.indexOf('.');
        if (-1 != pos) {
            return name.substring(0, pos);
        }

        return name;
    }

    /**
     * /application/home -> /home
     * @param uri origin uri
     * @return trimmed uri
     */
    static String trimURI(String uri) {
        String trimmed = uri.substring(1);
        int splashIndex = trimmed.indexOf('/');

        return trimmed.substring(splashIndex);
    }
}
