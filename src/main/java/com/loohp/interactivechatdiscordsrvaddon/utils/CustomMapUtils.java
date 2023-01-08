/*
 * This file is part of InteractiveChatDiscordSrvAddon.
 *
 * Copyright (C) 2022. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2022. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.interactivechatdiscordsrvaddon.utils;

import java.util.*;
import java.util.Map.Entry;

public class CustomMapUtils {

    public static <K, V extends Comparable<? super V>> List<Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        List<Entry<K, V>> sortedEntries = new ArrayList<>(map.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        return sortedEntries;
    }

    public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> sortMapByValue(Map<K, V> map) {
        LinkedHashMap<K, V> linkedmap = new LinkedHashMap<>();
        entriesSortedByValues(map).forEach((entry) -> linkedmap.put(entry.getKey(), entry.getValue()));
        return linkedmap;
    }

    public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> sortMapByValueReverse(Map<K, V> map) {
        LinkedHashMap<K, V> linkedmap = new LinkedHashMap<>();
        List<Entry<K, V>> list = new ArrayList<>(sortMapByValue(map).entrySet());
        ListIterator<Entry<K, V>> itr = list.listIterator(list.size());
        while (itr.hasPrevious()) {
            Entry<K, V> entry = itr.previous();
            linkedmap.put(entry.getKey(), entry.getValue());
        }
        return linkedmap;
    }

}
