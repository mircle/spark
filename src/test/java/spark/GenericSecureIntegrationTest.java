package spark;

import junit.framework.Assert;
import spark.util.SparkTestUtil;
import spark.util.SparkTestUtil.UrlResponse;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static spark.Spark.*;

public class GenericSecureIntegrationTest {

    static SparkTestUtil testUtil;

    @AfterClass
    public static void tearDown() {
        Spark.clearRoutes();
        Spark.stop();
    }

    @BeforeClass
    public static void setup() {
        testUtil = new SparkTestUtil(4567);

        // note that the keystore stuff is retrieved from SparkTestUtil which
        // respects JVM params for keystore, password
        // but offers a default included store if not.
        Spark.setSecure(SparkTestUtil.getKeyStoreLocation(),
                SparkTestUtil.getKeystorePassword(), null, null);

        before(new Filter("/protected/*") {

            @Override
            public void handle(Request request, Response response) {
                halt(401, "Go Away!");
            }
        });

        get(new Route("/hi") {

            @Override
            public Object handle(Request request, Response response) {
                return "Hello World!";
            }
        });

        get(new Route("/:param") {

            @Override
            public Object handle(Request request, Response response) {
                return "echo: " + request.params(":param");
            }
        });

        get(new Route("/paramwithmaj/:paramWithMaj") {

            @Override
            public Object handle(Request request, Response response) {
                return "echo: " + request.params(":paramWithMaj");
            }
        });

        get(new Route("/") {

            @Override
            public Object handle(Request request, Response response) {
                return "Hello Root!";
            }
        });

        post(new Route("/poster") {
            @Override
            public Object handle(Request request, Response response) {
                String body = request.body();
                response.status(201); // created
                return "Body was: " + body;
            }
        });

        after(new Filter("/hi") {
            @Override
            public void handle(Request request, Response response) {
                response.header("after", "foobar");
            }
        });

        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
    }

    @Test
    public void testGetHi() {
        try {
            SparkTestUtil.UrlResponse response = testUtil.doMethodSecure("GET",
                    "/hi", null);
            Assert.assertEquals(200, response.status);
            Assert.assertEquals("Hello World!", response.body);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testHiHead() {
        try {
            UrlResponse response = testUtil.doMethodSecure("HEAD", "/hi", null);
            Assert.assertEquals(200, response.status);
            Assert.assertEquals("", response.body);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetHiAfterFilter() {
        try {
            UrlResponse response = testUtil.doMethodSecure("GET", "/hi", null);
            Assert.assertTrue(response.headers.get("after").contains("foobar"));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetRoot() {
        try {
            UrlResponse response = testUtil.doMethodSecure("GET", "/", null);
            Assert.assertEquals(200, response.status);
            Assert.assertEquals("Hello Root!", response.body);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEchoParam1() {
        try {
            UrlResponse response = testUtil.doMethodSecure("GET", "/shizzy",
                    null);
            Assert.assertEquals(200, response.status);
            Assert.assertEquals("echo: shizzy", response.body);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEchoParam2() {
        try {
            UrlResponse response = testUtil.doMethodSecure("GET", "/gunit",
                    null);
            Assert.assertEquals(200, response.status);
            Assert.assertEquals("echo: gunit", response.body);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEchoParamWithMaj() {
        try {
            UrlResponse response = testUtil.doMethodSecure("GET",
                    "/paramwithmaj/plop", null);
            Assert.assertEquals(200, response.status);
            Assert.assertEquals("echo: plop", response.body);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = IOException.class)
    public void testUnauthorized() throws Exception {
        try {
            testUtil.doMethodSecure("GET", "/protected/resource", null);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("401"));
            throw e;
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void testNotFound() throws Exception {
        try {
            testUtil.doMethodSecure("GET", "/no/resource", null);
        } catch (Exception e) {
            throw e;
        }
    }

    @Test
    public void testPost() {
        try {
            UrlResponse response = testUtil.doMethodSecure("POST", "/poster",
                    "Fo shizzy");
            System.out.println(response.body);
            Assert.assertEquals(201, response.status);
            Assert.assertTrue(response.body.contains("Fo shizzy"));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
