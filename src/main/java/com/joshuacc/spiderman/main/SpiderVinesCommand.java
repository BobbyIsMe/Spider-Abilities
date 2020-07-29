package com.joshuacc.spiderman.main;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.StringTag;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

public class SpiderVinesCommand extends Command {

	private Config config;

	public SpiderVinesCommand(SBMain main) {
		super("spiderability", "Enchants your chestplate with Spider Climb ability!");
		this.setPermission("spider.ability");
		this.commandParameters.put("default", new CommandParameter[] {
				new CommandParameter("ability", new String[] {
						"spider-climb", "high-jump"	
				}),
				new CommandParameter("value", CommandParamType.INT, false)
		});
		this.config = main.getConfig();
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {

		if(!(sender instanceof Player))
			return false;

		Player player = (Player) sender;
		if(player.hasPermission("spider.ability"))
		{
			switch(args.length)
			{
			case 0: 
			case 1: player.sendMessage(format("Little-Arguements Message")); break;
			case 2:
				try {
					int value = Integer.parseInt(args[1]);
					Item item = player.getInventory().getItemInHand();
					switch(args[0])
					{
					case "spider-climb":
						addEnchantment(item, player, "radius", "Chestplate", value, item.isChestplate());
						break;
					case "high-jump":
						addEnchantment(item, player, "multiplier", "Boots", value, item.isBoots());
						break;
					}
				} catch (Exception e) {
					player.sendMessage(format("Incorrect-Number Message"));
				}
				break;
			}
		} else 
			player.sendMessage(format("No-Permission Message"));
		return true;
	}
	
	private void addEnchantment(Item item, Player player, String tag, String type, int value, boolean itemCheck)
	{
		if(itemCheck)
		{
			if(value <= config.getInt("Maximum "+tag.substring(0,1).toUpperCase() + tag.substring(1)))
			{
				if(value > 0)
				{
					if(SBMain.getRadius(tag, item) == 0)
					{
						player.getInventory().remove(item);
						addLore(item, "§r"+convertRadius(getEnchant(tag), value));
						addEnchantTag(item, tag, value);
						player.getInventory().addItem(item);
						player.sendMessage(convertTag("Successful Message", tag, value));
					} else
						player.sendMessage(convertTag("Has-Enchanted Message", tag, value));
				} else
					player.sendMessage(formatResponse("Lower-Radius Message"));
			} else 
				player.sendMessage(formatResponse("Higher-Radius Message"));
		} else 
			player.sendMessage(formatResponse("Incorrect-Item-"+type+" Message"));
	}
	
	private String getEnchant(String tag)
	{
		if(tag == "radius")
			return config.getString("Spider-Climb Enchantment");
		else if(tag == "multiplier")
			return config.getString("High-Jump Enchantment");
		return  "";
	}
	
	private String convertTag(String sel, String tag, int value)
	{
		return format(config.getString(sel).replace("%ENCHANT%", getEnchant(tag)).replace("%LEVEL%", Integer.toString(value)));
	}
	
	private String convertRadius(String sel, int value)
	{
		return format(sel.replace("%LEVEL%", Integer.toString(value)));
	}
	
	private String formatResponse(String message)
	{
		return TextFormat.colorize('&', config.getString(message));
	}

	private String format(String message)
	{
		return TextFormat.colorize('&', message);
	}

	private void addEnchantTag(Item item, String type, int radius)
	{
		CompoundTag tag;
		if(!item.hasCompoundTag())
			tag = new CompoundTag();
		else
			tag = item.getNamedTag();
		tag.putInt(type, radius);

		item.setNamedTag(tag);
	}

	private void addLore(Item item, String line) {
		CompoundTag tag;
		if(item.hasCompoundTag())
			tag = item.getNamedTag();
		else
			tag = new CompoundTag();

		if(!tag.contains("display"))
			tag.putCompound("display", new CompoundTag("display").putList(new ListTag<StringTag>("Lore").add(new StringTag("", line))));
		else
			tag.getCompound("display").getList("Lore", StringTag.class).add(new StringTag("", line));

		item.setNamedTag(tag);
	}
}
