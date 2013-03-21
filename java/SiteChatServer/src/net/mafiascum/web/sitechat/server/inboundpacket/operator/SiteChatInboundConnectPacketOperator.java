package net.mafiascum.web.sitechat.server.inboundpacket.operator;

import net.mafiascum.web.sitechat.server.SiteChatServer;
import net.mafiascum.web.sitechat.server.SiteChatServer.SiteChatWebSocket;
import net.mafiascum.web.sitechat.server.SiteChatUser;
import net.mafiascum.web.sitechat.server.conversation.SiteChatConversation;
import net.mafiascum.web.sitechat.server.conversation.SiteChatConversationWithUserList;
import net.mafiascum.web.sitechat.server.inboundpacket.SiteChatInboundConnectPacket;

import com.google.gson.Gson;

public class SiteChatInboundConnectPacketOperator implements SiteChatInboundPacketOperator {

  public void process(SiteChatServer siteChatServer, SiteChatWebSocket siteChatWebSocket, String siteChatInboundPacketJson) throws Exception {
    
    SiteChatInboundConnectPacket siteChatInboundConnectPacket = new Gson().fromJson(siteChatInboundPacketJson, SiteChatInboundConnectPacket.class);
    SiteChatConversation siteChatConversation;
    SiteChatConversationWithUserList siteChatConversationWithUserList;
    SiteChatUser siteChatUser = siteChatWebSocket.getSiteChatUser();
    
    if(siteChatUser == null) {
      //Not logged in.
      
      System.out.println("User trying to connect to chat without first logging in.");
      return;
    }
    
    String siteChatConversationName = siteChatInboundConnectPacket.getSiteChatConversationName();
    
    siteChatConversationWithUserList = siteChatServer.getSiteChatConversationWithUserList(siteChatConversationName);
    
    System.out.println("Connecting to conversation. Name: `" + siteChatInboundConnectPacket.getSiteChatConversationName() + "`");
    
    if(siteChatConversationWithUserList == null) {
      
      System.out.println("Creating conversation...");
      siteChatConversationWithUserList = siteChatServer.createSiteChatConversation(siteChatInboundConnectPacket.getSiteChatConversationName(), siteChatUser.getId());
    }
    
    siteChatServer.attemptJoinConversation(siteChatUser.getId(), siteChatConversationWithUserList.getSiteChatConversation().getId(), true, true);
  }
}
