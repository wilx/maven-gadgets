package org.freedesktop;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.List;
import java.util.Map;

public interface Notifications extends DBusInterface {
    List<String> GetCapabilities();

    UInt32 Notify(String appName, UInt32 replacesId, String appIcon, String summary, String body, List<String> actions, Map<String, Variant<?>> hints, int expireTimeout);

    void CloseNotification(UInt32 id);

    Tuple4<String, String, String, String> GetServerInformation();

    class Tuple4<T1, T2, T3, T4> extends Tuple {
        private T1 v1;
        private T2 v2;
        private T3 v3;
        private T4 v4;

        public Tuple4(final T1 v1, final T2 v2, final T3 v3, final T4 v4) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
            this.v4 = v4;
        }

        public T1 getV1() {
            return v1;
        }

        public T2 getV2() {
            return v2;
        }

        public T3 getV3() {
            return v3;
        }

        public T4 getV4() {
            return v4;
        }
    }
}