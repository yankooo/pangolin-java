package base;

import io.netty.buffer.ByteBuf;

import io.netty.buffer.Unpooled;

public class XorCrypto implements Crypto {
    private final byte xorKey;

    public XorCrypto(byte xorKey) {
        this.xorKey = xorKey;
    }

    @Override
    public ByteBuf encrypt(ByteBuf data) {
        ByteBuf encrypted = Unpooled.buffer(data.readableBytes());
        while (data.isReadable()) {
            encrypted.writeByte(data.readByte() ^ xorKey);
        }
        return encrypted;
    }

    @Override
    public ByteBuf decrypt(ByteBuf data) {
        ByteBuf decrypted = Unpooled.buffer(data.readableBytes());
        while (data.isReadable()) {
            decrypted.writeByte(data.readByte() ^ xorKey);
        }
        return decrypted;
    }
}
