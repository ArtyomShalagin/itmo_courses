package common;

import java.util.List;

public class Lists {
    public static <T> T get(List<T> data, int index) {
        if (index >= 0) {
            return data.get(index);
        } else {
            return data.get(data.size() + index);
        }
    }
}
