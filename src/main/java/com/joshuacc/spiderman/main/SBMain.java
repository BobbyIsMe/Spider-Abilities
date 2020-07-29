package com.joshuacc.spiderman.main;

import java.util.ArrayList;
import java.util.Iterator;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.player.PlayerJumpEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.level.particle.GenericParticle;
import cn.nukkit.level.particle.HappyVillagerParticle;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.Task;

public class SBMain extends PluginBase implements Listener {

	@Override
	public void onEnable()
	{
		saveDefaultConfig();
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getCommandMap().register("spiderability", new SpiderVinesCommand(this));
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event)
	{
		Player player = event.getPlayer();
		Item chestplate = player.getInventory().getChestplate();
		if(chestplate.hasCompoundTag())
		{
			int rad = getRadius("radius", chestplate);
			if(rad != 0)
			{
				ArrayList<Location> blocks = new ArrayList<>();
				for(int y = -rad; y <= rad; y++)
				{
					for(int f = -2; f <= 2; f++)
					{
						Vector3 vec = player.getHorizontalFacing().rotateYCCW().getUnitVector().multiply(f).add(0, y, 0);
						Location loc = player.getLocation().add(vec);

						if(loc.getLevelBlock().getId() == Block.AIR)
						{
							Block block = loc.getLevelBlock().getSide(player.getHorizontalFacing());

							if(block.getId() != Block.AIR && block.getId() != Block.VINES && block.isSolid())
							{
								Block b = Block.get(Block.VINE);
								b.setDamage(getMetaFromFace(player.getHorizontalFacing()));
								player.getLevel().setBlock(loc, b);
								blocks.add(loc);
							}
						}
					}
				}

				if(!blocks.isEmpty())
					destroyVines(player, blocks);
			}
		}
	}


	@EventHandler
	public void onHit(PlayerJumpEvent event)
	{
		Player player = event.getPlayer();
		Item boots = player.getInventory().getBoots();
		if(player.isSneaking())
		{
			int dis = getRadius("multiplier", boots);
			if(dis != 0)
			{
				if(player.isOnGround())
				{
					addParticle(player, Particle.TYPE_EVAPORATION, 40);
					player.getLevel().addSound(player, Sound.MOB_WITHER_SHOOT, 1, 2);
					player.setMotion(player.getDirectionVector().multiply(dis+.5));
				}
			}
		}
	}
	
	@EventHandler
	public void onFall(EntityDamageEvent event)
	{
		if(event.getEntity() instanceof Player)
		{
			if(event.getCause() == DamageCause.FALL)
			{
				event.setDamage(event.getDamage()/2F);
			}
		}
	}

	private void destroyVines(Player player, ArrayList<Location> blocks) 
	{
		player.getLevel().addParticle(new HappyVillagerParticle(player));
		Server.getInstance().getScheduler().scheduleRepeatingTask(new Task() {

			@Override
			public void onRun(int arg0) 
			{	
				Iterator<Location> b = blocks.iterator();
				if(player.isOnline())
				{
					Item item = player.getInventory().getChestplate();
					int radius = getRadius("radius", item);

					while(b.hasNext())
					{
						Location block = b.next();
						if(block.distance(player) >= radius+2)
							clearVines(block);

						if(block.getLevelBlock().getId() != Block.VINE)
							b.remove();
					}
				} else {
					while(b.hasNext())
					{
						Location block = b.next();
						clearVines(block);
						b.remove();
					}
				}

				if(blocks.isEmpty())
					this.cancel();
			}

		}, 3 * 20);
	}

	private void clearVines(Location block)
	{
		if(block.getLevelBlock().getId() == Block.VINE)
		{
			block.getLevel().setBlock(block, Block.get(Block.AIR));
			block.getLevel().addParticle(new DestroyBlockParticle(block, Block.get(Block.VINE)));
			block.getLevel().addSound(block, Sound.DIG_GRASS);
		}
	}

	private int getMetaFromFace(BlockFace face) {
		switch (face) {
		case SOUTH:
		default:
			return 0x01;
		case WEST:
			return 0x02;
		case NORTH:
			return 0x04;
		case EAST:
			return 0x08;
		}
	}

	private void addParticle(Player player, int p, int size)
	{
		Particle particle = new GenericParticle(player, p);
		for (int x = 0; x < size; x++) {
			particle.setComponents(
					player.x + randomWithRange(-0.7,0.4),
					player.y + randomWithRange(0.1,0.6),
					player.z + randomWithRange(-0.5,0.6)
					);
			player.getLevel().addParticle(particle);
		}
	}

	private double randomWithRange(double min, double max)
	{
		double range = Math.abs(max - min);     
		return (Math.random() * range) + (min <= max ? min : max);
	}

	public static int getRadius(String tag, Item item)
	{
		if(item.hasCompoundTag() && item.getNamedTag().contains(tag))
			return item.getNamedTag().getInt(tag);
		else
			return 0;
	}
}
