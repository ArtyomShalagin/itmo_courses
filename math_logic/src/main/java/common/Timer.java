package common;

import java.util.HashMap;
import java.util.Map;

public class Timer {
    private final Map<String, Long> map = new HashMap<>();
    private String DEFAULT_KEY = "ilovemathlogic";

    public void start(String key) {
        if (key.equals(DEFAULT_KEY)) {
            if (map.containsKey(DEFAULT_KEY)) {
                long value = map.get(DEFAULT_KEY);
                map.remove(DEFAULT_KEY);
                while (map.containsKey(DEFAULT_KEY)) {
                    DEFAULT_KEY += "~";
                }
                map.put(DEFAULT_KEY, value);
            }
        }
        map.put(key, System.currentTimeMillis());
    }

    public void start() {
        start(DEFAULT_KEY);
    }

    public long time(String key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("Can't get time on key " + key + ", didn't start with that key");
        }
        return System.currentTimeMillis() - map.get(key);
    }

    public long time() {
        return time(DEFAULT_KEY);
    }
}
