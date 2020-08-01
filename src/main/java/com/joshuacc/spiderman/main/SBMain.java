package com.joshuacc.spiderman.main;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.entity.EntitySpawnEvent;
import cn.nukkit.event.player.PlayerJumpEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.level.particle.GenericParticle;
import cn.nukkit.level.particle.HappyVillagerParticle;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.Task;
import cn.nukkit.scheduler.TaskHandler;

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
				if(player.isOnGround() && getEntityInSight(player, getDistance(player)) == null)
				{
					addParticle(player, Particle.TYPE_EVAPORATION, 40);
					player.getLevel().addSound(player, Sound.MOB_WITHER_SHOOT, 1, 2);
					player.setMotion(player.getDirectionVector().multiply(getBoost(dis, 1.2)));
				}
			}
		}
	}

	@EventHandler
	public void onJump(PlayerJumpEvent event)
	{
		Player player = event.getPlayer();
		if(player.isSneaking())
		{
			int radius = getDistance(player);
			if(radius != 0)
			{
				EntityCreature target = getEntityInSight(player, radius);
				if(target != null && player.hasEffect(Effect.POISON) && player.getEffect(Effect.POISON).getDuration() >= 10)
				{
					addParticle(player, Particle.TYPE_REDSTONE, 20);
					player.getLevel().addSound(player, Sound.MOB_SPIDER_SAY);
					player.setMotion(player.getDirectionVector().multiply(getBoost((int) target.distance(player), .16)+.5));

					TaskHandler t = Server.getInstance().getScheduler().scheduleRepeatingTask(new Task() {

						@Override
						public void onRun(int arg0) 
						{
							if(target != null && target.getBoundingBox().intersectsWith(player.getBoundingBox()) && player.hasEffect(Effect.POISON) && player.getEffect(Effect.POISON).getDuration() >= 10)
							{
								if(!target.namedTag.contains("web") && player.getInventory().getItemInHand().getId() == Item.STRING && getRadius("length", player.getInventory().getLeggings()) != 0)
								{
									Item item = player.getInventory().getItemInHand();
									item.setCount(item.getCount()-5);
									player.getInventory().setItemInHand(item);
									target.namedTag.putDouble("web", 5);
									createWebTrap(player, target);
								}
								Vector3 l = target.getDirectionVector().multiply(-1).add(target);
								player.teleport(new Location(l.x, target.y, l.z, target.yaw, target.pitch, player.getLevel()));
								EntityDamageByEntityEvent damage = new EntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, 6);
								damage.setKnockBack(1.3F);
								target.attack(damage);
								addParticle(target.add(0, target.getEyeHeight(), 0), Particle.TYPE_REDSTONE, 30);
								target.addEffect(Effect.getEffect(Effect.POISON).setDuration(10 * 20).setAmplifier(4));
								target.addEffect(Effect.getEffect(Effect.BLINDNESS).setDuration(5 * 20).setVisible(false));
								target.addEffect(Effect.getEffect(Effect.NAUSEA).setDuration(10 * 20).setVisible(false));
								target.addEffect(Effect.getEffect(Effect.SLOWNESS).setDuration(10 * 20).setVisible(false));
								if(target instanceof Player)
									target.getLevel().addSound(target, Sound.MOB_ELDERGUARDIAN_CURSE, 1, 1, (Player) target);
								player.getLevel().addSound(player, Sound.MOB_SPIDER_DEATH);
								player.removeEffect(Effect.POISON);
								this.cancel();
							}
						}
					}, 1);

					Server.getInstance().getScheduler().scheduleDelayedTask(new Task() {

						@Override
						public void onRun(int arg0) 
						{
							if(!t.isCancelled())
								t.cancel();
						}
					}, 20);
				}
			}
		}
	}

	@EventHandler
	public void onFall(EntityDamageEvent event)
	{
		if(event.getEntity() instanceof Player)
		{
			Player player = (Player) event.getEntity();
			if(event.getCause() == DamageCause.FALL && getRadius("multiplier", player.getInventory().getBoots()) != 0)
				event.setDamage(event.getDamage()/2F);

			else if(event.getCause() == DamageCause.MAGIC && player.hasEffect(Effect.POISON) && getRadius("distance", player.getInventory().getHelmet()) != 0)
				event.setCancelled(true);
		}
	}

	@EventHandler
	public void onSpawn(EntitySpawnEvent event)
	{
		createWebTrap(null, event.getEntity());
	}

	private void createWebTrap(Player player, Entity e)
	{
		CompoundTag tag = e.namedTag;
		if(tag.contains("web"))
		{
			double x = e.x;
			double y = e.y;
			double z = e.z;
			double wave = e.getWidth()+.1;
			final double radius = wave;
			FloatingTextParticle particle = new FloatingTextParticle(e.add(0, e.getHeight()+.5, 0), "10.0");
			e.getLevel().addParticle(particle);
			e.getLevel().addSound(e, Sound.RANDOM_ANVIL_USE);
			DecimalFormat df = new DecimalFormat("#.##");
			Server.getInstance().getScheduler().scheduleRepeatingTask(new Task() {

				int yaw = 5;
				int i = 0;

				@Override
				public void onRun(int arg0) 
				{
					yaw = yaw +=5;
					if(yaw > 360)
						yaw = 5;
					double angle = 2 * Math.PI * i / 30;
					Location point = e.clone().add(radius * Math.sin(angle), e.getHeight()-.3, radius * Math.cos(angle));
					e.getLevel().addParticle(new GenericParticle(point, Particle.TYPE_EVAPORATION));
					i++;

					double sec = tag.getDouble("web");
					if(player != null)
					{
						int length = getRadius("length", player.getInventory().getLeggings());
						int s = getLength(length);
						Item item = player.getInventory().getItemInHand();
						if(item.getId() == Item.STRING && player.distance(e) <= 3 && length != 0 && s >= sec &&
								item.getCount() >= 5)
						{
							item.setCount(item.getCount()-5);
							player.getLevel().addSound(player, Sound.BLOCK_SWEET_BERRY_BUSH_PICK);
							player.getInventory().setItemInHand(item);
							tag.putDouble("web", sec+5.0);
						}
					}

					tag.putDouble("web", tag.getDouble("web")-.10);
					e.teleport(new Location(x, y, z, yaw, 0));
					e.getLevel().addSound(e, Sound.MOB_SPIDER_STEP);
					particle.setTitle(df.format(sec));

					if(e == null || sec <= 0 || !e.isAlive() || e.getLocation().getLevelBlock().getId() == Block.WATER)
					{
						tag.remove("web");
						particle.setTitle("");
						particle.setInvisible(true);
						e.getLevel().addSound(e, Sound.RANDOM_ANVIL_BREAK);
						this.cancel();
					}
				}

			}, 1);
		}
	}

	private int getLength(int num)
	{
		return (int) getBoost(num, 50);
	}

	private double getBoost(int num, double add)
	{
		double i = 0;
		for(int x = 0; x < num; x++)
			i = i + add;
		return i;
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

	private void addParticle(Location player, int p, int size)
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

	private int getDistance(Player player)
	{
		Item item = player.getInventory().getHelmet();
		if(item.hasCompoundTag() && item.getNamedTag().contains("distance") && player.hasEffect(Effect.POISON))
			return (int) getBoost(item.getNamedTag().getInt("distance"), 12);
		else
			return 0;
	}

	public static int getRadius(String tag, Item item)
	{
		if(item.hasCompoundTag() && item.getNamedTag().contains(tag))
			return item.getNamedTag().getInt(tag);
		else
			return 0;
	}

	private EntityCreature getEntityInSight(Player player, int distance)
	{
		EntityCreature inSight = null;
		for(Entity nearbyEntity : player.getLevel().getNearbyEntities(player.getBoundingBox().grow(distance, distance, distance), player))
		{
			if(nearbyEntity instanceof EntityCreature)
			{
				if(nearbyEntity.boundingBox.isVectorInside(distance(player, nearbyEntity.distance(player))))
				{
					inSight = (EntityCreature) nearbyEntity;
					return inSight;
				}
			}
		}
		return inSight;
	}

	private Vector3 distance(Player player, double blocksAway) {
		return player.add(0, player.getEyeHeight(), 0).add(player.getDirectionVector().multiply(blocksAway));
	}
}
