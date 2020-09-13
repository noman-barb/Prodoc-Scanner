package com.aaindia.prodocscanner.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.aaindia.prodocscanner.wrappers.ListFIlesInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newDirectoryStream;

public class FileNav {


    public static final String BASE_DIR = "Scans";

    public static final String DIR_IDENTIFIER = "_is_dir";
    public static final String SCAN_IDENTIFIER = "_is_scan";

    public static final String TAG_IDENTIFIER = "tag";

    public static final String ORIGINAL_IMAGE_DIR = "original";
    public static final String PROCESSED_IMAGE_DIR = "processed";
    public static final String OUTPUT_DIR = "output";
    public static final String ORDERING_FILE = "ordering.json";
    public static final String EFFECTS_FILE = "effects1.json";

    public static final int DIR_ALREADY_EXISTS = 0;
    public static final int DIR_NAME_INVALID = -1;
    public static final int DIR_CREATED = 1;
    public static final int DIR_NOT_CREATED = 2;
    private static final String DEFAULT_DIR_PREIX = "Prodoc Scan";
    public static final String PROCESSED_IMAGE_FILE = "processed_image_file";
    public static final String ORIGINAL_IMAGE_FILE = "original_image_file";


    public static void unZipAll(InputStream inputStream, File destination, OnZipProgress OnZipProgress) throws IOException {


        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {


            destination.getParentFile().mkdirs();


            // Process each entry
            ZipEntry entry = null;

            if (zipInputStream.available() < 1)
                return;


            while ((entry = zipInputStream.getNextEntry()) != null) {


                String currentEntry = entry.getName();


                File destFile = new File(destination, currentEntry);

                File destinationParent = destFile.getParentFile();

                // create the parent directory structure if needed
                destinationParent.mkdirs();


                long totalLen = 0;

                if (!entry.isDirectory()) {


                    FileOutputStream fos = null;

                    try {
                        fos = new FileOutputStream(destFile);


                        byte[] buffer = new byte[4096];
                        int len;

                        while ((len = zipInputStream.read(buffer)) != -1) {
                            totalLen += len;
                            fos.write(buffer, 0, len);
                        }

                        fos.close();


                        OnZipProgress.onProgress(totalLen);


                    } catch (Exception e) {

                    } finally {
                        if (fos != null) {
                            fos.close();
                        }

                        zipInputStream.closeEntry();

                    }
                } else {

                    destFile.mkdirs();
                }

            }
        } catch (Exception e) {

        } finally {

        }

    }


    public static void zipDir(String dir2zip, File base, ZipOutputStream zos) {


        zipDir(dir2zip, base, zos, true);
    }

