package com.lastbastion.game.player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;

/**
 * {@link PlayerContext} 的字节序列化助手，Redis / MySQL 实现共用。
 */
final class SerializationCodec {

    private SerializationCodec() {}

    static byte[] encode(PlayerContext ctx) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(ctx);
            oos.flush();
        } catch (IOException e) {
            throw new RuntimeException("encode PlayerContext failed", e);
        }
        return bos.toByteArray();
    }

    static Optional<PlayerContext> decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return Optional.empty();
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object read = ois.readObject();
            if (read instanceof PlayerContext ctx) return Optional.of(ctx);
            return Optional.empty();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("decode PlayerContext failed", e);
        }
    }
}
