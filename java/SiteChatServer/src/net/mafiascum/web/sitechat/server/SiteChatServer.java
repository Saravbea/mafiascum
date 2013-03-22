package net.mafiascum.web.sitechat.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.http.HttpServletRequest;

import net.mafiascum.provider.Provider;
import net.mafiascum.web.sitechat.server.conversation.SiteChatConversation;
import net.mafiascum.web.sitechat.server.conversation.SiteChatConversationMessage;
import net.mafiascum.web.sitechat.server.conversation.SiteChatConversationWithUserList;
import net.mafiascum.web.sitechat.server.inboundpacket.SiteChatInboundPacketType;
import net.mafiascum.web.sitechat.server.inboundpacket.operator.SiteChatInboundConnectPacketOperator;
import net.mafiascum.web.sitechat.server.inboundpacket.operator.SiteChatInboundLeaveConversationPacketOperator;
import net.mafiascum.web.sitechat.server.inboundpacket.operator.SiteChatInboundLogInPacketOperator;
import net.mafiascum.web.sitechat.server.inboundpacket.operator.SiteChatInboundLookupUserPacketOperator;
import net.mafiascum.web.sitechat.server.inboundpacket.operator.SiteChatInboundPacketOperator;
import net.mafiascum.web.sitechat.server.inboundpacket.operator.SiteChatInboundSendMessagePacketOperator;
import net.mafiascum.web.sitechat.server.outboundpacket.SiteChatOutboundConnectPacket;
import net.mafiascum.web.sitechat.server.outboundpacket.SiteChatOutboundPacket;
import net.mafiascum.web.sitechat.server.outboundpacket.SiteChatOutboundUserJoinPacket;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import com.google.gson.Gson;

public class SiteChatServer extends Server implements SignalHandler {
  
  protected boolean _verbose;

  protected WebSocket webSocket;
  protected SelectChannelConnector selectChannelConnector;
  protected WebSocketHandler webSocketHandler;
  protected ResourceHandler resourceHandler;

  protected Provider provider = null;
  
  protected ConcurrentLinkedQueue<SiteChatWebSocket> descriptors = new ConcurrentLinkedQueue<SiteChatWebSocket>();
  protected Map<Integer, SiteChatConversationWithUserList> siteChatConversationWithMemberListMap = new HashMap<Integer, SiteChatConversationWithUserList>();
  protected Map<Integer, SiteChatUser> siteChatUserMap = new HashMap<Integer, SiteChatUser>();
  protected List<SiteChatConversationMessage> siteChatConversationMessagesToSave = new LinkedList<SiteChatConversationMessage>();
  
  protected final int MESSAGE_BATCH_SIZE = 100;
  protected int topSiteChatConversationMessageId;
  
  protected static final Map<SiteChatInboundPacketType, SiteChatInboundPacketOperator> siteChatInboundPacketTypeToSiteChatInboundPacketOperatorMap = new HashMap<SiteChatInboundPacketType, SiteChatInboundPacketOperator>();
  static {
    siteChatInboundPacketTypeToSiteChatInboundPacketOperatorMap.put(SiteChatInboundPacketType.connect, new SiteChatInboundConnectPacketOperator());
    siteChatInboundPacketTypeToSiteChatInboundPacketOperatorMap.put(SiteChatInboundPacketType.login, new SiteChatInboundLogInPacketOperator());
    siteChatInboundPacketTypeToSiteChatInboundPacketOperatorMap.put(SiteChatInboundPacketType.sendMessage, new SiteChatInboundSendMessagePacketOperator());
    siteChatInboundPacketTypeToSiteChatInboundPacketOperatorMap.put(SiteChatInboundPacketType.leaveConversation, new SiteChatInboundLeaveConversationPacketOperator());
    siteChatInboundPacketTypeToSiteChatInboundPacketOperatorMap.put(SiteChatInboundPacketType.lookupUser, new SiteChatInboundLookupUserPacketOperator());
  }
  
  public static class SiteChatInboundPacketSkeleton {
    
    public String command;
  }
  
