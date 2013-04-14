package plugin.gunther.chatsupport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatSupport extends JavaPlugin
  implements Listener
{
  Logger log;
  File chatcachefile;
  FileConfiguration chatcache;
  File messagesfile;
  FileConfiguration messages;

  public void onEnable()
  {
    getServer().getPluginManager().registerEvents(this, this);
    cachestart();
    messagesstart();
    this.log = getLogger();

    if (this.messages.getBoolean("queue.scheduledqueue", false)) {
      double delay = this.messages.getDouble("queue.frequency", 5.0D);
      long scheduleddelay = (long)(delay * 1200.0D);
      getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
        public void run() {
          for (Player p : ChatSupport.this.getServer().getOnlinePlayers())
            if ((p.hasPermission("support.scheduledqueue")) || (ChatSupport.this.nopermop(p)))
              ChatSupport.this.listqueue(p);
        }
      }
      , scheduleddelay, scheduleddelay);
    }
  }

  public void onDisable()
  {
  }

  public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
  {
    if ((sender instanceof Player)) {
      Player playerdummy = (Player)sender;
      if (args.length == 0) {
        if ((playerdummy.hasPermission("support.request")) || (nopermdefault(playerdummy))) {
          supportrequest(sender);
        }
        else {
          playerdummy.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("support.nopermission")));
        }

      }
      else if ((args[0].equalsIgnoreCase("join")) && (args.length > 1)) {
        if ((playerdummy.hasPermission("support.join")) || (nopermop(playerdummy))) {
          joinChat(playerdummy, args);
        }
        else {
          playerdummy.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("support.nopermission")));
        }

      }
      else if (args[0].equalsIgnoreCase("leave")) {
        leaveChat(playerdummy);
      }
      else if ((args[0].equalsIgnoreCase("help")) && (args.length >= 2)) {
        if ((playerdummy.hasPermission("support.join")) || (nopermop(playerdummy))) {
          joinChatbyPlayer(playerdummy, args);
        }
        else {
          playerdummy.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("support.nopermission")));
        }

      }
      else if (args[0].equalsIgnoreCase("chat")) {
        publicchat(playerdummy, args);
      }
      else if (args[0].equalsIgnoreCase("queue")) {
        if ((playerdummy.hasPermission("support.queue")) || (nopermop(playerdummy))) {
          listqueue(playerdummy);
        }
        else {
          playerdummy.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("support.nopermission")));
        }

      }
      else if ((args[0].equalsIgnoreCase("messages")) && (args.length >= 2)) {
        if ((playerdummy.hasPermission("support.messages")) || (nopermop(playerdummy))) {
          if (args[1].equalsIgnoreCase("english")) {
            changelanguage("messages.yml");
          }
          else if (args[1].equalsIgnoreCase("german")) {
            changelanguage("messagesgerman.yml");
          }
          playerdummy.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("support.messagessuccess")));
        }
        else {
          playerdummy.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("support.nopermission")));
        }

      }
      else if (args[0].equalsIgnoreCase("reload")) {
        if ((playerdummy.hasPermission("support.reload")) || (nopermop(playerdummy))) {
          try {
            this.messages.load(this.messagesfile);
          } catch (Exception e) {
            e.printStackTrace();
            this.log.info("Error with loading messages.yml");
          }
          playerdummy.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("support.reloadsuccess")));
        }
        else {
          playerdummy.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("support.nopermission")));
        }

      }
      else if (args[0].equalsIgnoreCase("commands")) {
        if ((playerdummy.hasPermission("support.commands")) || (nopermdefault(playerdummy))) {
          commands(playerdummy);
        }
        else {
          playerdummy.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("support.nopermission")));
        }

      }
      else
      {
        playerdummy.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("support.unknowncommand")));
      }

    }
    else
    {
      sender.sendMessage("You have to be a player");
    }

    return true;
  }

  @EventHandler
