package client;
import base.Config;
import base.TcpProxyRelayHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class Socks5ProxyClient {
    public static void main(String[] args) throws InterruptedException, IOException {
        // 读取配置文件
        ObjectMapper objectMapper = new ObjectMapper();
        Config config = objectMapper.readValue(new File("./config.json"), Config.class);

        // 获取配置参数
        String proxyHost = config.getProxyHost();
        int proxyPort = config.getProxyPort();
        int localPort = config.getLocalPort();

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        // 启动 Socks5 服务器
        startSocks5Server(bossGroup, workerGroup, localPort, proxyHost, proxyPort);

    }

    private static void startSocks5Server(EventLoopGroup bossGroup, EventLoopGroup workerGroup, int localPort, String proxyHost, int proxyPort) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                        ch.pipeline().addLast(new Socks5InitialRequestDecoder());
                        ch.pipeline().addLast(new Socks5CommandRequestDecoder());
                        ch.pipeline().addLast(new LocalSocks5ProxyClientHandler(proxyHost, proxyPort));
                        ch.pipeline().addLast(new TcpProxyRelayHandler(ch));
                    }
                });

        serverBootstrap.bind(localPort).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("客户端 Socks5 服务器启动成功，端口：" + localPort);
                } else {
                    System.err.println("客户端 Socks5 服务器启动失败，端口：" + localPort);
                }
            }
        });
    }
}
