package com.chess.service;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chess.pojo.Command;
import com.chess.pojo.UserOnlineInfo;
import com.chess.util.JsonUtil;

/**
 * {@link IoHandler} implementation of a simple chat server protocol.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class OnlineProtocolHandler extends IoHandlerAdapter {
	private final static Logger LOGGER = LoggerFactory.getLogger(OnlineProtocolHandler.class);

	UserOnlineInfoBLL userInfoBLL = UserOnlineInfoBLL.getInstance();
	public static final String INDEX_KEY = OnlineProtocolHandler.class.getName() + ".INDEX";
    //key=sessionId  value = session   sid 和 session对应
    private   ConcurrentHashMap<Long, IoSession>  ioSessionMap = new ConcurrentHashMap<Long, IoSession>();
    //key = userId value = sessionId   用户和 sid 对应
    private ConcurrentHashMap<Integer,Long> userSessionMap =   new ConcurrentHashMap<Integer, Long>(); 

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		LOGGER.warn("Unexpected exception.", cause);
		// Close connection when unexpected exception is caught.
		session.close(true);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {
		String theMessage = (String) message;
		//获取sessionId
	    Long sid = session.getId(); 
	    //如果没有此sessionId则代表第一次连接
	    if (!ioSessionMap.containsKey(sid)) {
		    //把此session放入map
		    ioSessionMap.put(sid, session);
		   
		    byte[] bufferAry = buffer.array();
	        String m = new String(bufferAry);
	       
	        //获取握手协议字符串
			String sss = getSecWebSocketAccept(m);
		
			buffer.clear();
			buffer.put(sss.getBytes("utf-8"));
		
			buffer.flip();
			session.write(buffer);
		
			buffer.free();
	    } 
		//UserInfo
		String[] result = theMessage.split(" ", 2);
		String theCommand = result[0];
		String valueItem = result[1].trim();
		UserOnlineInfo userOnlineInfo;
		
		try {
			Command cmd = Command.values()[Integer.valueOf(theCommand)];
			switch (cmd) {
			case GET_BATTLE_USER:
				userOnlineInfo=(UserOnlineInfo)JsonUtil.readValue(valueItem, UserOnlineInfo.class);
				session.write(userOnlineInfo);
				break;
			case UPDATE:
				userOnlineInfo=(UserOnlineInfo)JsonUtil.readValue(valueItem, UserOnlineInfo.class);	
				userInfoBLL.update(userOnlineInfo);
				break;
			default:
				break;
			}
		} catch (IllegalArgumentException e) {
			LOGGER.debug("Illegal argument", e);
		}
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		LOGGER.info("关闭session");
	}

}
