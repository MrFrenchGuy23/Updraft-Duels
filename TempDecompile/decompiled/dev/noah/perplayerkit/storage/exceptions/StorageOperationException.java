/*
 * Decompiled with CFR 0.152.
 */
package dev.noah.perplayerkit.storage.exceptions;

import dev.noah.perplayerkit.storage.exceptions.StorageException;

public class StorageOperationException
extends StorageException {
    public StorageOperationException(String message) {
        super(message);
    }

    public StorageOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
