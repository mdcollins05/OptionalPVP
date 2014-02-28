/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.blockmovers.plugins.optionalpvp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.ChatColor;

/**
 *
 * @author MattC
 */
public class Scheduler implements Runnable {

    OptionalPVP plugin = null;

    public Scheduler(OptionalPVP plugin) {
        this.plugin = plugin;
    }

    public void run() {
        if (!this.plugin.PVPtime.isEmpty()) {
            HashMap pvptime = new HashMap(this.plugin.PVPtime);

            Iterator entries = pvptime.entrySet().iterator();

            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                String key = (String) entry.getKey();
                String[] keyS = key.split(";");
                String keyR = keyS[1] + ";" + keyS[0];
                String namesSorted = this.plugin.sortNames(keyS[0], keyS[1]);
                String attackedName = keyS[0];
                String attackerName = keyS[1];
                Long value = (Long) entry.getValue();
                Long curTime = this.plugin.getCurTimestamp();
                
                Long battleLast = curTime - value;
                if (battleLast > 15L) {
                    if (this.plugin.reqPVPTime.containsKey(key)) {
                        this.plugin.reqPVPTime.remove(key);
                    }
                    if (this.plugin.reqPVPTime.containsKey(keyR)) {
                        this.plugin.reqPVPTime.remove(keyR);
                    }
                    this.plugin.PVPtime.remove(namesSorted);
                    if (this.plugin.getServer().getPlayerExact(attackerName) != null) {
                    this.plugin.getServer().getPlayerExact(attackerName).sendMessage(ChatColor.RED + "PVP has ended with " + attackedName + "!");
                    }
                    if (this.plugin.getServer().getPlayerExact(attackedName) != null) {
                    this.plugin.getServer().getPlayerExact(attackedName).sendMessage(ChatColor.RED + "PVP has ended with " + attackerName + "!");
                    }
                    return;
                }
            }
        }
    }
}
