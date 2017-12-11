package droidefense.handler;


import droidefense.exception.ConfigFileNotFoundException;
import droidefense.sdk.helpers.DroidDefenseEnvironmentConfig;
import droidefense.sdk.log4j.Log;
import droidefense.sdk.log4j.LoggerType;
import droidefense.sdk.system.SystemCallReturn;
import droidefense.sdk.helpers.InternalConstant;
import droidefense.sdk.helpers.Util;
import droidefense.sdk.model.base.DroidefenseProject;
import droidefense.sdk.model.io.AbstractHashedFile;
import droidefense.social.RemoteFileDownloader;
import droidefense.util.JsonStyle;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by sergio on 16/2/16.
 */
public class FileIOHandler {

    private static DroidDefenseEnvironmentConfig environmentConfig;
    private static RemoteFileDownloader remoteDownloader = new RemoteFileDownloader();

    public static void init(){
        try {
            environmentConfig = DroidDefenseEnvironmentConfig.getInstance();
        } catch (ConfigFileNotFoundException e) {
            Log.write(LoggerType.FATAL, "could not read external configuration file");
        }
    }

    public static File getUnpackOutputFile() {
        return new File(environmentConfig.UNPACK_FOLDER);
    }

    public static File getBaseDirFile() {
        File base = new File("");
        return new File(base.getAbsolutePath());
    }

    public static File getUnpackOutputFile(AbstractHashedFile source) {
        return new File(getUnpackOutputFile() + File.separator + source.getSha256());
    }

    public static InputStream getApkrFileInputStream(String name) throws FileNotFoundException {
        if(!name.contains(File.separator))
            return new FileInputStream(environmentConfig.RESOURCE_FOLDER + File.separator + name);
        else
            return new FileInputStream(name);
    }

    public static File getResourceFolder() {
        return new File(
                getBaseDirPath() + File.separator +
                        environmentConfig.RESOURCE_FOLDER);
    }

    public static File getResourceFolder(String path) {
        return new File(getResourceFolder().getAbsolutePath() + File.separator + path);
    }

    public static ObjectInputStream getResourceObjectStream(String name) throws NullPointerException, IOException {
        return new ObjectInputStream(getApkrFileInputStream(name));
    }

    public static File getStaticPluginsFolderFile() {
        return new File(getResourceFolder() + File.separator +
                environmentConfig.STATIC_PLG_FOLDER);
    }

    public static File getDynamicPluginsFolderFile() {
        return new File(getResourceFolder() + File.separator +
                environmentConfig.DYNAMIC_PLG_FOLDER);
    }

    public static File getPluginsFile(String pluginName) {
        return new File(getPluginsFolderPath() + File.separator + pluginName);
    }

    public static String getBaseDirPath() {
        return getBaseDirFile().getAbsolutePath();
    }

    public static String getUnpackOutputPath(AbstractHashedFile source) {
        return getUnpackOutputFile(source).getAbsolutePath();
    }

    public static String getPluginsFolderPath() {
        return getStaticPluginsFolderFile().getAbsolutePath();
    }

    public static String getPluginsPath(String pluginName) {
        return getPluginsFile(pluginName).getAbsolutePath();
    }

    public static String getProjectFolderPath(DroidefenseProject project) {
        return getUnpackOutputPath(project.getSample());
    }

    public static byte[] readBytes(AbstractHashedFile file) throws IOException {
        return Files.readAllBytes(Paths.get(file.getAbsolutePath()));
    }

