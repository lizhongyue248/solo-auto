package cn.echocow.soloauto.util;

/**
 * @author Echo
 * @version 1.0
 * @date 2019-03-11 15:11
 */
public enum ConfigInfo {
  /**
   * 家目录
   */
  HOME_DIR("homeDir"),
  /**
   * 其它需要移动的文件
   */
  OTHER_FILES("otherFiles"),
  /**
   * 自定义启动命令
   */
  START_COMMAND("startCommand"),
  /**
   * 当前已有的 solo 版本
   */
  VERSION("version"),
  /**
   * 多少小时检测一次
   */
  INTERVAL("interval"),
  /**
   * 请求超时时间设置
   */
  TIME_OUT("timeOut"),
  /**
   * 最新版本的下载路径
   */
  LATEST_URL("latestUrl");

  private String value;

  ConfigInfo(String s) {
    this.value = s;
  }

  public String getValue() {
    return this.value;
  }
}
