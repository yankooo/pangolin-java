package base;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class CryptoDecoder extends ByteToMessageDecoder {
    private final Crypto crypto;

    public CryptoDecoder(Crypto crypto) {
        this.crypto = crypto;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf decrypted = crypto.decrypt(in);
        out.add(decrypted);
    }
}
