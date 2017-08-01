package common;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class Util {
    @FunctionalInterface
    public interface ThrowableConverter<T, R, E extends Throwable> {
        R convert(T arg) throws E;
    }

    public static <T, R, E extends Throwable>
    Function<T, R> wrapNoThrow(ThrowableConverter<? super T, ? extends R, E> converter) {
        return arg -> {
            try {
                return converter.convert(arg);
            } catch (Throwable e) {
                System.err.println(e.getMessage());
                return null;
            }
        };
    }

    public static boolean areInstance(Class<?> token, Object... args) {
        return Arrays.stream(args)
                .allMatch(obj -> obj != null && obj.getClass() == token);
    }

    public static List<String> readFully(File file) throws IOException {
        List<String> result = new ArrayList<>();
        Scanner in = new Scanner(file);
        while (in.hasNextLine()) {
            result.add(in.nextLine());
        }
        in.close();
        return result;
    }

    public static String removeSpaces(String s) {
        return s.replaceAll("\\s", "");
//        int len = (int) IntStream.range(0, s.length())
//                .map(s::charAt)
//                .filter(c -> !Character.isSpaceChar(c))
//                .count();
//        char[] result = new char[len];
//        int p = 0;
//        for (char c : s.toCharArray()) {
//            if (!Character.isSpaceChar(c)) {
//                result[p++] = c;
//            }
//        }
//        return new String(result);
    }

    public static void log(Object message) {
        System.out.println(message);
    }

    public static void logNoNewline(Object message) {
        System.out.print(message);
    }

    public static void cleanLine(int length) {
        System.out.print("\r" + String.join("", Collections.nCopies(length, " ")) + "\r");
    }
}
