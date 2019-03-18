package cn.echocow.soloauto.util;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;

/**
 * @author Echo
 * @version 1.2
 * @date 2019-03-07 14:53
 */
public class WarUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(WarUtils.class);


  /**
   * 解压 war 包
   *
   * @param warPath   war 包路径
   * @param unzipPath 解压的路径
   */
  public static void unwar(String warPath, String unzipPath) {
    File warFile = new File(warPath);
    try {
      BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(warFile));
      ArchiveInputStream in = new ArchiveStreamFactory()
        .createArchiveInputStream(ArchiveStreamFactory.JAR, bufferedInputStream);
      JarArchiveEntry entry;
      while ((entry = (JarArchiveEntry) in.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          new File(unzipPath, entry.getName()).mkdir();
        } else {
          OutputStream out = FileUtils.openOutputStream(new File(unzipPath, entry.getName()));
          IOUtils.copy(in, out);
          out.close();
        }
      }
      in.close();
      LOGGER.info("Success unwar:  " + warPath);
    } catch (FileNotFoundException e) {
      LOGGER.error("No found war file: " + warPath);
    } catch (ArchiveException e) {
      LOGGER.error("Unsupported compression format.");
    } catch (IOException e) {
      LOGGER.error("File write error");
    }
  }

}
