package net.mafiascum.web.sitechat.server.inboundpacket.operator;

import java.util.Date;

import net.mafiascum.web.sitechat.server.SiteChatServer;
import net.mafiascum.web.sitechat.server.SiteChatServer.SiteChatWebSocket;
import net.mafiascum.web.sitechat.server.SiteChatUser;
import net.mafiascum.web.sitechat.server.conversation.SiteChatConversationWithUserList;
import net.mafiascum.web.sitechat.server.inboundpacket.SiteChatInboundLeaveConversationPacket;
import net.mafiascum.web.sitechat.server.outboundpacket.SiteChatOutboundLeaveConversationPacket;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

public class SiteChatInboundLeaveConversationPacketOperator extends SiteChatInboundSignedInPacketOperator {

  private static final Logger logger = Logger.getLogger(SiteChatInboundLeaveConversationPacketOperator.class.getName());
  
  public SiteChatInboundLeaveConversationPacketOperator() {
    super();
  }
  
  public void process(SiteChatServer siteChatServer, SiteChatUser siteChatUser, SiteChatWebSocket siteChatWebSocket, String siteChatInboundPacketJson) throws Exception {
    
    SiteChatInboundLeaveConversationPacket siteChatInboundLeaveConversationPacket = new Gson().fromJson(siteChatInboundPacketJson, SiteChatInboundLeaveConversationPacket.class);
    SiteChatConversationWithUserList siteChatConversationWithUserList;
    
    siteChatServer.updateUserActivity(siteChatUser.getId());
    
    synchronized(siteChatUser) {
      
      siteChatUser.setLastActivityDatetime(new Date());
    }
    
    siteChatConversationWithUserList = siteChatServer.getSiteChatConversationWithUserList(siteChatInboundLeaveConversationPacket.getSiteChatConversationId());
    
    if(siteChatConversationWithUserList == null) {
      
      return;
    }
    
    //Remove the user from the user ID cache.
    siteChatConversationWithUserList.getUserIdSet().remove(siteChatWebSocket.getSiteChatUser().getId());
    
    //Notify all other users in the conversation of the user's departure.
    SiteChatOutboundLeaveConversationPacket siteChatOutboundLeaveConversationPacket = new SiteChatOutboundLeaveConversationPacket();
    siteChatOutboundLeaveConversationPacket.setUserId(siteChatWebSocket.getSiteChatUser().getId());
    siteChatOutboundLeaveConversationPacket.setSiteChatConversationId(siteChatInboundLeaveConversationPacket.getSiteChatConversationId());
    
    siteChatServer.sendOutboundPacketToUsers(siteChatConversationWithUserList.getUserIdSet(), siteChatOutboundLeaveConversationPacket, null);
  }
}
