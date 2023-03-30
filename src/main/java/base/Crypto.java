package base;

import io.netty.buffer.ByteBuf;
public interface Crypto {
    ByteBuf encrypt(ByteBuf data);
    ByteBuf decrypt(ByteBuf data);
}
