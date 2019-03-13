package cn.echocow.soloauto.verticle;

import cn.echocow.soloauto.util.ConfigInfo;
import cn.echocow.soloauto.util.Constant;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;

/**
 * 版本检查
 *
 * @author Echo
 * @version 1.0
 * @date 2019-03-06 10:39
 */
public class WebClientVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebClientVerticle.class);
  private WebClient webClient;
  private LocalMap<String, String> solo;

  @Override
  public void start() {
    SharedData sd = vertx.sharedData();
    solo = sd.getLocalMap(Constant.SOLO.getValue());
    webClient = WebClient.create(vertx);
    check();
    vertx.setPeriodic(Duration.ofHours(Integer.parseInt(solo.get(ConfigInfo.INTERVAL.getValue()))).toMillis(), handle -> check());
  }


  /**
   * 检查更新
   */
  private void check() {
    String url = config().getString(ConfigInfo.LATEST_URL.getValue(), "https://api.github.com/repos/b3log/solo/releases/latest");
    webClient.getAbs(url)
      .as(BodyCodec.jsonObject())
      .send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<JsonObject> response = ar.result();
          @Nullable JsonObject body = response.body();
          if (!solo.get(ConfigInfo.VERSION.getValue()).equalsIgnoreCase(
            Objects.requireNonNull(body).getString(Constant.TAG_NAME.getValue()))) {
            LOGGER.info("0. Has new version...");
            sendMessage(body);
          } else {
            LOGGER.info("0. Now version is " + body.getString(Constant.TAG_NAME.getValue()) + ", local version is " + solo.get("version") + ".No change.");
          }
        } else {
          LOGGER.error("Error get releases latest:" + ar.cause().getMessage());
          ar.cause().printStackTrace();
        }
      });
  }

  /**
   * EventBus
   *
   * @param body 数据
   */
  private void sendMessage(JsonObject body) {
    vertx.eventBus().<JsonObject>send(FileVerticle.class.getName(),
      body.getJsonArray("assets").getJsonObject(0).put(Constant.TAG_NAME.getValue(), body.getString(Constant.TAG_NAME.getValue())),
      new DeliveryOptions().setSendTimeout(
        Duration.ofSeconds(Integer.parseInt(solo.get(ConfigInfo.TIME_OUT.getValue())) * 10).toMillis()),
      asyncResult -> {
        if (asyncResult.succeeded() && asyncResult.result().body().getInteger("code") == 1) {
          LOGGER.info("Update succeeded!");
        } else {
          LOGGER.error("Message get error. Please check your network! The reason for this error may be that it takes more time to download new version file.", asyncResult.cause());
        }
      });
  }

}
