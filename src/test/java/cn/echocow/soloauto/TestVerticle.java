package cn.echocow.soloauto;

import cn.echocow.soloauto.util.ExecuteCommand;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static io.vertx.core.spi.resolver.ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class TestVerticle {

  @Test
  @DisplayName("测试文件下载")
  @Timeout(value = 15, timeUnit = TimeUnit.HOURS)
  void downloadTest(Vertx vertx, VertxTestContext testContext) {
    System.getProperties().setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, "true");
    WebClient client = WebClient.create(vertx);
    client.getAbs("https://img.hacpai.com/bing/20180924.jpg")
      .timeout(10000000)
      .send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          @Nullable Buffer buffer = response.bodyAsBuffer();
          System.out.println("get file success!");
          vertx.fileSystem().mkdirs("test/test/", aar ->
            vertx.fileSystem().writeFile("test/test/test123.jpg", buffer, result -> {
            if (result.succeeded()) {
              System.out.println("success");
              testContext.completeNow();
            } else {
              System.err.println("error");
              testContext.failNow(result.cause());
            }
          }));
        } else {
          System.err.println("get file failed!");
          ar.cause().printStackTrace();
          testContext.failNow(ar.cause());
        }
      });
  }

  @Test
  @DisplayName("测试获取 solo 最新版本数据")
  @Timeout(value = 15, timeUnit = TimeUnit.SECONDS)
  void githubTest(Vertx vertx, VertxTestContext testContext) {
    WebClient client = WebClient.create(vertx);
    client.getAbs("https://api.github.com/repos/b3log/solo/releases/latest")
      .as(BodyCodec.jsonObject())
      .send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<JsonObject> response = ar.result();
          @Nullable JsonObject body = response.body();
          JsonObject assets = body.getJsonArray("assets").getJsonObject(0);
          System.out.println(body.encodePrettily());
          System.out.println(assets.encodePrettily());
          System.out.println(assets.getString("browser_download_url"));
          testContext.completeNow();
        } else {
          ar.cause().printStackTrace();
        }
      });
  }

  @Test
  @DisplayName("测试杀死 solo 进程")
  @Timeout(value = 15, timeUnit = TimeUnit.SECONDS)
  void killSoloTest(Vertx vertx, VertxTestContext testContext) {
//    WarUtils.unwar("/home/echo/Documents/solo-v3.2.0/solo-v3.2.0.war", "/home/echo/Documents/solo-v3.2.0/123");
    assertTrue(ExecuteCommand.killSolo());
    testContext.completeNow();
  }


}
