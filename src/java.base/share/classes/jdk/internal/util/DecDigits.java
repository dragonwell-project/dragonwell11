package jdk.internal.util;

import jdk.internal.vm.annotation.Stable;

public class DecDigits {
    @Stable
    public static final int[] DIGITS;

    static {
        int[] digits = new int[1000];
        for (int i = 0; i < 10; i++) {
            int i100 = i * 100;
            for (int j = 0; j < 10; j++) {
                int j10 = j * 10;
                for (int k = 0; k < 10; k++) {
                    digits[i100 + j10 + k]
                            = ((i == 0 && j == 0) ? 2 : (i == 0) ? 1 : 0) << 24
                            | ((i + '0') << 16)
                            | ((j + '0') << 8)
                            | (k + '0');
                }
            }
        }
        DIGITS = digits;
    }
}
