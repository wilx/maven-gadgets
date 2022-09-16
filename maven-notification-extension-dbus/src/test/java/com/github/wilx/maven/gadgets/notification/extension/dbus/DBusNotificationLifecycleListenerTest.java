package com.github.wilx.maven.gadgets.notification.extension.dbus;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ChoiceFormat;
import java.text.MessageFormat;


class DBusNotificationLifecycleListenerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DBusNotificationLifecycleListenerTest.class);

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 10})
    void test(final int exceptionsCount) {
        final double[] exceptionsLimits = {0, 1, 2};
        final String[] exceptionsPart = {"were no exceptions", "was one exception", "were {1,number} exceptions"};
        final ChoiceFormat exceptionsForm = new ChoiceFormat(exceptionsLimits, exceptionsPart);
        final MessageFormat form = new MessageFormat("Maven build of project <b>{0}</b> has finished."
                + "\nThere {1}.");
        form.setFormatByArgumentIndex(1, exceptionsForm);
        final String msg = form.format(new Object[]{"testProject", exceptionsCount});
        Assertions.assertNotNull(msg);
        LOGGER.info("message: {}", msg);
    }

}