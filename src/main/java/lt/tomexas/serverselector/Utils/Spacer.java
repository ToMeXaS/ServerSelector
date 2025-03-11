package lt.tomexas.serverselector.Utils;

import java.util.ArrayList;
import java.util.List;

public class Spacer {
    public static String getNegativeSpacer(int pixel) {
        StringBuilder negativeSpacer = new StringBuilder();
        for (int px : divideIntoPowersOfTwo(pixel))
            negativeSpacer.append(convertToNegativeUnicode(px));

        return negativeSpacer.toString();
    }

    public static char convertToNegativeUnicode(int pixel) {
        return switch (pixel) {
            case 1 -> '\uF801';
            case 2 -> '\uF802';
            case 4 -> '\uF804';
            case 8 -> '\uF808';
            case 16 -> '\uF809';
            case 32 -> '\uF80A';
            case 64 -> '\uF80B';
            case 128 -> '\uF80C';
            default -> throw new IllegalStateException("Unexpected value: " + pixel);
        };
    }

    public static String getPositiveSpacer(int pixel) {
        StringBuilder positiveSpacer = new StringBuilder();
        for (int px : divideIntoPowersOfTwo(pixel))
            positiveSpacer.append(convertToPositiveUnicode(px));

        return positiveSpacer.toString();
    }

    public static char convertToPositiveUnicode(int pixel) {
        return switch (pixel) {
            case 1 -> '\uF821';
            case 2 -> '\uF822';
            case 4 -> '\uF824';
            case 8 -> '\uF828';
            case 16 -> '\uF829';
            case 32 -> '\uF82A';
            case 64 -> '\uF82B';
            case 128 -> '\uF82C';
            default -> throw new IllegalStateException("Unexpected value: " + pixel);
        };
    }

    public static List<Integer> divideIntoPowersOfTwo(int totalWidth) {
        List<Integer> result = new ArrayList<>();

        // Start with the largest power of 2 we want to consider (128)
        int[] powers = {128, 64, 32, 16, 8, 4, 2, 1}; // Define powers of 2

        for (int power : powers) {
            while (totalWidth >= power) {
                result.add(power);
                totalWidth -= power; // Subtract the power from totalWidth
            }
        }
        return result;
    }
}
