// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.controls;
import java.util.Objects;
/** Width is in UI units, independent of GUI pixel density. */
public record TableColumn(String id,String label,float width,boolean sortable) {
    public TableColumn {Objects.requireNonNull(id);Objects.requireNonNull(label);if(id.isBlank()||!Float.isFinite(width)||width<=0)throw new IllegalArgumentException("Invalid column");}
}
