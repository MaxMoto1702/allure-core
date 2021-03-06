package ru.yandex.qatools.allure.data.plugins

import org.codehaus.groovy.runtime.InvokerHelper
import org.junit.Test
import ru.yandex.qatools.allure.data.AllureTestCase
import ru.yandex.qatools.allure.data.ReportGenerationException
import ru.yandex.qatools.allure.data.Statistic
import ru.yandex.qatools.allure.data.utils.PluginUtils

import static ru.yandex.qatools.allure.config.AllureModelUtils.createFeatureLabel
import static ru.yandex.qatools.allure.config.AllureModelUtils.createStoryLabel
import static ru.yandex.qatools.allure.data.utils.PluginUtils.DEFAULT_FEATURE
import static ru.yandex.qatools.allure.data.utils.PluginUtils.DEFAULT_STORY
import static ru.yandex.qatools.allure.model.Status.BROKEN
import static ru.yandex.qatools.allure.model.Status.FAILED
import static ru.yandex.qatools.allure.model.Status.PASSED

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 07.02.15
 */
class BehaviorTest {

    def plugin = new Behavior()

    @Test
    void shouldBeDefaultWhenLabelsDoesNotExist() {
        def testCase = new AllureTestCase(
                uid: "uid", name: "name", status: FAILED
        )

        plugin.process(testCase)

        assert plugin.behavior.features.size() == 1

        def feature = plugin.behavior.features[0]
        assert feature.title == DEFAULT_FEATURE
        assert feature.stories.size() == 1

        use(PluginUtils) {
            assert feature.statistic.eq(new Statistic(failed: 1, total: 1))
        }

        def story = feature.stories[0]
        assert story.title == DEFAULT_STORY
        assert story.testCases.size() == 1

        use(PluginUtils) {
            assert story.statistic.eq(new Statistic(failed: 1, total: 1))
        }

        def info = story.testCases[0]

        use([InvokerHelper, PluginUtils]) {
            assert info.getProperties() == testCase.toInfo().getProperties()
        }
    }

    @Test(expected = ReportGenerationException.class)
    void testCaseStatusShouldNotBeNull() {
        def testCase = new AllureTestCase(
                uid: "uid", name: "name"
        )

        plugin.process(testCase)
    }

    @Test
    void shouldGroupByFeature() {
        def testCase = new AllureTestCase(
                uid: "uid", name: "name", status: PASSED, labels: [createFeatureLabel("feature")]
        )

        plugin.process(testCase)

        assert plugin.behavior.features.size() == 1

        def feature = plugin.behavior.features[0]
        assert feature.title == "feature"
        assert feature.stories.size() == 1

        use(PluginUtils) {
            assert feature.statistic.eq(new Statistic(passed: 1, total: 1))
        }

        def story = feature.stories[0]
        assert story.title == DEFAULT_STORY
        assert story.testCases.size() == 1

        use(PluginUtils) {
            assert story.statistic.eq(new Statistic(passed: 1, total: 1))
        }

        def info = story.testCases[0]

        use([InvokerHelper, PluginUtils]) {
            assert info.getProperties() == testCase.toInfo().getProperties()
        }
    }

    @Test
    void shouldGroupByStory() {
        def testCase = new AllureTestCase(
                uid: "uid", name: "name", status: BROKEN, labels: [createStoryLabel("story")]
        )

        plugin.process(testCase)

        assert plugin.behavior.features.size() == 1

        def feature = plugin.behavior.features[0]
        assert feature.title == DEFAULT_FEATURE
        assert feature.stories.size() == 1

        use(PluginUtils) {
            assert feature.statistic.eq(new Statistic(broken: 1, total: 1))
        }

        def story = feature.stories[0]
        assert story.title == "story"
        assert story.testCases.size() == 1

        use(PluginUtils) {
            assert story.statistic.eq(new Statistic(broken: 1, total: 1))
        }

        def info = story.testCases[0]

        use([InvokerHelper, PluginUtils]) {
            assert info.getProperties() == testCase.toInfo().getProperties()
        }
    }

    @Test
    void shouldGroupByStoryAndFeature() {
        def testCase = new AllureTestCase(
                uid: "uid", name: "name", status: BROKEN,
                labels: [createStoryLabel("story1"), createStoryLabel("story2"),
                         createFeatureLabel("feature1"), createFeatureLabel("feature2")]
        )

        plugin.process(testCase)

        def features = plugin.behavior.features
        assert features.size() == 2

        assert features.collect { it.title }.containsAll(["feature1", "feature2"])
        assert features.each {
            item ->
                use(PluginUtils) {
                    assert item.statistic.eq(new Statistic(broken: 2, total: 2))
                }
        }

        features.each {
            assert it.stories.collect { it.title }.containsAll(["story1", "story2"])
        }

        features.each {
            it.stories.each {
                item ->
                    use(PluginUtils) {
                        assert item.statistic.eq(new Statistic(broken: 1, total: 1))
                    }
                    assert item.testCases.size() == 1
                    def info = item.testCases[0]

                    use([InvokerHelper, PluginUtils]) {
                        assert info.getProperties() == testCase.toInfo().getProperties()
                    }
            }
        }
    }
}
