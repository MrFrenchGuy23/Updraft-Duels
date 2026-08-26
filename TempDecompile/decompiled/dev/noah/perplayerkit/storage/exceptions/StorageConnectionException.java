/*
 * Decompiled with CFR 0.152.
 */
package dev.noah.perplayerkit.storage.exceptions;

import dev.noah.perplayerkit.storage.exceptions.StorageException;

public class StorageConnectionException
extends StorageException {
    public StorageConnectionException(String message) {
        super(message);
    }

    public StorageConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
