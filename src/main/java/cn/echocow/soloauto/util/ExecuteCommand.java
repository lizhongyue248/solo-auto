package cn.echocow.soloauto.util;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;

/**
 * @author Echo
 * @version 1.2
 * @date 2019-03-06 13:50
 */
public class ExecuteCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteCommand.class);
  private static boolean IS_WINDOWS = Constant.isWindows();
  private static final String KILL_SOLO_LINUX = "pkill -9 -f org.b3log.solo.Starter";
  private static final String KILL_SOLO_WINDOWS = "pkill -9 -f org.b3log.solo.Starter";

  /**
   * 执行命令
   *
   * @param command 命令
   * @return 执行结果
   */
  public static boolean commandRun(String command) {
    if (IS_WINDOWS) {
      return Windows.commandRun(command);
    } else {
      return Linux.commandRun(command);
    }
  }

  /**
   * 杀死 solo
   *
   * @return 执行结果
   */
  public static boolean killSolo() {
    if (IS_WINDOWS) {
      return Windows.kill();
    } else {
      return Linux.kill();
    }
  }

  /**
   * Linux 系统处理
   */
  private static class Linux {
    /**
     * 执行命令 —— linux
     *
     * @param command 命令
     * @return 执行结果
     */
    private static boolean commandRun(String command) {
      LOGGER.info("-. Exec command: " + command);
      String[] cmd = {"-c", command};
      CommandLine convertCmd = new CommandLine("/bin/sh");
      convertCmd.addArguments(cmd, false);
      try {
        DefaultExecutor defaultExecutor = new DefaultExecutor();
        defaultExecutor.setExitValue(1);
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        defaultExecutor.execute(convertCmd, resultHandler);
        resultHandler.waitFor();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        LOGGER.error("Exception in commandRunLinux method.", e);
        return false;
      }
    }

    /**
     * linux 下杀死 solo
     *
     * @return 结果
     */
    private static boolean kill() {
      return commandRun(KILL_SOLO_LINUX);
    }
  }

  /**
   * Linux 系统处理
   */
  private static class Windows {
    /**
     * 执行命令 —— windows
     *
     * @param command 命令
     * @return 执行结果
     */
    private static boolean commandRun(String command) {
      return true;
    }


    /**
     * windows 下杀死 solo
     *
     * @return 结果
     */
    private static boolean kill() {
      return true;
    }
  }


}
