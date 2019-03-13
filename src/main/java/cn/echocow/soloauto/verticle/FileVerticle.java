package cn.echocow.soloauto.verticle;

import cn.echocow.soloauto.util.ConfigInfo;
import cn.echocow.soloauto.util.Constant;
import cn.echocow.soloauto.util.ExecuteCommand;
import cn.echocow.soloauto.util.WarUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import javax.annotation.Nullable;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 文件处理
 *
 * @author Echo
 * @version 1.0
 * @date 2019-03-06 10:39
 */
public class FileVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileVerticle.class);
  private static final String NAME = "name";
  private WebClient webClient;
  private FileSystem fileSystem;
  private LocalMap<String, String> solo;
  private String newVersionFileName;
  private WebClientOptions options = new WebClientOptions()
    .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36")
    .setKeepAlive(true)
    .setDefaultHost("github.com");

  @Override
  public void start(Future<Void> startFuture) {
    SharedData sd = vertx.sharedData();
    solo = sd.getLocalMap(Constant.SOLO.getValue());
    MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(FileVerticle.class.getName());
    fileSystem = vertx.fileSystem();
    webClient = WebClient.create(vertx, options);

    consumer.handler(message -> {
      JsonObject body = message.body();
      LOGGER.info("1. New version is " + body.getString(NAME));
      newVersionFileName = body.getString(NAME);
      update(message, body);
    });
  }


  private void update(Message message, JsonObject body) {
    webClient.getAbs(body.getString(Constant.BROWSER_DOWNLOAD_URL.getValue()))
      .timeout(Duration.ofSeconds(Integer.parseInt(solo.get(ConfigInfo.TIME_OUT.getValue()))).toMillis())
      .send(download -> {
        if (download.succeeded()) {
          LOGGER.info("2. Get new version successed");
          @Nullable Buffer buffer = download.result().bodyAsBuffer();
          createFileDir();
          fileSystem.writeFile(newVersionFilePath(), buffer, result -> {
            if (result.succeeded()) {
              debugInfo();
              LOGGER.info("3. Download new version " + newVersionFilePath() + " succeeded!");
              WarUtils.unwar(newVersionFilePath(), newVersionDir());
              moveOtherFiles();
              Future<Void> copyLocal = Future.future();
              Future<Void> copyLatke = Future.future();
              Future<Void> copySolo = Future.future();
              fileSystem.deleteBlocking(Constant.FILE_LOCAL.getFile(newVersionDir()));
              fileSystem.deleteBlocking(Constant.FILE_LATKE.getFile(newVersionDir()));
              fileSystem.deleteBlocking(Constant.FILE_SOLO.getFile(newVersionDir()));
              fileSystem.copy(Constant.FILE_LOCAL.getFile(lastVersionDir()), Constant.FILE_LOCAL.getFile(newVersionDir()), copyLocal.completer());
              fileSystem.copy(Constant.FILE_LATKE.getFile(lastVersionDir()), Constant.FILE_LATKE.getFile(newVersionDir()), copyLatke.completer());
              fileSystem.copy(Constant.FILE_SOLO.getFile(lastVersionDir()), Constant.FILE_SOLO.getFile(newVersionDir()), copySolo.completer());
              CompositeFuture.all(copyLocal, copyLatke, copySolo).setHandler(ar -> {
                if (ar.succeeded() && startSolo()) {
                  solo.put("version", body.getString(Constant.TAG_NAME.getValue()));
                  message.reply(new JsonObject().put("code", 1));
                } else {
                  errorHandle(ar, "Failed ! The old version " + lastVersionDir() + " to new version " + newVersionDir(), message);
                }
              });
            } else {
              errorHandle(result, "Download new version " + newVersionFilePath() + " failed!", message);
            }
          });
        } else {
          errorHandle(download, "Error get new download:" + download.cause().getMessage(), message);
        }
      });

  }

  /**
   * 启动 solo
   *
   * @return 启动结果
   */
  private boolean startSolo() {
    if (isTomcat()) {
      return tomcatHandle();
    } else {
      return soloHandle();
    }
  }

  /**
   * tomcat 部署下处理器
   *
   * @return 处理结果
   */
  private boolean tomcatHandle() {
    if (fileSystem.existsBlocking(lastVersionDir())) {
      fileSystem.moveBlocking(lastVersionDir(), homeDir() + tomcatDir() + LocalDateTime.now());
    }
    fileSystem.moveBlocking(newVersionDir(), lastVersionDir());
    return fileSystem.existsBlocking(lastVersionDir());
  }

  /**
   * 执行 杀死 —— 启动
   *
   * @return 执行结果
   */
  private boolean soloHandle() {
    if (!ExecuteCommand.killSolo()) {
      LOGGER.error("E. Kill last solo failed!");
      return false;
    }
    LOGGER.info("-. Kill last solo succeeded!");
    if (ExecuteCommand.commandRun(startCommand())) {
      LOGGER.info("4. Success ! The old version " + lastVersionDir() + " to new version " + newVersionDir());
      return true;
    }
    return false;
  }

  /**
   * 移动配置文件中的其它文件
   */
  private void moveOtherFiles() {
    config().getJsonArray(ConfigInfo.OTHER_FILES.getValue(), new JsonArray()).stream()
      .forEach(file -> {
        if (fileSystem.existsBlocking(newVersionDir() + file)) {
          fileSystem.deleteBlocking(newVersionDir() + file);
        }
        fileSystem.copyBlocking(lastVersionDir() + file, newVersionDir() + file);
        LOGGER.info("-. Copy other file: " + lastVersionDir() + file + " to " + newVersionDir() + file);
      });
  }

  /**
   * 创建文件路径, 阻塞
   */
  private void createFileDir() {
    if (fileSystem.existsBlocking(newVersionDir())) {
      fileSystem.deleteRecursiveBlocking(newVersionDir(), true);
      LOGGER.info("-. Exist " + newVersionDir() + ", delete them success!");
    }
    fileSystem.mkdirsBlocking(newVersionDir());
  }

  /**
   * 启动命令
   *
   * @return 命令
   */
  private String startCommand() {
    return Constant.startSolo(newVersionDir(), config().getString(ConfigInfo.START_COMMAND.getValue(), null));
  }

  /**
   * 获取 home 文件路径
   *
   * @return home 路径
   */
  private String homeDir() {
    return config().getString(ConfigInfo.HOME_DIR.getValue());
  }

  /**
   * 获取 tomcat 下 solo 文件路径
   *
   * @return home 路径
   */
  private String tomcatDir() {
    return config().getString(ConfigInfo.TOMCAT_DIR.getValue());
  }

  /**
   * 前一个版本文件路径
   *
   * @return 文件路径
   */
  private String lastVersionDir() {
    String fileName;
    if (isTomcat()) {
      fileName = tomcatDir();
    } else {
      fileName = "solo-" + solo.get(ConfigInfo.VERSION.getValue());
    }
    return homeDir() + fileName + File.separator;
  }

  /**
   * 是否是 tomcat 方式部署
   *
   * @return 结果
   */
  private boolean isTomcat() {
    return Constant.isTomcat(config().getString(ConfigInfo.DEPLOY.getValue()));
  }

  /**
   * 新的版本的文件路径
   *
   * @return 文件
   */
  private String newVersionFilePath() {
    return newVersionDir() + File.separator + newVersionFileName;
  }

  /**
   * 新版本存放文件夹
   *
   * @return 文件夹
   */
  private String newVersionDir() {
    return homeDir() + newVersionFileName.substring(0, newVersionFileName.lastIndexOf(".")) + File.separator;
  }

  /**
   * 错误处理
   *
   * @param asyncResult 异步结果
   * @param info        信息
   * @param message     返回
   */
  private void errorHandle(AsyncResult asyncResult, String info, Message message) {
    LOGGER.error("E: " + info + asyncResult.cause().getMessage());
    message.reply(new JsonObject().put("code", 0));
    asyncResult.cause().printStackTrace();
  }

  private void debugInfo() {
    debug("startCommand —— " + startCommand());
    debug("homeDir —— " + homeDir());
    debug("tomcatDir —— " + tomcatDir());
    debug("isTomcat —— " + isTomcat());
    debug("newVersionDir —— " + newVersionDir());
    debug("newVersionFilePath —— " + newVersionFilePath());
  }

  /**
   * 随意处理下 debug 信息
   *
   * @param message 信息
   */
  private void debug(String message) {
    if (Boolean.parseBoolean(solo.get(ConfigInfo.DEBUG.getValue()))) {
      LOGGER.info("Debug: " + message);
    }
  }
}
