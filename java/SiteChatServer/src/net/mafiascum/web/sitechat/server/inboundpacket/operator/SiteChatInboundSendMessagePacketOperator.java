package net.mafiascum.web.sitechat.server.inboundpacket.operator;

import net.mafiascum.web.sitechat.server.SiteChatServer;
import net.mafiascum.web.sitechat.server.SiteChatServer.SiteChatWebSocket;
import net.mafiascum.web.sitechat.server.conversation.SiteChatConversationWithUserList;
import net.mafiascum.web.sitechat.server.inboundpacket.SiteChatInboundSendMessagePacket;
import net.mafiascum.web.sitechat.server.outboundpacket.SiteChatOutboundNewMessagePacket;

import com.google.gson.Gson;

public class SiteChatInboundSendMessagePacketOperator implements SiteChatInboundPacketOperator {

  public void process(SiteChatServer siteChatServer, SiteChatWebSocket siteChatWebSocket, String siteChatInboundPacketJson) throws Exception {

    SiteChatInboundSendMessagePacket siteChatInboundSendMessagePacket = new Gson().fromJson(siteChatInboundPacketJson, SiteChatInboundSendMessagePacket.class);
    
    if(siteChatWebSocket.getSiteChatUser() == null) {
      
      return;//User is not logged in.
    }
    
    SiteChatConversationWithUserList siteChatConversationWithUserList = siteChatServer.getSiteChatConversationWithUserList(siteChatInboundSendMessagePacket.getSiteChatConversationId());
    
    if(siteChatConversationWithUserList == null) {
      
      return;//Conversation does not exist.
    }
    
    if(!siteChatConversationWithUserList.getUserIdSet().contains(siteChatWebSocket.getSiteChatUser().getId())) {
      
      return;//User is not in the conversation.
    }
    
    //TODO: Record the message to the database.
    
    //Send the message to all users in the conversation(including the user who sent it).
    SiteChatOutboundNewMessagePacket siteChatOutboundNewMessagePacket = new SiteChatOutboundNewMessagePacket();
    siteChatOutboundNewMessagePacket.setUserId(siteChatWebSocket.getSiteChatUser().getId());
    siteChatOutboundNewMessagePacket.setSiteChatConversationId(siteChatInboundSendMessagePacket.getSiteChatConversationId());
    siteChatOutboundNewMessagePacket.setMessage(siteChatInboundSendMessagePacket.getMessage());
    
    siteChatServer.sendOutboundPacketToUsers(siteChatConversationWithUserList.getUserIdSet(), siteChatOutboundNewMessagePacket, null);
  }
}
