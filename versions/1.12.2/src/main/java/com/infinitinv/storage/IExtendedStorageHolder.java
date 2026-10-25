package com.infinitinv.storage;

/**
 * Implemented by every player (server and client side) so that the extended
 * storage can be reached through the entity.
 */
public interface IExtendedStorageHolder {

    /** The player's unbounded item store. Never {@code null}. */
    ExtendedStorage infinitinv$getExtendedStorage();
}
