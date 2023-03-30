package base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class CryptoEncoder extends MessageToByteEncoder<ByteBuf> {
    private final Crypto crypto;

    public CryptoEncoder(Crypto crypto) {
        this.crypto = crypto;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        ByteBuf encrypted = crypto.encrypt(msg);
        out.writeBytes(encrypted);
        encrypted.release();
    }
}
