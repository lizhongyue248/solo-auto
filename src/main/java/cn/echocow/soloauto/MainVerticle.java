package cn.echocow.soloauto;

import cn.echocow.soloauto.verticle.FileVerticle;
import cn.echocow.soloauto.verticle.WebClientVerticle;
import cn.echocow.soloauto.util.ConfigInfo;
import cn.echocow.soloauto.util.Constant;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;

import java.io.File;

import static io.vertx.core.spi.resolver.ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME;

/**
 * 主入口
 *
 * @author Echo
 * @version 1.2
 * @date 2019-03-18 10:39
 */
public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  private JsonObject fileConfig = new JsonObject();

  @Override
  public void start(Future<Void> startFuture) {
    System.getProperties().setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, "true");
    ConfigStoreOptions fileStore = new ConfigStoreOptions()
      .setType("file")
      .setConfig(new JsonObject().put("path", "config.json"));
    ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore);
    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    retriever.getConfig(ar -> {
      if (ar.succeeded()) {
        fileConfig = ar.result().mergeIn(config());
        shareDate();
        fileConfig.stream().forEach(entity -> LOGGER.info("Read configuration : " + entity.getKey() + " —— " + entity.getValue()));
        LOGGER.info("Tip. Your configuration will override the default configuration");
        Future<String> webClientFuture = Future.future();
        Future<String> fileFuture = Future.future();
        vertx.deployVerticle(WebClientVerticle.class.getName(), webDeploymentOptions(), webClientFuture.completer());
        vertx.deployVerticle(FileVerticle.class.getName(), fileDeploymentOptions(), fileFuture.completer());
        CompositeFuture.all(webClientFuture, fileFuture).setHandler(res -> {
          if (res.succeeded()) {
            LOGGER.info("All verticle deploy succeeded!");
            startFuture.complete();
          } else {
            LOGGER.info("Some verticle deploy failed!" + res.cause().getMessage());
            startFuture.fail(res.cause());
          }
        });
      } else {
        LOGGER.error("The configuration file: config.json does not exist or in wrong format, start failed!");
        startFuture.fail(ar.cause());
      }
    });
  }

  /**
   * web 部署参数
   *
   * @return DeploymentOptions
   */
  private DeploymentOptions webDeploymentOptions() {
    return new DeploymentOptions().setConfig(new JsonObject()
      .put(ConfigInfo.LATEST_URL.getValue(), fileConfig.getString(ConfigInfo.LATEST_URL.getValue())));
  }

  /**
   * file 部署参数
   *
   * @return DeploymentOptions
   */
  private DeploymentOptions fileDeploymentOptions() {
    String homeDir = fileConfig.getString(ConfigInfo.HOME_DIR.getValue());
    if (!homeDir.endsWith(File.separator)) {
      homeDir += File.separator;
    }
    String startCommand = fileConfig.getString(ConfigInfo.START_COMMAND.getValue(), null);
    if (startCommand != null && !startCommand.startsWith(";")) {
      startCommand = ";" + startCommand;
    }
    if (Constant.isTomcat(fileConfig.getString(ConfigInfo.DEPLOY.getValue()))) {
      LOGGER.info("Deploy method: Tomcat");
    } else {
      LOGGER.info("Deploy method: Solo");
    }
    return new DeploymentOptions().setConfig(new JsonObject()
      .put(ConfigInfo.HOME_DIR.getValue(), homeDir)
      .put(ConfigInfo.START_COMMAND.getValue(), startCommand)
      .put(ConfigInfo.OTHER_FILES.getValue(), fileConfig.getJsonArray(ConfigInfo.OTHER_FILES.getValue()))
      .put(ConfigInfo.DEPLOY.getValue(), fileConfig.getString(ConfigInfo.DEPLOY.getValue()))
      .put(ConfigInfo.TOMCAT_DIR.getValue(), fileConfig.getString(ConfigInfo.TOMCAT_DIR.getValue()))
      .put(ConfigInfo.DEBUG.getValue(), fileConfig.getBoolean(ConfigInfo.DEBUG.getValue())));
  }

  /**
   * 共享数据处理
   */
  private void shareDate() {
    LocalMap<String, String> solo = vertx.sharedData().getLocalMap(Constant.SOLO.getValue());
    solo.put(ConfigInfo.VERSION.getValue(), fileConfig.getString(ConfigInfo.VERSION.getValue()));
    solo.put(ConfigInfo.INTERVAL.getValue(), fileConfig.getInteger(ConfigInfo.INTERVAL.getValue()).toString());
    solo.put(ConfigInfo.TIME_OUT.getValue(), fileConfig.getInteger(ConfigInfo.TIME_OUT.getValue()).toString());
  }
}
