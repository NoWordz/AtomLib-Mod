// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.controls;
/** Sort events are intentions. Only the host sorts/replaces its data. */
public record CollectionEvent<K>(Kind kind,K key,String column,String action,boolean ascending) {
    public enum Kind { ITEM, ACTION, SORT }
}
