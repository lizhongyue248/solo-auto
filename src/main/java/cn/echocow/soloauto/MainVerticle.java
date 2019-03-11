package cn.echocow.soloauto;

import cn.echocow.soloauto.Verticle.FileVerticle;
import cn.echocow.soloauto.Verticle.WebClientVerticle;
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

import static io.vertx.core.spi.resolver.ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  private JsonObject fileConfig = new JsonObject();


  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class.getName());
  }

  @Override
  public void start(Future<Void> startFuture) {
    System.getProperties().setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, "true");
    ConfigStoreOptions fileStore = new ConfigStoreOptions()
      .setType("file")
      .setConfig(new JsonObject().put("path", "config.json"));
    ConfigRetrieverOptions options = new ConfigRetrieverOptions()
      .addStore(fileStore);
    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

    retriever.getConfig(ar -> {
      if (ar.succeeded()) {
        fileConfig.mergeIn(ar.result());
        shareDate();
        Future<String> webClientFuture = Future.future();
        Future<String> fileFuture = Future.future();
        vertx.deployVerticle(WebClientVerticle.class.getName(), webDeploymentOptions(), webClientFuture.completer());
        vertx.deployVerticle(FileVerticle.class.getName(), fileDeploymentOptions(), fileFuture.completer());
        CompositeFuture.all(webClientFuture, fileFuture).setHandler(res -> {
          if (res.succeeded()) {
            LOGGER.info("All Verticle deploy succeeded!");
          } else {
            LOGGER.info("Some Verticle deploy failed!" + res.cause().getMessage());
            vertx.close();
          }
        });
      } else {
        LOGGER.error("The configuration file: config.json does not exist or in wrong format, start failed!");
        vertx.close();
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
      .put(ConfigInfo.LATEST_URL.getValue(), config().getString(ConfigInfo.LATEST_URL.getValue(), fileConfig.getString(ConfigInfo.LATEST_URL.getValue()))));
  }

  /**
   * file 部署参数
   *
   * @return DeploymentOptions
   */
  private DeploymentOptions fileDeploymentOptions() {
    return new DeploymentOptions().setConfig(new JsonObject()
      .put(ConfigInfo.HOME_DIR.getValue(), config().getString(ConfigInfo.HOME_DIR.getValue(), fileConfig.getString(ConfigInfo.HOME_DIR.getValue())))
      .put(ConfigInfo.OTHER_FILES.getValue(), config().getJsonArray(ConfigInfo.OTHER_FILES.getValue(), fileConfig.getJsonArray(ConfigInfo.OTHER_FILES.getValue())))
      .put(ConfigInfo.START_COMMAND.getValue(), config().getString(ConfigInfo.START_COMMAND.getValue(), fileConfig.getString(ConfigInfo.START_COMMAND.getValue()))));
  }

  /**
   * 共享数据处理
   */
  private void shareDate() {
    LocalMap<String, String> solo = vertx.sharedData().getLocalMap(Constant.SOLO.getValue());
    solo.put(ConfigInfo.VERSION.getValue(), config().getString(ConfigInfo.VERSION.getValue(), fileConfig.getString(ConfigInfo.VERSION.getValue())));
    solo.put(ConfigInfo.INTERVAL.getValue(), config().getLong(ConfigInfo.INTERVAL.getValue(), fileConfig.getLong(ConfigInfo.INTERVAL.getValue())).toString());
    solo.put(ConfigInfo.TIME_OUT.getValue(), config().getInteger(ConfigInfo.TIME_OUT.getValue(), fileConfig.getInteger(ConfigInfo.TIME_OUT.getValue())).toString());
  }

}
