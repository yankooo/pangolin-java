package client;
import base.Crypto;
import base.CryptoDecoder;
import base.XorCrypto;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import base.TcpProxyRelayHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;


public class LocalSocks5ProxyClientHandler extends SimpleChannelInboundHandler<Object> {

    // 定义一个异或密钥
    private static final byte XOR_KEY = 0x1A;
    private static final Crypto CRYPTO = new XorCrypto(XOR_KEY);
    private final String remoteProxyHost;
    private final int remoteProxyPort;

    public LocalSocks5ProxyClientHandler(String remoteProxyHost, int remoteProxyPort) {
        this.remoteProxyHost = remoteProxyHost;
        this.remoteProxyPort = remoteProxyPort;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Socks5InitialRequest) {
            Socks5InitialRequest initialRequest = (Socks5InitialRequest) msg;
            ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
        } else if (msg instanceof Socks5CommandRequest) {
            Socks5CommandRequest commandRequest = (Socks5CommandRequest) msg;
            if (commandRequest.type() == Socks5CommandType.CONNECT) {
                handleConnect(ctx, commandRequest);
            } else {
                ctx.write(new DefaultSocks5CommandResponse(
                        Socks5CommandStatus.COMMAND_UNSUPPORTED,
                        commandRequest.dstAddrType()));
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void handleConnect(final ChannelHandlerContext ctx, final Socks5CommandRequest commandRequest) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                        ch.pipeline().addLast(new CryptoDecoder(CRYPTO)); // 解密
                        ch.pipeline().addLast(new Socks5CommandRequestDecoder());
                        ch.pipeline().addLast(new Socks5InitialRequestDecoder());
                        ch.pipeline().addLast(new LocalSocks5ProxyClientHandler(remoteProxyHost, remoteProxyPort));
                    }
                });

        ChannelFuture connectFuture = bootstrap.connect(remoteProxyHost, remoteProxyPort);
        connectFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    ctx.pipeline().addLast(new TcpProxyRelayHandler(future.channel()));
                    future.channel().writeAndFlush(new DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH));
                    future.channel().writeAndFlush(commandRequest);
                } else {
                    ctx.writeAndFlush(new DefaultSocks5CommandResponse(
                            Socks5CommandStatus.FAILURE,
                            commandRequest.dstAddrType()));
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
