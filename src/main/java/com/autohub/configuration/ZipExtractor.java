package com.autohub.configuration;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZipExtractor {

    public void unzip(
            MultipartFile zipFile,
            String destDir)
            throws IOException {

        File dir = new File(destDir);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        ZipInputStream zis =
                new ZipInputStream(zipFile.getInputStream());

        ZipEntry entry;

        while ((entry = zis.getNextEntry()) != null) {

            File newFile =
                    new File(destDir, entry.getName());

            if (entry.isDirectory()) {

                newFile.mkdirs();

            } else {

                new File(newFile.getParent()).mkdirs();

                Files.copy(
                        zis,
                        newFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }

            zis.closeEntry();
        }

        zis.close();
    }
}
