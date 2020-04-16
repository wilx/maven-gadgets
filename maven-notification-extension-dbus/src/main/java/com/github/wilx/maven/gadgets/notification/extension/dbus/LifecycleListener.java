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
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Named
@Singleton
public class LifecycleListener extends AbstractEventSpy {
    private final Logger logger = LoggerFactory.getLogger(LifecycleListener.class);

    private final String dbusAddress;

    public LifecycleListener() {
        final Map<String, String> env = System.getenv();
        this.dbusAddress = env.getOrDefault("DBUS_SESSION_BUS_ADDRESS", DBusConnection.DEFAULT_SYSTEM_BUS_ADDRESS);
        logger.debug("Using D-Bus address {}", this.dbusAddress);
    }

    @Override
    public void onEvent(Object event) throws Exception {
        logger.trace("Got event of type: {}", event.getClass().getName());
        if (event instanceof ExecutionEvent) {
            ExecutionEvent ee = (ExecutionEvent) event;
            ExecutionEvent.Type eeType = ee.getType();
            MavenSession mavenSession = ee.getSession();
            if (eeType.equals(ExecutionEvent.Type.SessionEnded)) {
                MavenProject topLevelProject = mavenSession.getTopLevelProject();
                MavenExecutionResult sessionResult = mavenSession.getResult();
                boolean success = !sessionResult.hasExceptions();
                String msg;
                if (success) {
                    msg = MessageFormat.format("Maven build of project <b>{0}</b> has finished",
                            topLevelProject.getName());
                } else {
                    msg = MessageFormat.format("Maven build of project <b>{0}</b> has finished."
                                    + "\nThere were {1} exceptions.",
                            topLevelProject.getName(), sessionResult.getExceptions().size());
                }
                notifyMessage(success ? "Success" : "Failure", msg);
            }
        }
    }

    private void notifyMessage(String title, String msg) throws DBusException, IOException {
        try (DBusConnection dbConn = DBusConnection.getConnection(this.dbusAddress)) {
            Notifications notifications = dbConn.getRemoteObject("org.freedesktop.Notifications",
                    "/org/freedesktop/Notifications", Notifications.class);
            Set<String> caps = new TreeSet<>(notifications.GetCapabilities());
            logger.debug("org.freedesktop.Notifications.GetCapabilities(): {}", caps);
            final boolean bodyMarkupSupport = caps.contains("body-markup");
            final boolean soundSupport = caps.contains("sound");

            if (!bodyMarkupSupport) {
                // Strip the markup.
                logger.debug("Stripping body markup");
                msg = Jsoup.parse(msg).text();
            }

            Map<String, Variant<?>> hints = new LinkedHashMap<>(1);
            hints.put("urgency", new Variant<>((byte)1));
            if (soundSupport) {
                hints.put("sound-name", new Variant<>("dialog-information"));
            }

            UInt32 id = notifications.Notify("Apache Maven", new UInt32(0), "", title, msg,
                    Collections.emptyList(), hints, 10000);
            logger.debug("Notify() returned {}", id);
        }
    }
}
