/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.util.List;

public class TrivialArrayUtil {
    public static long[] convertListToArray(List<Long> list) {
        if (null == list || list.isEmpty()) {
            return null;
        }
        int size = list.size();
        long[] arrays = new long[size];
        for (int i = 0; i < size; i++) {
            arrays[i] = list.get(i);
        }
        return arrays;
    }

    public static long[] getLongFromString(String[] strs) {
        if (null == strs || strs.length < 1) {
            return null;
        }
        long[] res = new long[strs.length];
        int index = 0;
        for (String ss : strs) {
            try {
                res[index] = Long.parseLong(ss);
                index++;
            } catch (NumberFormatException e) {
            }
        }
        return res;
    }


}
