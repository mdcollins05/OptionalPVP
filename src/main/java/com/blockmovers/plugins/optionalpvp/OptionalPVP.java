package com.blockmovers.plugins.optionalpvp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OptionalPVP extends JavaPlugin implements Listener {

    static final Logger log = Logger.getLogger("Minecraft"); //set up our logger
    private Scheduler sched = new Scheduler(this);
    private Random randomGenerator = new Random();
    public Map<String, Long> PVPtime = new HashMap();
    public Map<String, Long> reqPVPTime = new HashMap();
    public Map<String, Boolean> PVPToggle = new HashMap();

    @Override
    public void onEnable() {
        PluginDescriptionFile pdffile = this.getDescription();
        PluginManager pm = this.getServer().getPluginManager(); //the plugin object which allows us to add listeners later on

        pm.registerEvents(this, this);

        this.loadConfiguration();

        List<World> worlds = this.getServer().getWorlds();

        getServer().getScheduler().scheduleSyncRepeatingTask(this, this.sched, 60L, 60L);

        for (World w : worlds) {
            if (!w.getPVP()) {
                log.info(pdffile.getName() + ": Warning: PVP disabled on world: " + w.getName());
            }
        }

        log.info(pdffile.getName() + " version " + pdffile.getVersion() + " is enabled.");
    }

    @Override
    public void onDisable() {
        PluginDescriptionFile pdffile = this.getDescription();

        log.info(pdffile.getName() + " version " + pdffile.getVersion() + " is disabled.");
    }

    public boolean onCommand(CommandSender cs, Command cmd, String alias, String[] args) {
        Player target = null;
        if (cmd.getName().equalsIgnoreCase("pvp")) {
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("version")) {
                    PluginDescriptionFile pdf = this.getDescription();
                    cs.sendMessage(pdf.getName() + " " + pdf.getVersion() + " by MDCollins05");
                    return true;
                } else if (args[0].equalsIgnoreCase("toggle") || args[0].equalsIgnoreCase("t")) {
                    if (cs instanceof Player) {
                        Player s = (Player) cs;
                        if (s.hasPermission("opvp.toggle") || this.getPVP(s.getName())) {
                            target = (Player) s;
                        } else {
                            s.sendMessage(ChatColor.RED + "You don't have permission to do that!");
                            return false;
                        }
                    } else {
                        cs.sendMessage(ChatColor.RED + "Console cannot run that command.");
                        return false;
                    }
                } else {
                    return false;
                }
                if (this.togglePVP(target.getName())) {
                    cs.sendMessage(ChatColor.GREEN + "PVP disabled!");
                } else {
                    cs.sendMessage(ChatColor.GREEN + "PVP enabled!");
                }
            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Entity att = event.getDamager();
        Player attacker = null;
        if (att instanceof Player) {
            attacker = (Player) att;
        } else if (att instanceof Projectile) {
            Projectile arr = (Projectile) att;
            Entity e = arr.getShooter();
            if (e instanceof Player) {
                attacker = (Player) e;
            }
        }

        if (event.getEntity() instanceof Player && attacker instanceof Player) {
            if (attacker.getName().equalsIgnoreCase(((Player) event.getEntity()).getName())) {
                return;
            }
            if (!this.doPVPStuff(attacker, (Player) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.log.info(String.valueOf("[OptionalPVP] " + this.removePlayer(event.getPlayer().getName())) + " entries removed from player quit: " + event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        this.log.info(String.valueOf("[OptionalPVP] " + this.removePlayer(event.getEntity().getName())) + " entries removed from player death: " + event.getEntity().getName());
        if (this.chance(5, 100)) {
            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            skullMeta.setOwner(event.getEntity().getName());
            skull.setItemMeta(skullMeta);
            event.getDrops().add(skull);
        }
    }

    private void loadConfiguration() {
        if (this.getConfig().getConfigurationSection("PVPToggle") != null) {
            Map<String, Object> replantItems = this.getConfig().getConfigurationSection("PVPToggle").getValues(true);
            for (Map.Entry<String, Object> entry : replantItems.entrySet()) {
                this.PVPToggle.put(entry.getKey(), (Boolean) entry.getValue());
            }
        }
        this.log.info(getDescription().getName() + " --Loaded " + this.PVPToggle.size() + " player preferences!");
    }

    private boolean togglePVP(String p) {
        if (this.getPVP(p)) {
            this.PVPToggle.remove(p);
            this.getConfig().set("PVPToggle." + p, null);
            this.saveConfig();
            return false;
        } else {
            this.PVPToggle.put(p, true);
            this.getConfig().set("PVPToggle." + p, true);
            this.saveConfig();
            return true;
        }
    }

    private boolean getPVP(String p) {
        if (this.PVPToggle.containsKey(p)) {
            return this.PVPToggle.get(p);
        }
        return false;
    }

    private Boolean doPVPStuff(Player attacker, Player attacked) {
        String attackerName = attacker.getName();
        String attackedName = attacked.getName();
        String names = attackerName + ";" + attackedName;
        String namesR = attackedName + ";" + attackerName;
        String namesSorted = this.sortNames(attackerName, attackedName);
        Long curTime = this.getCurTimestamp();

        if (!attacker.getGameMode().equals(GameMode.SURVIVAL)) {
            return false;
        }

        if (this.getPVP(attackedName)) {
            attacker.sendMessage(ChatColor.RED + attackedName + " has toggled PVP off!");
            return false;
        }

        if (this.getPVP(attackerName)) {
            attacker.sendMessage(ChatColor.RED + "You have disabled PVP! Use /pvp toggle to change your mode!");
            return false;
        }

        if (this.PVPtime.containsKey(namesSorted)) { // engaged in pvp
            Long battleLast = curTime - this.PVPtime.get(namesSorted);
            if (battleLast < 15L) {
                this.PVPtime.put(namesSorted, curTime); // update the time as they are still in battle
                return true;
            } else { // after the time has expired, delete all the things!
                if (this.reqPVPTime.containsKey(names)) {
                    this.reqPVPTime.remove(names);
                }
                if (this.reqPVPTime.containsKey(namesR)) {
                    this.reqPVPTime.remove(namesR);
                }
                this.PVPtime.remove(namesSorted);
                attacker.sendMessage(ChatColor.RED + "PVP has ended with " + attackedName + "!");
                attacked.sendMessage(ChatColor.RED + "PVP has ended with " + attackerName + "!");
                return false;
            }
        }
        //not in pvp cause we got this far
        if (this.reqPVPTime.containsKey(names)) {
            Long reqLast = curTime - this.reqPVPTime.get(names);
            if (reqLast < 10L) {
                attacker.sendMessage(ChatColor.RED + attackedName + " has not accepted your request!");
                return false;
            } else {
                this.reqPVPTime.put(names, curTime);
            }
        } else {
            this.reqPVPTime.put(names, curTime);
        }

        if (this.reqPVPTime.containsKey(namesR)) {
            Long reqLast = curTime - this.reqPVPTime.get(namesR);
            if (reqLast > 10L) {
                this.reqPVPTime.remove(namesR);
            }
        }

        if (!this.reqPVPTime.containsKey(namesR)) {
            attacker.sendMessage(ChatColor.RED + attackedName + " not in PVP mode with you!");
            attacked.sendMessage(ChatColor.RED + attackerName + " wants to PVP you! Hit 'em back to enter PVP!");
            return false;
        } else {
            this.PVPtime.put(namesSorted, curTime);
            attacker.sendMessage(ChatColor.GREEN + "You have entered into PVP mode with " + attackedName + "!");
            attacked.sendMessage(ChatColor.GREEN + attackerName + " has entered into PVP mode with you!");
            return false;
        }
    }

    public String sortNames(String name1, String name2) {
        List<String> names = new ArrayList();
        names.add(name1);
        names.add(name2);
        Collections.sort(names);
        return names.get(0) + ";" + names.get(1);
    }

    public Long getCurTimestamp() {
        return System.currentTimeMillis() / 1000L;
    }

    private Integer removePlayer(String p) {
        Integer count = 0;
        HashMap pvptime = new HashMap(this.PVPtime);
        Iterator pvptimeE = pvptime.entrySet().iterator();

        while (pvptimeE.hasNext()) {
            Map.Entry entry = (Map.Entry) pvptimeE.next();
            String key = (String) entry.getKey();
            if (key.contains(p + ";")) {
                this.PVPtime.remove(key);
                count++;
            }
            if (key.contains(";" + p)) {
                this.PVPtime.remove(key);
                count++;
            }
        }

        HashMap reqpvptime = new HashMap(this.reqPVPTime);
        Iterator reqpvptimeE = reqpvptime.entrySet().iterator();

        while (reqpvptimeE.hasNext()) {
            Map.Entry entry = (Map.Entry) reqpvptimeE.next();
            String key = (String) entry.getKey();
            if (key.contains(p + ";")) {
                this.reqPVPTime.remove(key);
                count++;
            }
            if (key.contains(";" + p)) {
                this.reqPVPTime.remove(key);
                count++;
            }
        }
        return count;
    }

    public boolean chance(Integer percent, Integer ceiling) {
        Integer randomInt = this.random(ceiling);
        if (randomInt < percent) {
            return true;
        }
        return false;
    }

    public Integer random(Integer ceil) {
        Integer randomInt = this.randomGenerator.nextInt(ceil * 1000); //moar random?
        Integer value = randomInt / 1000; //I think so, so now we fix that and round
        return value;
    }
}
