package com.yinyuan.radarsdk.server;

import com.yinyuan.radarsdk.enums.RadarEnum;
import com.yinyuan.radarsdk.handler.RadarDataHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * 创建一个TCP服务,用于接收24G or 77G毫米波雷达数据上传的数据
 * @author YD_Tao
 */
@Slf4j
public class RadarServer {

    private EventLoopGroup bossGroup = new NioEventLoopGroup();

    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    private ServerBootstrap bootstrap = new ServerBootstrap();

    private Channel channel;

    public RadarServer(){

    }

    /**
     * 启动RadarServer服务
     * @param address 服务监听的地址
     * @throws InterruptedException 中断异常
     * */
    public void startup(int radarType, RadarDataHandler handlerAdapter, InetSocketAddress address) throws InterruptedException {
        try {
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new IdleStateHandler(1800, 0, 0));
                            //77G雷达比24G多的两个Handler
                            if (radarType == RadarEnum.Radar_77G.getType()){
                                socketChannel.pipeline().addLast("http-codec",new HttpServerCodec());
                                socketChannel.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));
                            }
                            socketChannel.pipeline().addLast("data-handler", handlerAdapter);
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind(address).sync();
            log.info("--->RadarServerPort {}G Tcp服务器启动成功，端口：{}", radarType, address.getPort());
            channelFuture.channel().closeFuture().sync();
            channel = channelFuture.channel();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 手动销毁tcp server
     * */
    public void destroy(){
        if (channel != null){
            channel.close();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
