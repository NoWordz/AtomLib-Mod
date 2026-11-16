// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.controls;

/** Host-owned synchronous snapshot. Do not perform IO in these callbacks. Keys must be unique and immutable. */
public interface VirtualData<K,T> {
    int count();
    T itemAt(int index);
    K key(T item);
    /** Override for indexed lookup on large collections; invoked only when replacing data to keep an anchor. */
    default int indexOf(K key){for(int i=0;i<count();i++)if(java.util.Objects.equals(key,key(itemAt(i))))return i;return -1;}
}
