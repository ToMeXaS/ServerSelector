package lt.tomexas.serverselector.Utils;

import lt.tomexas.serverselector.Main;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class CamPath {


    //Locations
    private Location start;
    private Location end;
    private Location step;

    //Integers
    private int durationInTicks;
    private int tick = 0;
    private int taskId;

    //Lists
    private List<Location> pathLocations = new ArrayList<>();
    private List<UUID> playersInCam = new ArrayList<>();
    private List<UUID> players;

    //Before states
    private Map<UUID, GameMode> playersGamemodesBefore = new HashMap<>();
    private Map<UUID, Location> playersLocationsBefore = new HashMap<>();
    private Map<UUID, Boolean> playersFlyingBefore = new HashMap<>();

    private Map<UUID, ArmorStand> armorStands = new HashMap<>();



    //The method gets values given on calling the class and makes the instances
    public CamPath(List<UUID> players, Location start, Location end, int durationInTicks) {
        this.players = players;
        this.start = start;
        this.end = end;
        this.durationInTicks = durationInTicks;
    }



    //The method generates the locations for the path and puts them into a list called pathLocations
    public void generatePath() {
        //Making a step location, which gets added to the location before to fill the list
        double stepX = (end.getX() - start.getX()) / durationInTicks;
        double stepY = (end.getY() - start.getY()) / durationInTicks;
        double stepZ = (end.getZ() - start.getZ()) / durationInTicks;
        float stepYaw = (end.getYaw() - start.getYaw()) / durationInTicks;
        float stepPitch = (end.getPitch() - start.getPitch()) / durationInTicks;

        step = new Location(Bukkit.getWorld("world"), stepX, stepY, stepZ, stepYaw, stepPitch);


        //Fills the list with locations (previous location + step location)
        pathLocations.add(start);
        for (int i = 1; i <= durationInTicks; i++) {
            Location prevLocation = pathLocations.get(i-1).clone();
            Location nextlocation = prevLocation.add(step);

            float yaw = nextlocation.getYaw() + stepYaw;
            float pitch = nextlocation.getPitch() + stepPitch;
            nextlocation.setYaw(yaw);
            nextlocation.setPitch(pitch);

            pathLocations.add(nextlocation);
        }

        //Storing before variables
        for (UUID playerUUID : players) {
            playersGamemodesBefore.put(playerUUID, Bukkit.getPlayer(playerUUID).getGameMode());
            playersLocationsBefore.put(playerUUID, Bukkit.getPlayer(playerUUID).getLocation());
            playersFlyingBefore.put(playerUUID, Bukkit.getPlayer(playerUUID).isFlying());

            playersInCam.add(playerUUID);
        }


        runPath();
    }


    //This method actually makes the player move on the path by the predefined locations in the pathLocations list
    public void runPath() {
        int updatesPerTick = 4;
        int totalUpdates = durationInTicks * updatesPerTick;

        List<Location> smoothPath = new ArrayList<>();
        for (int i = 0; i < pathLocations.size() - 1; i++) {
            Location current = pathLocations.get(i);
            Location next = pathLocations.get(i + 1);

            for (int j = 0; j < updatesPerTick; j++) {
                double progress = (double) j / updatesPerTick;
                // Apply easing to the progress
                double easedProgress = easeInOutQuad(progress);

                Location interpolated = current.clone();
                interpolated.add(next.clone().subtract(current).multiply(easedProgress));
                interpolated.setYaw((float) (current.getYaw() + (next.getYaw() - current.getYaw()) * easedProgress));
                interpolated.setPitch((float) (current.getPitch() + (next.getPitch() - current.getPitch()) * easedProgress));
                smoothPath.add(interpolated);
            }
        }
        smoothPath.add(pathLocations.get(pathLocations.size() - 1));
        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            armorStands.put(playerUUID, (ArmorStand) player.getWorld().spawnEntity(start, EntityType.ARMOR_STAND));
        }

        taskId = new BukkitRunnable() {
            int updateCount = 0;

            @Override
            public void run() {
                for (UUID playerUUID : players) {
                    Player player = Bukkit.getPlayer(playerUUID);
                    ArmorStand as = armorStands.get(playerUUID);
                    as.setGravity(false);
                    as.setInvisible(true);

                    if (updateCount == 0) {
                        player.setGameMode(GameMode.SPECTATOR);
                        player.setSpectatorTarget(as);
                        as.teleport(start);
                    } else if (updateCount >= totalUpdates) {
                        as.teleport(end);
                        as.remove();
                        cancel();
                        stop();
                    } else {
                        // Smooth movement using interpolated locations
                        Location currentPos = smoothPath.get(updateCount);
                        as.teleport(currentPos);
                    }
                }
                updateCount++;
            }
        }.runTaskTimer(Main.getInstance(), 1, 1L / updatesPerTick).getTaskId();
    }


    //Stopping everything and putting player back to the state where it started
    public void stop() {
        //I don't know if it needs a try catch but safe is safe right :D
        try {
            Bukkit.getScheduler().cancelTask(taskId);
        }catch (Exception e) {
            e.printStackTrace();
        }

        //Reading and setting default variables
        for (UUID playerUUID : players) {
            Player target = Bukkit.getPlayer(playerUUID);

            target.setGameMode(playersGamemodesBefore.get(playerUUID));
            target.teleport(playersLocationsBefore.get(playerUUID));
            target.setFlying(playersFlyingBefore.get(playerUUID));

            playersInCam.remove(playerUUID);
        }

    }


    //This method calculates the velocity by a starting and ending position
    public Vector calculateVector(Location start, Location end) {
        return end.toVector().subtract(start.toVector());
    }

    private double easeInOutQuad(double x) {
        // Quadratic easing for smooth acceleration and deceleration
        return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2;
    }

}
