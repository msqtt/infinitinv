//? if>=1.20 {
package com.infinitinv.storage;

/** Implemented by every player so the store can be reached through the entity. */
public interface IExtendedStorageHolder {

    /** The player's unbounded item store. Never {@code null}. */
    ExtendedStorage infinitinv$getExtendedStorage();
}
//? }
