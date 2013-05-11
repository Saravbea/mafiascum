package net.mafiascum.web.sitechat.server.outboundpacket;

import java.util.Date;
import java.util.List;

import net.mafiascum.web.sitechat.server.SiteChatUser;
import net.mafiascum.web.sitechat.server.conversation.SiteChatConversationWithUserList;

public class SiteChatOutboundUserListPacket extends SiteChatOutboundPacket {

  protected List<SiteChatUser> siteChatUsers;
  protected List<SiteChatConversationWithUserList> siteChatConversations;
  protected Date packetSentDatetime;

  public List<SiteChatUser> getSitechatUsers() {
    
    return siteChatUsers;
  }
  
  public void setSiteChatUsers(List<SiteChatUser> siteChatUsers) {
    
    this.siteChatUsers = siteChatUsers;
  }
  
  public SiteChatOutboundPacketType getType() {
    return SiteChatOutboundPacketType.userList;
  }
  
  public Date getPacketSentDatetime() {
    
    return packetSentDatetime;
  }
  
  public void setPacketSentDatetime(Date packetSentDatetime) {
    
    this.packetSentDatetime = packetSentDatetime;
  }

  public List<SiteChatConversationWithUserList> getSiteChatConversations() {
    return siteChatConversations;
  }

  public void setSiteChatConversations(List<SiteChatConversationWithUserList> siteChatConversations) {
    this.siteChatConversations = siteChatConversations;
  }
}