    public static SystemCallReturn callSystemExec(String command) throws IOException {

        if (InternalConstant.IS_WINDOWS_HOST)
            return new SystemCallReturn();

        Process p = null;
        SystemCallReturn ret = new SystemCallReturn();
        String[] calls = command.split(";");

        for (String cmd : calls) {
            // run the command using the Runtime run method:
            p = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            String s, answer = "", err = "";
            while ((s = stdInput.readLine()) != null) {
                answer += s;
            }

            //clean answer
            int idx = answer.indexOf(":");
            answer = answer.substring(idx + 1).trim();

            // read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                err += s;
            }
            stdError.close();
            stdInput.close();

            ret.addAnswer(answer);
            ret.addError(err);
            ret.addCommand(cmd);
        }
        //destroy process
        if (p != null)
            p.destroy();
        //return result;
        return ret;
    }

    public static void saveProjectReport(DroidefenseProject project) {

        String name, data, outputPath;

        outputPath = FileIOHandler.getUnpackOutputPath(project.getSample());

        //REPORT

        //OPCODES DATA

        //save opcodes count as json
        //save project
        name = "opcodes" + ".json";
        data = project.getOpCodeStats();
        boolean savedOpcodes = saveFile(name, outputPath, data, "Could not save .json opcodes");

        //NORMAL FLOWMAP

        //save call graph as dot format
        name = "normal-graphviz" + ".dot";
        data = project.getNormalControlFlowMap().getAsDotGraph();
        data = cleanDot(data);
        boolean savedNormalDot = saveFile(name, outputPath, data, "Could not save .dot normal flowmap");

        //save call graph as .json
        //save project
        name = "normal-flowmap" + ".json";
        data = Util.toJson(project.getNormalControlFlowMap(), JsonStyle.JSON_BEAUTY);
        boolean savedNormalFlowmap = saveFile(name, outputPath, data, "Could not save .json normal flowmap");

        //REFLECTED FLOWMAP

        //save call graph as dot format
        name = "reflected-graphviz" + ".dot";
        data = project.getReflectedFlowMap().getAsDotGraph();
        data = cleanDot(data);
        boolean savedReflectedDot = saveFile(name, outputPath, data, "Could not save .dot reflected flowmap");

        //save call graph as .json
        //save project
        name = "reflected-flowmap" + ".json";
        data = Util.toJson(project.getReflectedFlowMap(), JsonStyle.JSON_BEAUTY);
        boolean savedReflectedFlowmap = saveFile(name, outputPath, data, "Could not save .json reflected flowmap");

        //FOLLOW DalvikInstruction FLOWMAP

        //save call graph as dot format
        name = "follow-graphviz" + ".dot";
        data = project.getFollowCallsMap().getAsDotGraph();
        data = cleanDot(data);
        boolean followDot = saveFile(name, outputPath, data, "Could not save .dot follow flowmap");

        //save call graph as .json
        //save project
        name = "follow-flowmap" + ".json";
        data = Util.toJson(project.getFollowCallsMap(), JsonStyle.JSON_BEAUTY);
        boolean followJson = saveFile(name, outputPath, data, "Could not save .json follow flowmap");

        //save report as json
        //save project
        name = "report" + ".json";
        data = project.getProjectAsJson();
        boolean savedReport = saveFile(name, outputPath, data, "Could not save .json report");

    }

    private static String cleanDot(String data) {
        data = data.replace("new <init>", "new()");
        data = data.replace("new <clinit>", "new()");
        return data;
    }

    private static boolean saveFile(String name, String outpath, String data, String msg) {
        try {
            FileOutputStream out = new FileOutputStream(outpath + File.separator + name);
            out.write(data.getBytes());
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.write(LoggerType.FATAL, msg, e, e.getStackTrace());
        }
        return false;
    }

    public static String getWebsiteContent(String urlString) {
        if(urlString!=null){
            try {
                return remoteDownloader.downloadFileFromUrlUsingNio(urlString);
            } catch (MalformedURLException e) {
                Log.write(LoggerType.ERROR, "Could not retrieve Google Play Store information", e.getLocalizedMessage());
            }
        }
        return "";
    }

    public static File getDecompiledPath(DroidefenseProject currentProject) {
        return new File(getUnpackOutputPath(currentProject.getSample()) + File.separator + "code");
    }

    public static String getUploadsFolderPath() {
        return getUploadsFolder().getAbsolutePath();
    }

    public static File getUploadsFolder() {
        File f = new File(environmentConfig.UPLOAD_FOLDER);
        if (!f.exists())
            f.mkdirs();
        return f;
    }

    public static void saveFile(String folder, String filename, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(folder + File.separator + filename);
            fos.write(data);
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void saveFile(File f, byte[] data) {
        try {
            createParentFolder(f);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(data);
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void createParentFolder(File f) {
        File parent = f.getParentFile();
        if(!parent.exists()){
            parent.mkdirs();
        }
    }

    public static void saveFile(File f, String data) {
        saveFile(f, data.getBytes());
    }

    public static void saveFile(String name, String data) {
        saveFile(new File(name), data.getBytes());
    }

    public static void saveAsRAW(Object o, String name, File outputDir) throws IOException {
        if (!outputDir.exists())
            outputDir.mkdirs();
        FileOutputStream fout = new FileOutputStream(outputDir.getAbsolutePath() + File.separator + name);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(o);
        oos.close();
    }

    public static Object readAsRAW(File file) throws IOException, ClassNotFoundException {
        FileInputStream fout = new FileInputStream(file);
        ObjectInputStream oos = new ObjectInputStream(fout);
        return oos.readObject();
    }

    public static Object readAsRAW(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        return ois.readObject();
    }

    public static File getRuleEngineDir() {
        return new File(
                getResourceFolder() + File.separator +
                        environmentConfig.RULE_FOLDER);
    }

    public static File getToolsDir() {
        return new File(
                getResourceFolder() + File.separator +
                environmentConfig.RESOURCE_FOLDER + File.separator +
                        "tools");
    }

    public static File getModelsDir() {
        return new File(
                getResourceFolder() + File.separator +
                        environmentConfig.MODEL_FOLDER);
    }

    public static File getReportFolder(String projectId) {
        String reportFolderPath = environmentConfig.STATIC_REPORT_FOLDER;
        builDir(reportFolderPath);
        return new File(reportFolderPath + File.separator + projectId);
    }

    private static boolean builDir(String path) {
        File dir = new File(path);
        if(!dir.exists()){
            return dir.mkdirs();
        }
        return true;
    }

    public static String getConfigPath() {
        return  getBaseDirPath()+File.separator+InternalConstant.CONFIG_FOLDER;
    }

    public static File getConfigPathDir() {
        return new File( getConfigPath() );
    }
}
