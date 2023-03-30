package server;

import base.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

import java.io.File;
import java.io.IOException;

public class Socks5ProxyServer {
    private static final InternalLogger logger = Slf4JLoggerFactory.getInstance(Socks5ProxyServer.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        // 读取配置文件
        ObjectMapper objectMapper = new ObjectMapper();
        Config config = objectMapper.readValue(new File("config.json"), Config.class);
        int port = config.getProxyPort();

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                            ch.pipeline().addLast(new Socks5InitialRequestDecoder());
                            ch.pipeline().addLast(new Socks5PasswordAuthRequestDecoder());
                            ch.pipeline().addLast(new Socks5CommandRequestDecoder());
                            ch.pipeline().addLast(Socks5ServerEncoder.DEFAULT);
                            ch.pipeline().addLast(new Socks5ProxyServerHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("Socks5代理服务器已启动，端口号：" + port);
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}