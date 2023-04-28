import org.apache.http.entity.ContentType;
import org.apache.jmeter.protocol.http.util.HTTPConstants;
import org.junit.jupiter.api.Test;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.listeners.JtlWriter;
import us.abstracta.jmeter.javadsl.core.threadgroups.DslDefaultThreadGroup;
import us.abstracta.jmeter.javadsl.core.threadgroups.RpsThreadGroup;

import java.time.Duration;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.*;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;
import static us.abstracta.jmeter.javadsl.dashboard.DashboardVisualizer.dashboardVisualizer;

class PerformanceTestApplicationTests {

    private static final String PORT = "8282";
    private static final String BASE_URL = String.format("http://10.0.0.192:%s/inventory", PORT);

    @Test
    void testPerformanceUpdateInventory() throws Exception {
        DslDefaultThreadGroup threadGroup = buildInventoryThreadGroup();

//        Throughput based thread group
//        RpsThreadGroup rpsThreadGroup = rpsThreadGroup("rps thread group")
//                .maxThreads(10)
//                .rampToAndHold(100.0, Duration.ofSeconds(5), Duration.ofSeconds(10))
//                .children(
//                        httpSampler("test", "test.com")
//                        , percentController(),
//                        , forEachController()
//                        , forLoopController()
//                        , whileController()
//                        , ifController()
//                        , dummySampler("dummy response")
//                        , influxDbListener("url")
//                );

        DslTestPlan testPlan = testPlan(
                httpDefaults()
                        .connectionTimeout(Duration.ofSeconds(10))
                        .responseTimeout(Duration.ofMinutes(1)),
                threadGroup,
//                dashboardVisualizer(),
                htmlReporter("target/reports"),
                jtlWriter("target/jtls/success").logOnly(JtlWriter.SampleStatus.SUCCESS),
                jtlWriter("target/jtls/error").logOnly(JtlWriter.SampleStatus.ERROR).withAllFields()
        );

//        java -jar jmdsl.jar jmx2dsl test-plan.jmx // script to convert JMX to DSL code

//        jbang us.abstracta.jmeter:jmeter-java-dsl-cli:1.9 recorder {{URL}} // script to start recording at the url


//    threadGroup.showTimeline(); // shows visual for thread group i.e. ramping, holding, and thread count over time

//    testPlan.saveAsJmx("f.jmx"); // saves the test plan in a format that the jmeter gui can use
//    testPlan.showInGui(); // opens the test plan in the jmeter gui

        TestPlanStats stats = testPlan.run(); // TestPlanStats gives you access to all the stats that you can then test against

//        InfluxDB, ElasticSearch, Grafana

//    testPlan.runIn(new DistributedJmeterEngine("host1", "host2", "host...")); // runIn method allows you to perform performance testing at scale using a JmeterEngine like octoperf and blazemeter

//    System.out.println(stats.byLabel("update inventory endpoint").sampleTimePercentile99());
//    System.out.println(stats.overall().sampleTimePercentile99());

        assertThat(stats.overall().sampleTimePercentile99()).isLessThan(Duration.ofSeconds(5));

    }

    private static String buildInventoryUpdateRequestBody() {
        Random random = new Random();
        int itemIdInt = random.nextInt(9999);
        String itemId = Integer.toString(itemIdInt);
        StringBuilder stringBuilder = new StringBuilder(itemId);

        while (stringBuilder.length() < 4) {
            stringBuilder.insert(0, '0');
        }

        itemId = stringBuilder.toString();
        int unitChange = random.nextInt(500);

        return String.format("{\"itemid\":\"%s\",\"unitchange\":%s,\"discontinued\":false}", itemId, unitChange);
    }

    private DslDefaultThreadGroup buildInventoryThreadGroup() {
        return threadGroup("Inventory Thread Group")
                .rampTo(5, Duration.ofSeconds(5))
                .rampToAndHold(10, Duration.ofSeconds(5), Duration.ofSeconds(5))
//                .holdIterating(20)
                .rampTo(0, Duration.ofSeconds(5))
                .children(
                        httpSampler("update inventory endpoint", String.format("%s/update", BASE_URL))
                                .method(HTTPConstants.PUT)
                                .body("${REQUEST_BODY}")
                                .contentType(ContentType.APPLICATION_JSON)
                                .children(
                                        jsr223PreProcessor("vars.put('REQUEST_BODY', " + getClass().getName() + ".buildInventoryUpdateRequestBody())"),
                                        responseAssertion().containsSubstrings("OK")
//                                        , responseAssertion().containsSubstrings("${REQUEST_BODY}")
                                )
                );
    }
}
