package cn.echocow.soloauto;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * 启动测试
 *
 * @author Echo
 * @version 1.2
 * @date 2019-03-18 10:39
 */
class ApplicationTest {
  @Test
  void test() {    JsonObject config = new JsonObject();
    // 测试传入配置
    config.put("deploy", "solo")
      .put("homeDir", "/home/echo/Other/apache-tomcat-9.0.16/webapps/")
      .put("tomcatDir", "solo")
      .put("version", "v3.2.0");
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class.getName(), new DeploymentOptions().setConfig(config));
  }
}
