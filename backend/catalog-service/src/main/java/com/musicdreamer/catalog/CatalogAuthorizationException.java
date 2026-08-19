package com.musicdreamer.catalog;

public final class CatalogAuthorizationException extends RuntimeException {

    public CatalogAuthorizationException() {
        super("Catalog operation is not permitted");
    }
}