//  public void PlayerChat(AsyncPlayerChatEvent e)
  public void PlayerChat(PlayerChatEvent e)
  {
    Map<String, Object> chatting = this.chatcache.getConfigurationSection("Chattingplayers:").getValues(true);
    List<String> publicchat = this.chatcache.getStringList("Usingpublicchat:");
    List<String> Chatroom;
    String format;
    String format2;
    if ((chatting.containsKey(e.getPlayer().getName())) && (!publicchat.contains(e.getPlayer().getName()))) {
      e.setCancelled(true);
      String chatnumber = chatting.get(e.getPlayer().getName()).toString();
      Chatroom = this.chatcache.getStringList(chatnumber);

      format = this.messages.getString("support.chatformat");
      format2 = format.replace("{Support-Prefix}", this.messages.getString("support.prefix"));
      String format3 = format2.replace("{PlayerName}", e.getPlayer().getName());
      String format4 = colorize(format3.replace("{ChatName}", chatnumber));
      String format5 = format4.replace("{Message}", e.getMessage());

      for (Player p : getServer().getOnlinePlayers()) {
        if ((p.hasPermission("support.*")) || (p.hasPermission("support.listen")) || (Chatroom.contains(p.getName())) || (nopermop(p)))
        {
          p.sendMessage(format5);
        }
      }

    }
    else if (this.messages.getBoolean("support.hidechatinsupportchats", true)) {
      int format6 = getServer().getOnlinePlayers().length; 
      for (int i = 0; i < format6; i++) { 
          Player p = getServer().getOnlinePlayers()[i];
          if (chatting.containsKey(p.getName()))
            e.getRecipients().remove(p);
      }
    }
  }

  @EventHandler
  public void PlayerJoin(PlayerJoinEvent e)
  {
    if ((this.messages.getBoolean("queue.showonjoin", false)) && ((e.getPlayer().hasPermission("support.queueonjoin")) || (nopermop(e.getPlayer()))))
      listqueue(e.getPlayer());
  }

  @EventHandler
  public void PlayerQuits(PlayerQuitEvent e)
  {
    leaveChat(e.getPlayer());
  }

  void cachestart()
  {
    this.chatcachefile = new File(getDataFolder(), "chatcache.yml");
    if (this.chatcachefile.exists()) {
      this.chatcachefile.delete();
    }
    this.chatcachefile.getParentFile().mkdirs();
    this.chatcache = new YamlConfiguration();
    this.chatcache.options().copyDefaults(true);

    Map<?, ?> queue = new HashMap<Object, Object>();
    List<?> publicchat = new ArrayList<Object>();
    Map<?, ?> inchats = new HashMap<Object, Object>();
    List<?> cooldown = new ArrayList<Object>();

    this.chatcache.createSection("Waitingforhelp:", queue);
    this.chatcache.addDefault("Usingpublicchat:", publicchat);
    this.chatcache.createSection("Chattingplayers:", inchats);
    this.chatcache.addDefault("Incooldown:", cooldown);
    cachesave();
  }

  void cachesave()
  {
    try
    {
      this.chatcache.save(this.chatcachefile);
    } catch (IOException e) {
      e.printStackTrace();
      this.log.info("Failed to save the ChatCache");
    }
  }

  void messagesstart()
  {
    this.messagesfile = new File(getDataFolder(), "messages.yml");
    this.messages = new YamlConfiguration();

    if (this.messagesfile.exists()) {
      try {
        this.messages.load(this.messagesfile);
      } catch (Exception e) {
        e.printStackTrace();
        this.log.info("Error with loading messages.yml");
      }

    }

    this.messages.options().copyDefaults(true);

    this.messages.addDefault("support.prefix", ChatColor.RED + "[Support] ");
    this.messages.addDefault("support.chatformat", "{Support-Prefix} §f{PlayerName} [{ChatName}]: {Message}");
    this.messages.addDefault("support.hidechatinsupportchats", Boolean.valueOf(false));
    this.messages.addDefault("support.ignorepermissions.op", Boolean.valueOf(false));
    this.messages.addDefault("support.ignorepermissions.user", Boolean.valueOf(false));

    this.messages.addDefault("support.messagessuccess", "Succesfully loaded messages.yml");
    this.messages.addDefault("support.reloadsuccess", "Successfully reloaded SupportChat");
    this.messages.addDefault("support.unknowncommand", "Unknown Command. Use /command to make a request.");
    this.messages.addDefault("support.nopermission", "You don't have permission to use this command");

    this.messages.addDefault("request.allowmultiplerequests", Boolean.valueOf(true));
    this.messages.addDefault("request.usecooldown", Boolean.valueOf(true));
    this.messages.addDefault("request.cooldown", Integer.valueOf(10));
    this.messages.addDefault("request.usetoleavechat", Boolean.valueOf(true));
    this.messages.addDefault("request.toadmins", ChatColor.WHITE + "%1$s is requesting support. He is in chat %2$d");
    this.messages.addDefault("request.success", ChatColor.WHITE + "All responsible players have been informed about your request.");
    this.messages.addDefault("request.noadmins", ChatColor.WHITE + "Sorry, but there is no one online to help you.");
    this.messages.addDefault("request.alreadyrequested.multiplerequests.true", ChatColor.WHITE + "There has been sent another request to all responsible players.");
    this.messages.addDefault("request.alreadyrequested.multiplerequests.false", ChatColor.WHITE + "Sorry, but you already requested help");
    this.messages.addDefault("request.alreadyrequested.incooldown", ChatColor.WHITE + "You have to wait a little bit before you can request help again.");
    this.messages.addDefault("request.alreadyinchat", ChatColor.WHITE + "You already joined a support-chat.");

    this.messages.addDefault("join.singleplayer", ChatColor.WHITE + "%s joined the support-chat to help you.");
    this.messages.addDefault("join.multipleplayers", ChatColor.WHITE + "%s joined the support-chat.");
    this.messages.addDefault("join.success", ChatColor.WHITE + "You successfully joined a support-chat.");
    this.messages.addDefault("join.alreadyinchat", ChatColor.WHITE + "You already joined a support-chat. You have to leave it first.");
    this.messages.addDefault("join.abondened", ChatColor.WHITE + "That support-chat is abandoned.");
    this.messages.addDefault("join.notexisting", ChatColor.WHITE + "That support-chat isn't existing.");

    this.messages.addDefault("help.notinchatorqueue", ChatColor.WHITE + "This person isn't in a support-chat or a queue.");

    this.messages.addDefault("leave.queue.player", ChatColor.WHITE + "You're not longer in the queue.");
    this.messages.addDefault("leave.queue.toadmins", ChatColor.WHITE + "%s doesn't need support anymore.");
    this.messages.addDefault("leave.chat.success", ChatColor.WHITE + "You successfully left the support-chat.");
    this.messages.addDefault("leave.chat.toplayers", ChatColor.WHITE + "%s left the chat.");
    this.messages.addDefault("leave.chat.chatterminated", ChatColor.WHITE + "The support-chat has been terminated.");
    this.messages.addDefault("leave.notinchatorqueue", ChatColor.WHITE + "You're not in a support-chat or a queue.");

    this.messages.addDefault("commands.request", "</support>: make a request or leave the support chat, you're currently in.");
    this.messages.addDefault("commands.join", "</support join (chatroom-name)>: join a chat to help someone. You get the chat-name with the request.");
    this.messages.addDefault("commands.help", "</support help (player)>: similar to previous one but you need the playername instead of the chat-name.");
    this.messages.addDefault("commands.leave", "</support leave>: leave the support-chat or the queue, you're currently in.");
    this.messages.addDefault("commands.chat", "</support chat (message)>: allows you to chat with all online players while you're in a support-chat.");
    this.messages.addDefault("commands.reload", "</support reload>: allows you to reload the meassages.yml, after you changed it.");
    this.messages.addDefault("commands.messages", "</support messages (english/german)>: allows you to change the language and to reload a fresh messages.yml, if you messed it up.");
    this.messages.addDefault("commands.commands", "</support commands>: shows this help page again");
    this.messages.addDefault("commands.queue", "</support queue>: shows you all players, that are requesting help");

    this.messages.addDefault("queue.format", ChatColor.WHITE + "The following players are waiting for help: ");
    this.messages.addDefault("queue.empty", ChatColor.WHITE + "No one seems to need help");
    this.messages.addDefault("queue.showonjoin", Boolean.valueOf(false));
    this.messages.addDefault("queue.scheduledqueue", Boolean.valueOf(false));
    this.messages.addDefault("queue.frequency", Integer.valueOf(5));

    if (!this.messagesfile.exists())
      try {
        this.messages.save(this.messagesfile);
      } catch (IOException e) {
        e.printStackTrace();
        this.log.info("Couldn't save messages.yml");
      }
  }

  void changelanguage(String g)
  {
    try
    {
      OutputStream out = new FileOutputStream(this.messagesfile);
      InputStream in = getResource(g);
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0)
      {
        out.write(buf, 0, len);
      }
      out.close();
      in.close();
      this.messages.load(this.messagesfile);
      this.log.info("successfully loaded messages.yml");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void supportrequest(CommandSender sender)
  {
    final Player player = (Player)sender;
    Map<String, Object> queue = this.chatcache.getConfigurationSection("Waitingforhelp:").getValues(true);
    Map<String, Object> chatting = this.chatcache.getConfigurationSection("Chattingplayers:").getValues(true);

    List<String> cooldown = this.chatcache.getStringList("Incooldown:");
    int i;
    List<String> Chatroom;
    if ((!queue.containsKey(player.getName())) && (!chatting.containsKey(player.getName()))) {
      int chatname = getChatNumber();

      i = 0;
      for (Player p : getServer().getOnlinePlayers()) {
        if ((p.hasPermission("support.*")) || (p.hasPermission("support.receive")) || (nopermop(p))) {
          p.sendMessage(colorize(this.messages.getString("support.prefix") + String.format(this.messages.getString("request.toadmins"), new Object[] { player.getName(), Integer.valueOf(chatname) })));
          i++;
        }

      }

      if (i > 0) {
        player.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("request.success")));
      }
      else {
        player.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("request.noadmins")));
      }

      queue.put(player.getName(), String.valueOf(chatname));
      this.chatcache.createSection("Waitingforhelp:", queue);

      Chatroom = new ArrayList<String>();
      Chatroom.add(player.getName());
      this.chatcache.set(String.valueOf(chatname), Chatroom);

      if (this.messages.getBoolean("request.usecooldown"))
        cooldown.add(player.getName());
      this.chatcache.set("Incooldown:", cooldown);
      cachesave();
      getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
        public void run() {
          List<String> cooldown = ChatSupport.this.chatcache.getStringList("Incooldown:");
          cooldown.remove(player.getName());
          ChatSupport.this.chatcache.set("Incooldown:", cooldown);
          ChatSupport.this.cachesave();
        }
      }
      , this.messages.getInt("request.cooldown") * 20);
    }
    else if (queue.containsKey(player.getName())) {
      if (this.messages.getBoolean("request.allowmultiplerequests")) {
        if (this.messages.getBoolean("request.usecooldown")) {
          if (!cooldown.contains(player.getName())) {
            player.sendMessage(this.messages.getString("support.prefix") + this.messages.getString("request.alreadyrequested.multiplerequests.true"));
            for (Player p : getServer().getOnlinePlayers()) {
              if ((p.hasPermission("support.*")) || (p.hasPermission("support.receive")) || (nopermop(p))) {
                p.sendMessage(colorize(this.messages.getString("support.prefix") + String.format(this.messages.getString("request.toadmins"), new Object[] { player.getName(), Integer.valueOf(Integer.parseInt((String)queue.get(player.getName()))) })));
              }
            }
            cooldown.add(player.getName());
            this.chatcache.set("Incooldown:", cooldown);
            cachesave();

            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
              public void run() {
                List<String> cooldown = ChatSupport.this.chatcache.getStringList("Incooldown:");
                cooldown.remove(player.getName());
                ChatSupport.this.chatcache.set("Incooldown:", cooldown);
                ChatSupport.this.cachesave();
              }
            }
            , this.messages.getInt("request.cooldown") * 20);
          }
          else {
            player.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("request.alreadyrequested.incooldown")));
          }
        }
        else {
          for (Player p : getServer().getOnlinePlayers()) {
            if ((p.hasPermission("support.*")) || (p.hasPermission("support.receive")) || (nopermop(p))) {
              p.sendMessage(colorize(this.messages.getString("support.prefix") + String.format(this.messages.getString("request.toadmins"), new Object[] { player.getName(), Integer.valueOf(Integer.parseInt((String)queue.get(player.getName()))) })));
            }
          }
        }
      }
      else {
        player.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("request.alreadyrequested.multiplerequests.false")));
      }

    }
    else if (chatting.containsKey(player.getName())) {
      if (this.messages.getBoolean("request.usetoleavechat")) {
        leaveChat(player);
      }
      else
        player.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("request.alreadyinchat")));
    }
  }

  int getChatNumber()
  {
    for (int i = 1; i < 100; i++) {
      if (!this.chatcache.contains(String.valueOf(i))) {
        return i;
      }
    }
    return 0;
  }

  void joinChat(Player player, String[] args)
  {
    Map<String, Object> queue = this.chatcache.getConfigurationSection("Waitingforhelp:").getValues(true);
    Map<String, Object> chatting = this.chatcache.getConfigurationSection("Chattingplayers:").getValues(true);

    if ((args.length > 1) && (this.chatcache.contains(args[1]))) {
      List<String> Chatroom = this.chatcache.getStringList(args[1]);

      if (!Chatroom.contains("this chatroom is abondened")) {
        if ((!chatting.containsKey(player.getName())) && (!queue.containsKey(player.getName()))) {
          if (Chatroom.size() == 1) {
            for (String a : Chatroom) {
              Player p = getServer().getPlayer(a);
              p.sendMessage(colorize(this.messages.getString("support.prefix") + String.format(this.messages.getString("join.singleplayer"), new Object[] { player.getName() })));

              queue.remove(p.getName());
              chatting.put(p.getName(), args[1]);
            }

            this.chatcache.createSection("Waitingforhelp:", queue);

            chatting.put(player.getName(), args[1]);
            this.chatcache.createSection("Chattingplayers:", chatting);

            Chatroom.add(player.getName());
            this.chatcache.set(args[1], Chatroom);

            cachesave();
          }
          else
          {
            for (String b : Chatroom) {
              Player p = getServer().getPlayer(b);
              p.sendMessage(colorize(this.messages.getString("support.prefix") + String.format(this.messages.getString("join.multipleplayers"), new Object[] { player.getName() })));
            }

            chatting.put(player.getName(), args[1]);
            this.chatcache.createSection("Chattingplayers:", chatting);

            Chatroom.add(player.getName());
            this.chatcache.set(args[1], Chatroom);

            cachesave();
          }

          player.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("join.success")));
        }
        else
        {
          player.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("join.alreadyinchat")));
        }
      }
      else
      {
        player.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("join.abondened")));
      }
    }
    else
    {
      player.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("join.notexisting")));
    }
  }

  void joinChatbyPlayer(Player player, String[] args)
  {
    Map<String, Object> queue = this.chatcache.getConfigurationSection("Waitingforhelp:").getValues(true);
    Map<String, Object> chatting = this.chatcache.getConfigurationSection("Chattingplayers:").getValues(true);

    if (queue.containsKey(args[1])) {
      args[1] = queue.get(args[1]).toString();
      joinChat(player, args);
    }
    else if (chatting.containsKey(args[1])) {
      args[1] = chatting.get(args[1]).toString();
      joinChat(player, args);
    }
    else
    {
      player.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("help.notinchatorqueue")));
    }
  }

  void leaveChat(Player p)
  {
    Map<String, Object> queue = this.chatcache.getConfigurationSection("Waitingforhelp:").getValues(true);
    Map<String, Object> chatting = this.chatcache.getConfigurationSection("Chattingplayers:").getValues(true);

    if (queue.containsKey(p.getName())) {
      String s = queue.get(p.getName()).toString();
      queue.remove(p.getName());
      this.chatcache.createSection("Waitingforhelp:", queue);
      p.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("leave.queue.player")));

      for (Player g : getServer().getOnlinePlayers()) {
        if ((g.hasPermission("support.*")) || (g.hasPermission("support.receive")) || (nopermop(g))) {
          g.sendMessage(colorize(this.messages.getString("support.prefix") + String.format(this.messages.getString("leave.queue.toadmins"), new Object[] { p.getName() })));
        }

      }

      List<String> Chatroom = this.chatcache.getStringList(s);
      Chatroom.remove(p.getName());
      Chatroom.add("this chatroom is abondened");
      this.chatcache.set(s, Chatroom);
      cachesave();
    }
    else if (chatting.containsKey(p.getName())) {
      String s = chatting.get(p.getName()).toString();
      List<String> Chatroom = this.chatcache.getStringList(s);
      boolean k = Chatroom.size() == 2;
      Chatroom.remove(p.getName());
      chatting.remove(p.getName());
      p.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("leave.chat.success")));

      for (int i = 0; i < Chatroom.size(); i++) {
        String b = (String)Chatroom.get(i);
        Player g = getServer().getPlayerExact(b);
        g.sendMessage(colorize(this.messages.getString("support.prefix") + String.format(this.messages.getString("leave.chat.toplayers"), new Object[] { p.getName() })));
        if (k) {
          g.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("leave.chat.chatterminated")));
          chatting.remove(b);
          Chatroom.remove(b);
          Chatroom.add("this chatroom is abondened");
        }

      }

      this.chatcache.createSection("Chattingplayers:", chatting);
      this.chatcache.set(s, Chatroom);
      cachesave();
    }
    else
    {
      p.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("leave.notinchatorqueue")));
    }
  }

  void publicchat(Player player, String[] args)
  {
    List<String> publicchat = this.chatcache.getStringList("Usingpublicchat:");
    publicchat.add(player.getName());
    this.chatcache.set("Usingpublicchat:", publicchat);
    StringBuffer message = new StringBuffer();
    for (int i = 1; i < args.length; i++) {
      message.append(args[i]);
      message.append(" ");
    }
    player.chat(message.toString());
    publicchat.remove(player.getName());
    this.chatcache.set("Usingpublicchat:", publicchat);
    cachesave();
  }

  void commands(Player player)
  {
    if ((player.hasPermission("support.commands")) || (nopermdefault(player))) {
      player.sendMessage(colorize(this.messages.getString("commands.commands", "</support commands>: shows this help page again")));
    }

    if ((player.hasPermission("support.request")) || (nopermdefault(player))) {
      player.sendMessage(colorize(this.messages.getString("commands.request")));
    }

    if ((player.hasPermission("support.join")) || (nopermop(player))) {
      player.sendMessage(colorize(this.messages.getString("commands.join")));
      player.sendMessage(colorize(this.messages.getString("commands.help")));
    }

    player.sendMessage(colorize(this.messages.getString("commands.leave")));
    player.sendMessage(colorize(this.messages.getString("commands.chat")));

    if ((player.hasPermission("support.reload")) || (nopermop(player))) {
      player.sendMessage(colorize(this.messages.getString("commands.reload")));
    }

    if ((player.hasPermission("support.messages")) || (nopermop(player))) {
      player.sendMessage(colorize(this.messages.getString("commands.messages")));
    }

    if ((player.hasPermission("support.queue")) || (nopermop(player)))
      player.sendMessage(colorize(this.messages.getString("commands.queue", "</support queue>: shows you all players, that are requesting help")));
  }

  String colorize(String string)
  {
    String string2 = string.replace("&", "§");
    String string3 = string2.replace("ß", "§");
    return string3;
  }

  boolean nopermdefault(Player p) {
    if ((nopermop(p)) || (this.messages.getBoolean("support.ignorepermissions.user", false))) {
      return true;
    }

    return false;
  }

  boolean nopermop(Player p)
  {
    if ((this.messages.getBoolean("support.ignorepermissions.op", false)) && (p.isOp())) {
      return true;
    }

    return false;
  }

  void listqueue(Player p)
  {
    Map<String, Object> queue = this.chatcache.getConfigurationSection("Waitingforhelp:").getValues(true);
    StringBuffer list = new StringBuffer(this.messages.getString("queue.format", ChatColor.WHITE + "The following players are waiting for help: "));
    if (queue.size() > 0) {
      for (Player player : getServer().getOnlinePlayers()) {
        if (queue.containsKey(player.getName())) {
          list.append(player.getName() + ": " + queue.get(player.getName()) + " ");
        }
      }
      p.sendMessage(colorize(this.messages.getString("support.prefix") + list.toString()));
    }
    else {
      p.sendMessage(colorize(this.messages.getString("support.prefix") + this.messages.getString("queue.empty", new StringBuilder().append(ChatColor.WHITE).append("No one seems to need help").toString())));
    }
  }
}
