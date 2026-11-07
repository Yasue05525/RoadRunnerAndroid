# Road Runner — Unity 3D source

This is a **separate Unity implementation** alongside the existing native Android/Kotlin prototype. It targets Unity 2022.3, Android and the Old Input Manager. The existing Kotlin app is not a Unity project and cannot become a real 3D Unity game just by changing its Canvas drawing code.

## Scene requirements

Create a scene named `Runner` and use only 3D GameObjects with MeshRenderer and 3D colliders:

- Main Camera: Projection **Perspective**, Position **(0, 6, -8)**, Rotation **(15, 0, 0)**.
- Road: Cube, Position **(0, 0, 100)**, Scale **(6, 0.2, 200)**.
- Lane markings: two Cube objects, Scale **(0.15, 0.21, 200)**, Positions **(-1, 0.11, 100)** and **(1, 0.11, 100)**.
- Lane centers are X = **-2, 0, +2**.
- Player root: Position **(0, 0.5, 0)**, tag **Player**, BoxCollider, Rigidbody (Is Kinematic on, Use Gravity off). Add `RunnerController`.
- Player body: Cube Scale **(1.6, 0.8, 3.2)**. Add a smaller cabin Cube above it and four Cylinder wheels rotated **(0, 0, 90)**. Keep all parts as children of Player.
- Obstacle prefab: same primitive-car construction, different material/color, root tag **Obstacle**, BoxCollider and kinematic Rigidbody; add `ObstacleMover`.
- Add `ObstacleSpawner` to an empty GameObject and assign the Player and obstacle prefab in Inspector.

The scripts intentionally do not generate meshes or use Sprite/SpriteRenderer/2D physics. Build the car prefabs from Unity's **3D Object > Cube/Cylinder** primitives in the Editor. Ensure the `Player` and `Obstacle` tags exist in **Project Settings > Tags and Layers**.

## Controls and gameplay

Swipe horizontally at least 50 screen pixels to move one lane. Player X is moved with MoveTowards to (currentLane - 1) * 2. Obstacles spawn at X = (lane - 1) * 2, Y = 0.5, Z = 60, move toward negative Z and are destroyed below Z = -20. Each spawn blocks one lane only; the spawner also avoids repeating the same lane consecutively.

## Android setup

1. Open this folder's Unity project (create a Unity 2022.3 project and copy the `Assets` folder into it, or copy these scripts into an existing Unity 2022.3 project).
2. In **Edit > Project Settings > Player > Active Input Handling**, select **Input Manager (Old)** or **Both**.
3. Add the configured scene to Build Settings, select Android, then Build.

This source drop does not include a compiled APK or a complete Unity scene/prefab asset; those must be authored/imported in the Unity Editor and built with Unity.