  public SiteChatServer(int port, Provider provider) throws Exception
  {
    this.provider = provider;
    Connection connection = provider.getConnection();
    
    //Record Process ID.
    if(!provider.getIsWindows()) {
      recordProcessId(provider.getDocRoot());
    }
    
    //Add handler for interruption signal.
    Signal.handle(new Signal("INT"), this);
    Signal.handle(new Signal("TERM"), this);
    
    selectChannelConnector = new SelectChannelConnector();
    selectChannelConnector.setPort(port);

    addConnector(selectChannelConnector);
    webSocketHandler = new WebSocketHandler()
    {
      public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol)
      {
        if(protocol != null && protocol.equals("site-chat"))
        {
          webSocket = new SiteChatWebSocket();
          descriptors.add((SiteChatWebSocket)webSocket);
        }
        
        return webSocket;
      }
    };

    setHandler(webSocketHandler);
    
    System.out.println("Loading Site Chat Users...");
    this.siteChatUserMap = SiteChatUtil.loadSiteChatUserMap(connection);
    
    System.out.println("Loading Site Chat Conversations...");
    List<SiteChatConversation> siteChatConversationList = SiteChatUtil.getSiteChatConversations(connection);
    for(SiteChatConversation siteChatConversation : siteChatConversationList) {
      
      SiteChatConversationWithUserList siteChatConversationWithUserList = new SiteChatConversationWithUserList();
      siteChatConversationWithUserList.setSiteChatConversation(siteChatConversation);
      
      siteChatConversationWithMemberListMap.put(siteChatConversation.getId(), siteChatConversationWithUserList);
    }
    
    System.out.println("Loading Top Site Chat Conversation Message ID...");
    topSiteChatConversationMessageId = SiteChatUtil.getTopSiteChatConversationMessageId(connection);
    
    resourceHandler=new ResourceHandler();
    resourceHandler.setDirectoriesListed(true);
    resourceHandler.setResourceBase("src/test/webapp");
    webSocketHandler.setHandler(resourceHandler);
    
