package cn.echocow.soloauto.util;

import java.io.File;

/**
 * @author Echo
 * @version 1.0
 * @date 2019-03-07 16:31
 */
public enum Constant {
  /**
   * local.properties
   */
  FILE_LOCAL(File.separator + "WEB-INF" + File.separator + "classes" + File.separator + "local.properties"),
  /**
   * latke.properties
   */
  FILE_LATKE(File.separator + "WEB-INF" + File.separator + "classes" + File.separator + "latke.properties"),
  /**
   * solo.properties
   */
  FILE_SOLO(File.separator + "WEB-INF" + File.separator + "classes" + File.separator + "solo.properties"),
  /**
   * 启动_linux
   */
  COMMAND_START_SOLO_LINUX(";nohup java -cp WEB-INF/lib/*:WEB-INF/classes org.b3log.solo.Starter >/dev/null 2>&1 &"),
  /**
   * 停止_windows
   */
  COMMAND_START_SOLO_WINDOWS(""),
  /**
   * 切换目录_linux
   */
  COMMAND_CHANGE_DIR_LINUX("cd "),
  /**
   * 切换目录_windows
   */
  COMMAND_CHANGE_DIR_WINDOWS(""),
  /**
   * 下载链接
   */
  BROWSER_DOWNLOAD_URL("browser_download_url"),
  /**
   * 默认家目录
   */
  DEFAULT_HOME_DIR_LINUX("/root/"),
  /**
   * solo
   */
  SOLO("solo"),
  /**
   * 版本名称
   */
  TAG_NAME("tag_name");

  private String value;

  Constant(String s) {
    this.value = s;
  }

  public String getFile(String parent) {
    if (!parent.endsWith(File.separator)) {
      parent += File.separator;
    }
    return parent + this.value;
  }

  public String getValue() {
    return this.value;
  }

  /**
   * 启动 solo 命令
   *
   * @param path 路径
   * @param command 命令
   * @return 结果
   */
  public static String startSolo(String path, String command) {
    if (isWindows()) {
      return startSoloWindows(path, command);
    } else {
      return startSoloLinux(path, command);
    }
  }

  /**
   * windows 版本 solo
   *
   * @param path 路径
   * @param command 命令
   * @return 结果
   */
  private static String startSoloWindows(String path, String command) {
    if (command == null) {
      command = COMMAND_START_SOLO_WINDOWS.value;
    }
    return COMMAND_CHANGE_DIR_WINDOWS.value + path + command;
  }

  /**
   * linux 版本 solo
   *
   * @param path 路径
   * @param command 命令
   * @return 结果
   */
  private static String startSoloLinux(String path, String command) {
    if (command == null) {
      command = COMMAND_START_SOLO_LINUX.value;
    }
    return COMMAND_CHANGE_DIR_LINUX.value + path + command;
  }

  /**
   * 判断是否为 windows
   *
   * @return 是否是 windows
   */
  public static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("windows");
  }

}
