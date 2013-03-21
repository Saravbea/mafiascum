package net.mafiascum.web.sitechat.server.conversation;

import java.util.Date;

import net.mafiascum.web.misc.DataObjectWithIntId;

public class SiteChatConversationMessage extends DataObjectWithIntId {

  protected int userId;
  protected Date createdDatetime;
  protected int siteChatConversationId;
  protected String content;
  
  public SiteChatConversationMessage() {
    
    id = NEW;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  public Date getCreatedDatetime() {
    return createdDatetime;
  }

  public void setCreatedDatetime(Date createdDatetime) {
    this.createdDatetime = createdDatetime;
  }

  public int getSiteChatConversationId() {
    return siteChatConversationId;
  }

  public void setSiteChatConversationId(int siteChatConversationId) {
    this.siteChatConversationId = siteChatConversationId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
