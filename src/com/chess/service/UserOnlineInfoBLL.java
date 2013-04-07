package com.chess.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.chess.pojo.GameStatus;
import com.chess.pojo.UserOnlineInfo;
import com.chess.util.ConfigProperties;

/**
 * 
 * @author winse
 * @ex UserOnlineInfoBLL userInfoBLL = UserOnlineInfoBLL.getInstance();
 *     userInfoBLL.setQuarterUsers();
 */
public class UserOnlineInfoBLL {
	public  ConcurrentHashMap<String, UserOnlineInfo> onlineMap;
	public ConcurrentHashMap<String, ConcurrentHashMap<String,UserOnlineInfo>> arenaMap;
	public ConcurrentHashMap<String,Integer> serverMap;
	
	private static UserOnlineInfoBLL instance=new UserOnlineInfoBLL();

	private long lastPrintOnline=new Date().getTime();
	
	private UserOnlineInfoBLL() {
		onlineMap = new ConcurrentHashMap<String, UserOnlineInfo>();
		serverMap=new ConcurrentHashMap<String, Integer>();

	}

	public static UserOnlineInfoBLL getInstance() {
		if (instance == null) {
			instance = new UserOnlineInfoBLL();
		}
		return instance;
	}

	public void update(String info) {
		if (onlineMap == null)
			onlineMap = new ConcurrentHashMap<String, UserOnlineInfo>();

		String[] result = info.split(",", 2);
		String id = result[0];
		String nick = result[1];

		if (onlineMap.containsKey(id)) {
			onlineMap.get(id).setDateTime(new Date().getTime());
			onlineMap.get(id).setNick(nick);
		} else {
			UserOnlineInfo userInfo = new UserOnlineInfo();
			userInfo.setDateTime(new Date().getTime());
			userInfo.setNick(nick);
			onlineMap.put(id, userInfo);
		}

	}
	
	public void update(UserOnlineInfo userOnlineInfo) {
		if (onlineMap == null)
			onlineMap = new ConcurrentHashMap<String, UserOnlineInfo>();
		if(this.arenaMap==null)
			this.arenaMap=new ConcurrentHashMap<String, ConcurrentHashMap<String,UserOnlineInfo>>();
		String id=userOnlineInfo.getUserId();
		String nick=userOnlineInfo.getNick();
		//更新在线用户列表
		if (onlineMap.containsKey(id)) {
			onlineMap.get(id).setDateTime(new Date().getTime());
			onlineMap.get(id).setNick(nick);
		} else {
			onlineMap.put(id, userOnlineInfo);
			if(serverMap.get(userOnlineInfo.getIp())==null)
			{
				serverMap.put(userOnlineInfo.getIp(), 0);
			}
			Integer onlineCount=serverMap.get(userOnlineInfo.getIp());
			serverMap.put(userOnlineInfo.getIp(),onlineCount+1);
		}
	}
	public  void detection() throws InterruptedException {
		while (true) {
			if (onlineMap == null || onlineMap.isEmpty()) {
				Thread.sleep(3 * 60 * 1000);
				continue;
			}
			
			Set<String> ites = onlineMap.keySet();
			List<String> usernames = new ArrayList<String>(ites);
			for (String s : usernames) {
				if (onlineMap.get(s).getDateTime().compareTo(new Date().getTime() - 15 * 60 * 1000) <= 0)
				{
					if(serverMap.get(onlineMap.get(s).getIp())==null)
					{
						serverMap.put(onlineMap.get(s).getIp(), 0);
					}

					Integer onlineCount=serverMap.get(onlineMap.get(s).getIp());
					onlineCount=onlineCount>1?onlineCount-1:0;

					serverMap.put(onlineMap.get(s).getIp(),onlineCount);
					onlineMap.remove(s);					
				}
			}
			
			//判断是否需要重新计算各服务器的在线任务
			reComputeServerOnline();
			Thread.sleep(60 * 1000);
		}
	}

	public Integer setQuarterUsers() {
		Integer count;
		if (onlineMap == null)
			count = 0;
		else
			count = onlineMap.size();
		return count;
	}
	
	final int controlUserCount = Integer.parseInt(ConfigProperties.getConfigProperty("control.user.count"));
	//1:true 0:false
	//0被拦截 
	public String setQuarterUsers(String id) {

		
		String result = "";
		
		if(id.equals("GetUsersCount"))
		{
			result= onlineMap.size()+","+0;
			return result;
		}
		
		String[] params=id.split(",");
		String userId=params[0];
		String ip=params[1];
		
		if (onlineMap == null)
			result += "0,0";
		else {
			Integer count = serverMap.get(ip);
			if (count!=null&&(count > controlUserCount) && !onlineMap.containsKey(userId))
				result = count + "," + 0;
			else
				result = count + "," + 1;
		}
		return result;

	}
	private void reComputeServerOnline()
	{
		//判断是否超过阀值		
		List<String> listIp=new ArrayList<String>(serverMap.keySet());
		
		Integer total=0;
		long nowTime=new Date().getTime();
		boolean printOnline=false;
		if(ConfigProperties.getConfigProperty("print_online_debug").equals("true"))
		{
			printOnline=true;
		}
		if(nowTime-lastPrintOnline>30*60*1000)
		{
			printOnline=true;
			lastPrintOnline=nowTime;
		}
		for(int i=0;i<listIp.size();i++)
		{
			String ip=listIp.get(i);
			Integer onlineCount=serverMap.get(ip);
			if(printOnline)
			{
				System.out.println(ip+":"+onlineCount);
			}
			total+=onlineCount;
		}
		if(Math.abs(total-onlineMap.size())>=Integer.parseInt(ConfigProperties.getConfigProperty("online_interval_max")))
		{
			for(int i=0;i<listIp.size();i++)
			{
				String ip=listIp.get(i);
				serverMap.put(ip, 0);
			}
			List<String> listUserId=new ArrayList<String>(onlineMap.keySet());
			for(int i=0;i<onlineMap.size();i++)
			{
				UserOnlineInfo userOnlineInfo= onlineMap.get(listUserId.get(i));
				serverMap.put(userOnlineInfo.getIp(), serverMap.get(userOnlineInfo.getIp())+1);
			}
		}
	}
	/*
	 * //onlineAlive-1 dead-0
	 */
	public Integer setIsOnline(String userId) {
		return onlineMap != null && onlineMap.containsKey(userId) ? 1 : 0;

	}

}
