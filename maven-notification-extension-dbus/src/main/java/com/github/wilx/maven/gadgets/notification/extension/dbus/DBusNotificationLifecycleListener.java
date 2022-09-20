package com.github.wilx.maven.gadgets.notification.extension.dbus;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.freedesktop.Notifications;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Named
@Singleton
public class DBusNotificationLifecycleListener extends AbstractEventSpy {
    private final Logger logger = LoggerFactory.getLogger(DBusNotificationLifecycleListener.class);

    private final String dbusAddress;

    public DBusNotificationLifecycleListener() {
        final Map<String, String> env = System.getenv();
        this.dbusAddress = env.getOrDefault("DBUS_SESSION_BUS_ADDRESS", DBusConnection.DEFAULT_SYSTEM_BUS_ADDRESS);
        if (logger.isDebugEnabled()) {
            logger.debug("Using D-Bus address {}", this.dbusAddress);
        }
    }

    @Override
    public void onEvent(final Object event) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("Got event of type: {}", event.getClass().getName());
        }
        if (event instanceof ExecutionEvent) {
            final ExecutionEvent ee = (ExecutionEvent) event;
            if (ee.getType().equals(ExecutionEvent.Type.SessionEnded)) {
                final MavenSession mavenSession = ee.getSession();
                final MavenProject topLevelProject = mavenSession.getTopLevelProject();
                final MavenExecutionResult sessionResult = mavenSession.getResult();
                final boolean success = !sessionResult.hasExceptions();
                final String msg;
                if (success) {
                    msg = MessageFormat.format("Maven build of project <b>{0}</b> has finished",
                            topLevelProject.getName());
                } else {
                    msg = formatFailureMessage(topLevelProject, sessionResult);
                }
                notifyMessage(success ? "Success" : "Failure", msg);
            }
        }
    }

    @org.jetbrains.annotations.NotNull
    private String formatFailureMessage(final MavenProject topLevelProject, final MavenExecutionResult sessionResult) {
        final double[] exceptionsLimits = {0, 1, 2};
        final String[] exceptionsPart = {"were no exceptions", "was one exception", "were {1,number} exceptions"};
        final ChoiceFormat exceptionsForm = new ChoiceFormat(exceptionsLimits, exceptionsPart);
        final MessageFormat form = new MessageFormat("Maven build of project <b>{0}</b> has finished."
                + "\nThere {1}.");
        form.setFormatByArgumentIndex(1, exceptionsForm);
        return form.format(new Object[]{topLevelProject.getName(), sessionResult.getExceptions().size()});
    }

    private void notifyMessage(final String title, String msg) throws DBusException, IOException {
        try (final DBusConnection dbConn = DBusConnection.getConnection(this.dbusAddress)) {
            final Notifications notifications = dbConn.getRemoteObject("org.freedesktop.Notifications",
                    "/org/freedesktop/Notifications", Notifications.class);
            final Set<String> caps = new TreeSet<>(notifications.GetCapabilities());
            final boolean debugEnabled = logger.isDebugEnabled();
            if (debugEnabled) {
                logger.debug("org.freedesktop.Notifications.GetCapabilities(): {}", caps);
            }
            final boolean bodyMarkupSupport = caps.contains("body-markup");
            final boolean soundSupport = caps.contains("sound");

            if (!bodyMarkupSupport) {
                // Strip the markup.
                if (debugEnabled) {
                    logger.debug("Stripping body markup");
                }
                msg = Jsoup.parse(msg).text();
            }

            final Map<String, Variant<?>> hints = new LinkedHashMap<>(1);
            hints.put("urgency", new Variant<>((byte)1));
            if (soundSupport) {
                hints.put("sound-name", new Variant<>("dialog-information"));
            }

            final UInt32 id = notifications.Notify("Apache Maven", new UInt32(0), "", title, msg,
                    Collections.emptyList(), hints, 10000);
            if (debugEnabled) {
                logger.debug("Notify() returned {}", id);
            }
        }
    }
}
