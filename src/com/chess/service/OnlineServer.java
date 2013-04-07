package com.chess.service;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.chess.util.ConfigProperties;


/**
 * (<b>Entry point</b>) Chat server
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class OnlineServer {
	static String minaServerIp = ConfigProperties.getConfigProperty("onlineservice.ip");
	static Integer minaServerPort = Integer.valueOf(ConfigProperties.getConfigProperty("onlineservice.port").trim());
	
	//private IoSession session;
	public static void main(String[] args) {
		try{
		// ####################
		// 创建一个非阻塞的Server端Socket，用NIO
		NioSocketAcceptor acceptor = new NioSocketAcceptor();
		
		
		// 创建接受数据的过滤器
//		acceptor.getFilterChain().addLast( "logger", new LoggingFilter() );
		// 设定这个过滤器将一行一行（/r/n）的读取数据
		acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter( new TextLineCodecFactory(Charset.forName( "UTF-8" ))));
		acceptor.getFilterChain().addLast("executor", new ExecutorFilter());
 
//		acceptor.setHandler(  new TimeServerHandler() );
		acceptor.getSessionConfig().setReadBufferSize( 2048 );
//		acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10 );
		System.out.println("Logging ON");
	
		// 设定服务器端的消息处理器:一个SamplMinaServerHandler对象
		// Bind
		acceptor.setHandler(new OnlineProtocolHandler());
		acceptor.bind(new InetSocketAddress(minaServerIp, minaServerPort));

		System.out.println("Listening on port "+minaServerPort);
		// ##################
		new Thread() {
			public void run() {
				try {
					UserOnlineInfoBLL.getInstance().detection();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
		
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}finally{
			
		}
		
		
	}

}