    private static void zipDir(String dir2zip, File base, ZipOutputStream zos, boolean firstCall) {
        try {

            File zipDir = new File(dir2zip);


            //get a listing of the directory content
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            //loop through dirList, and zip the files
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    //if the File object is a directory, call this
                    //function again to add its content recursively
                    String filePath = f.getPath();
                    zipDir(filePath, base, zos, false);
                    //loop again
                    continue;
                }

                FileInputStream fis = new FileInputStream(f);

                String relativePath = base.toURI().relativize(f.toURI()).getPath();

                ZipEntry anEntry = new ZipEntry(relativePath);

                //place the zip entry in the ZipOutputStream object
                zos.putNextEntry(anEntry);
                //now write the content of the file to the ZipOutputStream


                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                //close the Stream
                fis.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public static File getEffectsFile(String scanDir) {

        return new File(scanDir, EFFECTS_FILE);

    }

    public static File getEffectsFile(File scanDir) {

        return new File(scanDir, EFFECTS_FILE);

    }


    public static File newOriginalImageFile(File scanDir) {

        String imageFileName = new Date().getTime() + ".jpg";

        File originalImageDir = getOriginalImageDir(scanDir);


        File imageFile = new File(originalImageDir, imageFileName);

        return imageFile;
    }

    public static HashMap<String, File> newImageFile(String scanDir) {

        String imageFileName = new Date().getTime() + ".jpg";

        File originalImageDir = getOriginalImageDir(new File(scanDir));

        File processedImageDir = getProcessedImageDir(new File(scanDir));


        File originalImageFile = new File(originalImageDir, imageFileName);
        File processedImageFile = new File(processedImageDir, imageFileName);

        HashMap<String, File> map = new HashMap<>();

        map.put(PROCESSED_IMAGE_FILE, processedImageFile);
        map.put(ORIGINAL_IMAGE_FILE, originalImageFile);
        return map;
    }


    public static File getOutputDir(String scanDirPath, int quality) {
        File f = new File(scanDirPath + File.separator + OUTPUT_DIR + File.separator + quality);
        f.mkdirs();
        return f;
    }

    public static String getPDFName(String scandirPath) {

        String n = new File(scandirPath).getName();

        //  return n;
        return n.substring(0, n.length() - SCAN_IDENTIFIER.length()) + ".pdf";
    }

    public static File getProcessedFileFromName(String scandir, String filename) {

        return new File(scandir + File.separator + PROCESSED_IMAGE_DIR + File.separator + filename);
    }


    public static File getOriginalImageDir(File scanDir) {

        File f = new File(scanDir, ORIGINAL_IMAGE_DIR);

        f.mkdirs();

        return f;
    }


    public static File getProcessedImageDir(File scanDir) {

        File f = new File(scanDir, PROCESSED_IMAGE_DIR);

        f.mkdirs();

        return f;
    }

    public static ArrayList<ListFIlesInfo> getDirInfo(String baseDir, String relativePath) {


        File baseList[] = FileNav.listDir(baseDir, relativePath);
        ArrayList<ListFIlesInfo> fIlesInfos = new ArrayList<>();


        for (File file : baseList) {

            String filename = file.getName();


            if (!isValidFile(file))
                continue;


            String filepath = file.getAbsolutePath();
            boolean isScan = FileNav.isScan(filename);
            long dateModified = file.lastModified();
            int numPages = 0;
            String thumbnailPath = null;

            if (isScan) {


                File original = new File(filepath + File.separator + ORIGINAL_IMAGE_DIR);


                String[] subFiles = original.list();

                if (subFiles == null)
                    numPages = 0;
                else
                    numPages = subFiles.length;


                filename = filename.substring(0, filename.length() - SCAN_IDENTIFIER.length());

                try {
                    thumbnailPath = new File(filepath + File.separator + PROCESSED_IMAGE_DIR).listFiles()[0].getAbsolutePath();
                } catch (Exception e) {

                    try {

                        thumbnailPath = new File(filepath + File.separator + ORIGINAL_IMAGE_DIR).listFiles()[0].getAbsolutePath();

                    } catch (Exception e2) {

                    }
                }


            } else {

                numPages = file.list().length;
                filename = filename.substring(0, filename.length() - DIR_IDENTIFIER.length());
            }


            if (numPages == 0 && isScan)
                continue;

            fIlesInfos.add(new ListFIlesInfo(filepath, filename, dateModified, numPages, isScan, thumbnailPath));

        }


        return fIlesInfos;
    }


    public static boolean rename(String fromFilePath, String fromFileName, String toNewFileName) {


        File dir = new File(fromFilePath + "/../");

        File from = new File(dir, fromFileName);

        File to = new File(dir, toNewFileName);

        if (to.exists()) {
            return false;
        }

        if (from.exists()) {
            from.renameTo(to);
        }

        return true;


    }


    public static boolean validFileName(String filename) {

        ArrayList<Character> illegalChars = new ArrayList<>();

        illegalChars.add('/');
        illegalChars.add('\\');
        illegalChars.add(':');
        illegalChars.add('*');
        illegalChars.add('\"');
        illegalChars.add('<');
        illegalChars.add('>');
        illegalChars.add('|');

        if (filename.length() == 0 || filename.replace(SCAN_IDENTIFIER, "").length() == 0 || filename.replace(DIR_IDENTIFIER, "").length() == 0)
            return false;


        for (Character s : illegalChars) {

            if (filename.indexOf(s) != -1)
                return false;
        }


        return true;

    }

    private static boolean isValidFile(File file) {

        try {

            String filename = file.getName();

            boolean a = isScan(filename);
            boolean b = isDir(filename);

            return a || b;
        } catch (Exception e) {
            return false;
        }


    }


    public static boolean isScan(String filename) {


        boolean isScan = filename.substring(filename.length() - SCAN_IDENTIFIER.length(), filename.length()).equals(SCAN_IDENTIFIER);

        return isScan;
    }

    public static boolean isDir(String filename) {


        boolean isDir = filename.substring(filename.length() - DIR_IDENTIFIER.length(), filename.length()).equals(DIR_IDENTIFIER);

        return isDir;
    }


    private static ArrayList<File> listScansRec(File baseDir, ArrayList<File> base) {


        File[] files = baseDir.listFiles();


        for (File f : files) {


            if (f.isDirectory()) {

                if (isScan(f.getName())) {
                    base.add(f);
                } else {
                    listScansRec(f, base);
                }
            }

        }


        return base;

    }

    public static ArrayList<ListFIlesInfo> listDirRec(File dirFile) {


        ArrayList<File> baseList = new ArrayList<>();

        ArrayList<ListFIlesInfo> fIlesInfos = new ArrayList<>();

        listScansRec(dirFile, baseList);


        for (File file : baseList) {


            String filename = file.getName();

            if (!isValidFile(file))
                continue;

            String filepath = file.getAbsolutePath();
            boolean isScan = FileNav.isScan(filename);
            long dateModified = file.lastModified();
            int numPages = 0;
            String thumbnailPath = null;

            if (isScan) {

                File processed = new File(filepath + File.separator + PROCESSED_IMAGE_DIR);

                if (processed.exists()) {

                    String[] subFiles = processed.list();

                    if (subFiles == null)
                        numPages = 0;
                    else
                        numPages = subFiles.length;

                } else {

                    File original = new File(filepath + File.separator + ORIGINAL_IMAGE_DIR);


                    String[] subFiles = original.list();

                    if (subFiles == null)
                        numPages = 0;
                    else
                        numPages = subFiles.length;

                }


                filename = filename.substring(0, filename.length() - SCAN_IDENTIFIER.length());

                try {
                    thumbnailPath = new File(filepath + File.separator + PROCESSED_IMAGE_DIR).listFiles()[0].getAbsolutePath();
                } catch (Exception e) {

                    try {

                        thumbnailPath = new File(filepath + File.separator + ORIGINAL_IMAGE_DIR).listFiles()[0].getAbsolutePath();
                    } catch (Exception e2) {
                    }

                }


            } else {

                numPages = file.list().length;
                filename = filename.substring(0, filename.length() - DIR_IDENTIFIER.length());
            }


            fIlesInfos.add(new ListFIlesInfo(filepath, filename, dateModified, numPages, isScan, thumbnailPath));

        }


        return fIlesInfos;
    }

    public static File[] listDir(String filepath, String relativePath) {


        File file2 = null;

        if (relativePath == null)
            file2 = new File(filepath);
        else
            file2 = new File(filepath + File.separator + relativePath);

        return file2.listFiles();
    }


    public static int createDir(String path, String dirName) {


        File file = new File(path, dirName + DIR_IDENTIFIER);


        if (!validFileName(dirName))
            return DIR_NAME_INVALID;

        if (file.exists())
            return DIR_ALREADY_EXISTS;


        if (file.mkdir())
            return DIR_CREATED;


        return DIR_NAME_INVALID;

    }

    public static void createBaseDir(Context context) {


        File file = getBaseDir(context);

        if (!file.exists())
            file.mkdirs();
    }

    public static boolean baseExists(Context context) {


        return getBaseDir(context).exists();
    }

    public static File originalScanDirFromScanDir(File dir) {


        File f = new File(dir + File.separator + ORIGINAL_IMAGE_DIR);
        f.mkdirs();
        return f;
    }


    public static float getFolderSizeMb(File f) {

        float d = 1024 * 1024f;

        float size = 0;

        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                size += getFolderSizeMb(file);
            }
        } else {
            size += f.length() / d;
        }
        return size;
    }

    public static File processedScanDirFromScanDir(File dir) {


        File f = new File(dir + File.separator + PROCESSED_IMAGE_DIR);
        f.mkdirs();
        return f;
    }

    public static File newScanDir(String path) {

        String date = new SimpleDateFormat("dd-MM-yy HH.mm.ss").format(System.currentTimeMillis());

        String dirName = DEFAULT_DIR_PREIX + " " + date + SCAN_IDENTIFIER;

        String fullPath = path + File.separator + dirName;

        File dir = new File(fullPath);

        dir.mkdir();

        new File(dir.getAbsolutePath() + File.separator + PROCESSED_IMAGE_DIR).mkdir();
        new File(dir.getAbsolutePath() + File.separator + ORIGINAL_IMAGE_DIR).mkdir();

        return dir;


    }


    public static File getTempFile(Context context, String tempFileName) {


        try {
            File.createTempFile(tempFileName, null, context.getCacheDir());

            File cacheFile = new File(context.getCacheDir(), tempFileName);

            return cacheFile;


        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;


    }

    public static File getBaseDir(Context context) {


        return new File(getRootDir(context), BASE_DIR);
    }

    public static File getRootDir(Context context) {

        return context.getExternalFilesDir(null);
    }


    public static void deleteDirectoryQuietely(File file) {


        try {

            FileUtils.deleteDirectory(file);
        } catch (Exception e) {
        }

    }


    public interface OnZipProgress {

        public void onProgress(long progress);
    }


}
