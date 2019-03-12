package cn.echocow.soloauto.Verticle;

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
          fileSystem.writeFile(newVersionPath(), buffer, result -> {
            if (result.succeeded()) {
              LOGGER.info("3. Download new version " + newVersionPath() + " succeeded!");
              WarUtils.unwar(newVersionPath(), fileDir());
              moveOtherFiles();
              Future<Void> copyLocal = Future.future();
              Future<Void> copyLatke = Future.future();
              Future<Void> copySolo = Future.future();
              fileSystem.deleteBlocking(Constant.FILE_LOCAL.getFile(fileDir()));
              fileSystem.deleteBlocking(Constant.FILE_LATKE.getFile(fileDir()));
              fileSystem.deleteBlocking(Constant.FILE_SOLO.getFile(fileDir()));
              fileSystem.copy(Constant.FILE_LOCAL.getFile(lastVersionDir()), Constant.FILE_LOCAL.getFile(fileDir()), copyLocal.completer());
              fileSystem.copy(Constant.FILE_LATKE.getFile(lastVersionDir()), Constant.FILE_LATKE.getFile(fileDir()), copyLatke.completer());
              fileSystem.copy(Constant.FILE_SOLO.getFile(lastVersionDir()), Constant.FILE_SOLO.getFile(fileDir()), copySolo.completer());
              CompositeFuture.all(copyLocal, copyLatke, copySolo).setHandler(ar -> {
                if (ar.succeeded() && executeCommand()) {
                  solo.put("version", body.getString(Constant.TAG_NAME.getValue()));
                  message.reply(new JsonObject().put("code", 1));
                } else {
                  errorHandle(ar, "Failed ! The old version " + lastVersionDir() + " to new version " + fileDir(), message);
                }
              });
            } else {
              errorHandle(result, "Download new version " + newVersionPath() + " failed!", message);
            }
          });
        } else {
          errorHandle(download, "Error get new download:" + download.cause().getMessage(), message);
        }
      });

  }

  /**
   * 执行 杀死 —— 启动
   *
   * @return 执行结果
   */
  private boolean executeCommand() {
    if (!ExecuteCommand.killSolo()) {
      LOGGER.error("E. Kill last solo failed!");
      return false;
    }
    LOGGER.info("-. Kill last solo succeeded!");
    if (ExecuteCommand.commandRun(startCommand())) {
      LOGGER.info("4. Success ! The old version " + lastVersionDir() + " to new version " + fileDir());
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
        if (fileSystem.existsBlocking(fileDir() + file)) {
          fileSystem.deleteBlocking(fileDir() + file);
        }
        fileSystem.copyBlocking(lastVersionDir() + file, fileDir() + file);
        LOGGER.info("-. Copy other file: " + lastVersionDir() + file + " to " + fileDir() + file);
      });
  }

  /**
   * 创建文件路径, 阻塞
   */
  private void createFileDir() {
    if (fileSystem.existsBlocking(fileDir())) {
      fileSystem.deleteRecursiveBlocking(fileDir(), true);
      LOGGER.info("-. Exist " + fileDir() + ", delete them success!");
    }
    fileSystem.mkdirsBlocking(fileDir());
  }

  /**
   * 启动命令
   *
   * @return 命令
   */
  private String startCommand() {
    return Constant.startSolo(fileDir(), config().getString(ConfigInfo.START_COMMAND.getValue(), null));
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
   * 前一个版本文件路径
   *
   * @return 文件路径
   */
  private String lastVersionDir() {
    return homeDir() + "solo-" + solo.get(ConfigInfo.VERSION.getValue()) + File.separator;
  }

  /**
   * 新的版本的文件路径
   *
   * @return 文件
   */
  private String newVersionPath() {
    return fileDir() + File.separator + newVersionFileName;
  }

  /**
   * 存放文件夹
   *
   * @return 文件夹
   */
  private String fileDir() {
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
}
