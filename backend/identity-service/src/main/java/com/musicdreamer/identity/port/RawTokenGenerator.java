package com.musicdreamer.identity.port;

@FunctionalInterface
public interface RawTokenGenerator {

    String generate();
}