    connection.commit();
    connection.close();
  }
  
  public SiteChatConversationWithUserList getSiteChatConversationWithUserList(String siteChatConversationName) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    
    SiteChatConversation siteChatConversation = null;
    for(Integer siteChatConversationId : siteChatConversationWithMemberListMap.keySet()) {
      
      SiteChatConversationWithUserList siteChatConversationWithUserList = siteChatConversationWithMemberListMap.get(siteChatConversationId);
      siteChatConversation = siteChatConversationWithUserList.getSiteChatConversation();
      
      if(siteChatConversation.getName().equalsIgnoreCase(siteChatConversationName)) {
            
        return siteChatConversationWithUserList;
      }
    }
    
    return null;
  }
  
  public SiteChatConversationWithUserList getSiteChatConversationWithUserList(int siteChatConversationId) {
    
    return siteChatConversationWithMemberListMap.get(siteChatConversationId);
  }
  
  protected void addSiteChatUser(SiteChatUser siteChatUser) {
    
    siteChatUserMap.put(siteChatUser.getId(), siteChatUser);
  }
  
  public Map<Integer, SiteChatUser> getSiteChatUserMap(Collection<Integer> userIdCollection) {
    
    Map<Integer, SiteChatUser> siteChatUserMap = new HashMap<Integer, SiteChatUser>();
    
    for(Integer userId : userIdCollection) {
      
      SiteChatUser siteChatUser = this.siteChatUserMap.get(userId);
      
      if(siteChatUser != null) {
        
        siteChatUserMap.put(siteChatUser.getId(), siteChatUser);
      }
      else{
        
        System.out.println("getSiteChatUserMap() : Could not find user #" + userId + ".");
      }
    }
    
    return siteChatUserMap;
  }
  
  public SiteChatConversationWithUserList createSiteChatConversation(String siteChatConversationName, int userId) throws Exception {
    
    SiteChatConversation siteChatConversation = new SiteChatConversation();
    siteChatConversation.setCreatedByUserId(userId);
    siteChatConversation.setCreatedDatetime(new Date());
    siteChatConversation.setName(siteChatConversationName);
    
    Connection connection = provider.getConnection();
    SiteChatUtil.putSiteChatConversation(connection, siteChatConversation);
    connection.close();
    
    SiteChatConversationWithUserList siteChatConversationWithUserList = new SiteChatConversationWithUserList();
    siteChatConversationWithUserList.setSiteChatConversation(siteChatConversation);
    siteChatConversationWithUserList.getUserIdSet().add(userId);
    
    siteChatConversationWithMemberListMap.put(siteChatConversation.getId(), siteChatConversationWithUserList);
    
    return siteChatConversationWithUserList;
  }
  
  public SiteChatUser getSiteChatUser(int userId) {
    
    //TODO: Add code to look for user ID that does not exist in the cache.
    return siteChatUserMap.get(userId);
  }
  
  public int recordSiteChatConversationMessage(int userId, int siteChatConversationId, String message) throws Exception {
    
    SiteChatConversationMessage siteChatConversationMessage = new SiteChatConversationMessage();
    siteChatConversationMessage.setMessage(message);
    siteChatConversationMessage.setCreatedDatetime(new Date());
    siteChatConversationMessage.setSiteChatConversationId(siteChatConversationId);
    siteChatConversationMessage.setUserId(userId);
    siteChatConversationMessage.setId(++topSiteChatConversationMessageId);
    
    siteChatConversationMessagesToSave.add(siteChatConversationMessage);
    
    if(siteChatConversationMessagesToSave.size() >= MESSAGE_BATCH_SIZE) {
      
      saveSiteChatConversationMessages();
    }
    
    return siteChatConversationMessage.getId();
  }
  
  public void saveSiteChatConversationMessages() throws Exception {

    if(siteChatConversationMessagesToSave.size() > 0) {
      Connection connection = provider.getConnection();
      SiteChatUtil.putNewSiteChatConversationMessages(connection, siteChatConversationMessagesToSave);
      connection.close();
    
      siteChatConversationMessagesToSave.clear();
    }
  }

  public void processInboundDataPacket(SiteChatWebSocket webSocket, String data) {
    
    SiteChatInboundPacketSkeleton siteChatInboundPacketSkeleton = null;
    SiteChatInboundPacketType siteChatInboundPacketType = null;
    SiteChatInboundPacketOperator siteChatInboundPacketOperator = null;
    try {
      
      siteChatInboundPacketSkeleton = new Gson().fromJson(data, SiteChatInboundPacketSkeleton.class);
      siteChatInboundPacketType = SiteChatInboundPacketType.getEnumByStandardName(siteChatInboundPacketSkeleton.command);
      
      if(siteChatInboundPacketType == null) {
        
        throw new Exception("Could not find processor for command `" + siteChatInboundPacketSkeleton.command + "`");
      }
      
      siteChatInboundPacketOperator = siteChatInboundPacketTypeToSiteChatInboundPacketOperatorMap.get(siteChatInboundPacketType);
      
      siteChatInboundPacketOperator.process(this, webSocket, data);
    }
    catch(Throwable throwable) {
      
      throwable.printStackTrace();
    }
  }
  
  public void sendOutboundPacketToUsers(Set<Integer> userIdSet, SiteChatOutboundPacket siteChatOutboundPacket, Integer excludeUserId) throws IOException {
    
    for(SiteChatWebSocket siteChatWebSocket : descriptors) {
      
      if(siteChatWebSocket.getSiteChatUser() != null) {
        
        for(Integer userId : userIdSet) {
          
          if(excludeUserId != null && excludeUserId == userId) {
            
            continue;
          }
          
          if(siteChatWebSocket.getSiteChatUser().getId() == userId) {
            
            try {
              siteChatWebSocket.sendOutboundPacket(siteChatOutboundPacket);
            }
            catch(IOException ioException) {
              
              System.out.println("Could not send outbound packet: " + ioException.getMessage());
            }
          }
        }
      }
    }
  }
  
  public void attemptJoinConversation(int siteChatUserId, int siteChatConversationId, boolean notifyUser, boolean notifyConversationMembers) throws IOException {
    
    SiteChatUser siteChatUser = siteChatUserMap.get(siteChatUserId);
    SiteChatConversationWithUserList siteChatConversationWithUserList = siteChatConversationWithMemberListMap.get(siteChatConversationId);
    SiteChatConversation siteChatConversation = siteChatConversationWithUserList.getSiteChatConversation();
    Map<Integer, SiteChatUser> siteChatUserMap = getSiteChatUserMap(siteChatConversationWithUserList.getUserIdSet());
    
    //Add user to user list.
    System.out.println("Adding user #" + siteChatUser.getId() + " to chat #" + siteChatConversationWithUserList.getSiteChatConversation().getId() + ".");
    siteChatConversationWithUserList.getUserIdSet().add(siteChatUser.getId());
    
    //Generate response packet to user.
    if(notifyUser) {
      SiteChatOutboundConnectPacket siteChatOutboundConnectPacket = new SiteChatOutboundConnectPacket();
      siteChatOutboundConnectPacket.setWasSuccessful(true);
      siteChatOutboundConnectPacket.setSiteChatConversationId(siteChatConversationWithUserList.getSiteChatConversation().getId());
      siteChatOutboundConnectPacket.setTitleText(siteChatConversation.getName());
    
      Set<SiteChatUser> siteChatUserSet = new HashSet<SiteChatUser>();
    
      for(Integer userId : siteChatUserMap.keySet()) {
      
        siteChatUserSet.add(siteChatUserMap.get(userId));
      }
      
      siteChatOutboundConnectPacket.setUsers(siteChatUserSet);
      
      for(SiteChatWebSocket siteChatWebSocket : descriptors) {
        
        if(siteChatWebSocket.getSiteChatUser() != null && siteChatWebSocket.getSiteChatUser().getId() == siteChatUserId) {
          
          try {
            siteChatWebSocket.sendOutboundPacket(siteChatOutboundConnectPacket);
          }
          catch(IOException ioException) {
            ioException.printStackTrace();
          }
        }
      }
    }
    
    //Notify all users in the chat room that this user has joined.
    if(notifyConversationMembers) {
      SiteChatOutboundUserJoinPacket siteChatOutboundUserJoinPacket = new SiteChatOutboundUserJoinPacket();
      siteChatOutboundUserJoinPacket.setSiteChatConversationId(siteChatConversationWithUserList.getSiteChatConversation().getId());
      siteChatOutboundUserJoinPacket.setSiteChatUser(siteChatUser);
    
      sendOutboundPacketToUsers(siteChatUserMap.keySet(), siteChatOutboundUserJoinPacket, siteChatUser.getId());
    }
  }
  
  public void cleanup() throws Exception {
    
    System.out.println("Saving queued conversation messages. Number in buffer: " + siteChatConversationMessagesToSave.size());
    
    saveSiteChatConversationMessages();
  }
  
  public boolean authenticateUserLogin(int userId, String sessionId) throws Exception {
    
    Connection connection = provider.getConnection();
    boolean result = SiteChatUtil.authenticateUserLogin(connection, userId, sessionId);
    connection.close();
    
    return result;
  }
  
  public boolean isVerbose()
  {
    return _verbose;
  }

  public void setVerbose(boolean verbose)
  {
    _verbose = verbose;
  }

  public void setResourceBase(String dir)
  {
    resourceHandler.setResourceBase(dir);
  }

  public String getResourceBase()
  {
    return resourceHandler.getResourceBase();
  }

  private static void usage()
  {
    System.out.println("java -cp CLASSPATH "+SiteChatServer.class+" [ OPTIONS ]");
    System.out.println("  -p|--port PORT  (default 8080)");
    System.out.println("  -v|--verbose ");
    System.out.println("  -d|--docroot file (default '.')");
    System.exit(1);
  }
  
  public static void main(String... args)
  {
    try
    {
      int port=8080;
      boolean verbose=false;
      String docRoot=".";
      
      for (int i=0;i<args.length;i++)
      {
        String a=args[i];
        if ("-p".equals(a)||"--port".equals(a))
          port=Integer.parseInt(args[++i]);
        else if ("-v".equals(a)||"--verbose".equals(a))
          verbose=true;
        else if ("-d".equals(a)||"--docroot".equals(a))
          docRoot=args[++i];
        else if (a.startsWith("-"))
          usage();
      }
      
      Provider provider = new Provider();
      provider.setDocRoot(docRoot);
      provider.loadConfiguration(docRoot + "/" + "config.txt");
      
      SiteChatServer server = new SiteChatServer(port, provider);
      server.setVerbose(verbose);
      server.setResourceBase(docRoot);
      server.start();
      server.join();
      
      System.out.println("Server has been stopped.\n");
      
      server.cleanup();
      
      System.exit(1);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public class SiteChatWebSocket implements WebSocket, WebSocket.OnFrame, WebSocket.OnBinaryMessage, WebSocket.OnTextMessage, WebSocket.OnControl
  {
    protected FrameConnection connection;
    protected SiteChatUser siteChatUser;
    
    public SiteChatUser getSiteChatUser() {
      
      return siteChatUser;
    }
    
    public void setSiteChatUser(SiteChatUser siteChatUser) {
      
      this.siteChatUser = siteChatUser;
    }
    
    public FrameConnection getConnection()
    {
      return connection;
    }
    
    public void onOpen(Connection connection)
    {
      if (_verbose)
        System.out.printf("%s#onOpen %s\n",this.getClass().getSimpleName(),connection);
    }
    
    public void onHandshake(FrameConnection connection)
    {
      if (_verbose)
        System.out.printf("%s#onHandshake %s %s\n",this.getClass().getSimpleName(),connection,connection.getClass().getSimpleName());
      this.connection = connection;
    }

    public void onClose(int code,String message)
    {
      if (_verbose)
        System.out.printf("%s#onDisonnect %d %s\n",this.getClass().getSimpleName(),code,message);
      
      descriptors.remove(this);
    }
    
    public boolean onFrame(byte flags, byte opcode, byte[] data, int offset, int length)
    {      
      if (_verbose)
        System.out.printf("%s#onFrame %s|%s %s\n",this.getClass().getSimpleName(),TypeUtil.toHexString(flags),TypeUtil.toHexString(opcode),TypeUtil.toHexString(data,offset,length));
      return false;
    }

    public boolean onControl(byte controlCode, byte[] data, int offset, int length)
    {
      if (_verbose)
        System.out.printf("%s#onControl  %s %s\n",this.getClass().getSimpleName(),TypeUtil.toHexString(controlCode),TypeUtil.toHexString(data,offset,length));      
      return false;
    }

    public void onMessage(String data)
    {
      if (_verbose)
        System.out.printf("%s#onMessage   %s\n",this.getClass().getSimpleName(),data);
      
      try {
        
        processInboundDataPacket(this, data);
      }
      catch(Throwable throwable) {
        
        throwable.printStackTrace();
        return;
      }
    }

    public void onMessage(byte[] data, int offset, int length)
    {
      if (_verbose)
        System.out.printf("%s#onMessage   %s\n",this.getClass().getSimpleName(),TypeUtil.toHexString(data,offset,length));
    }
    
    public void sendOutboundPacket(SiteChatOutboundPacket siteChatOutboundPacket) throws IOException {
      
      String siteChatOutboundPacketJson = new Gson().toJson(siteChatOutboundPacket);
      
      this.getConnection().sendMessage(siteChatOutboundPacketJson);
    }
  }
  
  public void recordProcessId(String docRoot) throws IOException, URISyntaxException {

    String processName = ManagementFactory.getRuntimeMXBean().getName();
    
    FileWriter fileWriter = new FileWriter(new File(new URI(docRoot + "/ProcessID")));
    fileWriter.write(processName.substring(0, processName.indexOf("@")));
    fileWriter.close();
  }

  public void handle(Signal signal) {
    
    try {
      
      stop();
      System.out.println("Attempting to shut down Web Socket Server...");
    }
    catch(Exception exception) {
      
      System.out.println("Could not shut down Web Socket Server:\n");
      exception.printStackTrace();
    }
    
  }
}